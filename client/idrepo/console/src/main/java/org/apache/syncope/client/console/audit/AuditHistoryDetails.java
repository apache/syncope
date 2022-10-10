/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.audit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxEventBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonDiffPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuditHistoryDetails<T extends Serializable> extends Panel implements ModalPanel {

    private static final long serialVersionUID = -7400543686272100483L;

    private static final Logger LOG = LoggerFactory.getLogger(AuditHistoryDetails.class);

    private static final List<String> EVENTS = List.of("create", "update", "matchingrule_update",
            "unmatchingrule_assign", "unmatchingrule_provision");

    private static final SortParam<String> REST_SORT = new SortParam<>("event_date", false);

    private EntityTO currentEntity;

    private AuditElements.EventCategoryType type;

    private String category;

    private Class<T> reference;

    private List<AuditEntry> auditEntries = new ArrayList<>();

    private AuditEntry latestAuditEntry;

    private AuditEntry after;

    private AjaxDropDownChoicePanel<AuditEntry> beforeVersionsPanel;

    private AjaxDropDownChoicePanel<AuditEntry> afterVersionsPanel;

    private final AjaxLink<Void> restore;

    private static class SortingNodeFactory extends JsonNodeFactory {

        private static final long serialVersionUID = 1870252010670L;

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<>());
        }
    }

    private static class SortedSetJsonSerializer extends StdSerializer<Set<?>> {

        private static final long serialVersionUID = 3849059774309L;

        SortedSetJsonSerializer(final Class<Set<?>> clazz) {
            super(clazz);
        }

        @Override
        public void serialize(
                final Set<?> set,
                final JsonGenerator gen,
                final SerializerProvider sp) throws IOException {

            if (set == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartArray();

            if (!set.isEmpty()) {
                Set<?> sorted = set;

                // create sorted set only if it itself is not already SortedSet
                if (!SortedSet.class.isAssignableFrom(set.getClass())) {
                    Object item = set.iterator().next();
                    if (Comparable.class.isAssignableFrom(item.getClass())) {
                        // and only if items are Comparable
                        sorted = new TreeSet<>(set);
                    } else {
                        LOG.debug("Cannot sort items of type {}", item.getClass());
                    }
                }

                for (Object item : sorted) {
                    gen.writeObject(item);
                }
            }

            gen.writeEndArray();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> cast(final Class<?> aClass) {
        return (Class<T>) aClass;
    }

    private static final ObjectMapper MAPPER = JsonMapper.builder().
            nodeFactory(new SortingNodeFactory()).build().
            registerModule(new SimpleModule().addSerializer(new SortedSetJsonSerializer(cast(Set.class)))).
            registerModule(new JavaTimeModule());

    @SuppressWarnings("unchecked")
    public AuditHistoryDetails(
            final String id,
            final EntityTO currentEntity,
            final AuditElements.EventCategoryType type,
            final String category,
            final String auditRestoreEntitlement) {

        super(id);

        this.setOutputMarkupId(true);
        this.reference = (Class<T>) currentEntity.getClass();
        this.currentEntity = currentEntity;
        this.type = type;
        this.category = category;

        IChoiceRenderer<AuditEntry> choiceRenderer = new IChoiceRenderer<>() {

            private static final long serialVersionUID = -3724971416312135885L;

            @Override
            public String getDisplayValue(final AuditEntry value) {
                return SyncopeConsoleSession.get().getDateFormat().format(value.getDate());
            }

            @Override
            public String getIdValue(final AuditEntry value, final int i) {
                return Long.toString(value.getDate().toInstant().toEpochMilli());
            }

            @Override
            public AuditEntry getObject(final String id, final IModel<? extends List<? extends AuditEntry>> choices) {
                return choices.getObject().stream().
                        filter(c -> StringUtils.isNotBlank(id)
                        && Long.parseLong(id) == c.getDate().toInstant().toEpochMilli()).
                        findFirst().orElse(null);
            }
        };
        // add also select to choose with which version compare

        beforeVersionsPanel =
                new AjaxDropDownChoicePanel<>("beforeVersions", getString("beforeVersions"), new Model<>(), true);
        beforeVersionsPanel.setChoiceRenderer(choiceRenderer);
        beforeVersionsPanel.add(new IndicatorAjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -6383712635009760397L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                AuditEntry beforeEntry = beforeVersionsPanel.getModelObject() == null
                        ? latestAuditEntry
                        : beforeVersionsPanel.getModelObject();
                AuditEntry afterEntry = afterVersionsPanel.getModelObject() == null
                        ? after
                        : buildAfterAuditEntry(beforeEntry);
                AuditHistoryDetails.this.addOrReplace(new JsonDiffPanel(toJSON(beforeEntry, reference),
                        toJSON(afterEntry, reference)));
                // change after audit entries in order to match only the ones newer than the current after one
                afterVersionsPanel.setChoices(auditEntries.stream().
                        filter(ae -> ae.getDate().isAfter(beforeEntry.getDate())
                        || ae.getDate().isEqual(beforeEntry.getDate())).
                        collect(Collectors.toList()));
                // set the new after entry
                afterVersionsPanel.setModelObject(afterEntry);
                target.add(AuditHistoryDetails.this);
            }
        });
        afterVersionsPanel =
                new AjaxDropDownChoicePanel<>("afterVersions", getString("afterVersions"), new Model<>(),
                        true);
        afterVersionsPanel.setChoiceRenderer(choiceRenderer);
        afterVersionsPanel.add(new IndicatorAjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -6383712635009760397L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                AuditHistoryDetails.this.addOrReplace(new JsonDiffPanel(
                        toJSON(beforeVersionsPanel.getModelObject() == null
                                ? latestAuditEntry
                                : beforeVersionsPanel.getModelObject(), reference),
                        toJSON(afterVersionsPanel.getModelObject() == null
                                ? after
                                : buildAfterAuditEntry(afterVersionsPanel.getModelObject()), reference)));
                target.add(AuditHistoryDetails.this);
            }
        });
        add(beforeVersionsPanel.setOutputMarkupId(true));
        add(afterVersionsPanel.setOutputMarkupId(true));

        restore = new AjaxLink<>("restore") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    AuditEntry before = beforeVersionsPanel.getModelObject() == null
                            ? latestAuditEntry
                            : beforeVersionsPanel.getModelObject();
                    String json = before.getBefore() == null
                            ? MAPPER.readTree(before.getOutput()).get("entity") == null
                            ? MAPPER.readTree(before.getOutput()).toPrettyString()
                            : MAPPER.readTree(before.getOutput()).get("entity").toPrettyString()
                            : before.getBefore();
                    restore(json, target);
                } catch (JsonProcessingException e) {
                    throw new WicketRuntimeException(e);
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(restore, ENABLE, auditRestoreEntitlement);
        add(restore);

        initDiff();
    }

    protected abstract void restore(String json, AjaxRequestTarget target);

    protected void initDiff() {
        // audit fetch size is fixed, for the moment... 
        this.auditEntries.clear();
        this.auditEntries.addAll(new AuditRestClient().search(
                currentEntity.getKey(),
                1,
                50,
                type,
                category,
                EVENTS,
                AuditElements.Result.SUCCESS,
                REST_SORT));

        // the default selected is the newest one, if any
        this.latestAuditEntry = auditEntries.isEmpty() ? null : auditEntries.get(0);
        this.after = latestAuditEntry == null ? null : buildAfterAuditEntry(latestAuditEntry);
        // add default diff panel
        addOrReplace(new JsonDiffPanel(toJSON(latestAuditEntry, reference), toJSON(after, reference)));

        beforeVersionsPanel.setChoices(auditEntries);
        afterVersionsPanel.setChoices(auditEntries.stream().
                filter(ae -> ae.getDate().isAfter(after.getDate()) || ae.getDate().isEqual(after.getDate())).
                collect(Collectors.toList()));

        beforeVersionsPanel.setModelObject(latestAuditEntry);
        afterVersionsPanel.setModelObject(after);

        restore.setEnabled(!auditEntries.isEmpty());
    }

    private AuditEntry buildAfterAuditEntry(final AuditEntry input) {
        AuditEntry output = new AuditEntry();
        output.setWho(input.getWho());
        output.setDate(input.getDate());
        // current by default is the output of the selected event
        output.setOutput(input.getOutput());
        output.setThrowable(input.getThrowable());
        return output;
    }

    private Model<String> toJSON(final AuditEntry auditEntry, final Class<T> reference) {
        try {
            if (auditEntry == null) {
                return Model.of();
            }
            String content = auditEntry.getBefore() == null
                    ? MAPPER.readTree(auditEntry.getOutput()).get("entity") == null
                    ? MAPPER.readTree(auditEntry.getOutput()).toPrettyString()
                    : MAPPER.readTree(auditEntry.getOutput()).get("entity").toPrettyString()
                    : auditEntry.getBefore();

            T entity = MAPPER.reader().
                    with(StreamReadFeature.STRICT_DUPLICATE_DETECTION).
                    readValue(content, reference);
            if (entity instanceof UserTO) {
                UserTO userTO = (UserTO) entity;
                userTO.setPassword(null);
                userTO.setSecurityAnswer(null);
            }

            return Model.of(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(entity));
        } catch (Exception e) {
            LOG.error("While (de)serializing entity {}", auditEntry, e);
            throw new WicketRuntimeException(e);
        }
    }
}
