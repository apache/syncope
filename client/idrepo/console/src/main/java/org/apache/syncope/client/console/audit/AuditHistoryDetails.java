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
import com.fasterxml.jackson.databind.JsonNode;
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
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.OpEvent;
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

    protected static final Logger LOG = LoggerFactory.getLogger(AuditHistoryDetails.class);

    protected static final SortParam<String> REST_SORT = new SortParam<>("when", false);

    protected static class SortingNodeFactory extends JsonNodeFactory {

        private static final long serialVersionUID = 1870252010670L;

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<>());
        }
    }

    protected static class SortedSetJsonSerializer extends StdSerializer<Set<?>> {

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
    protected static <T> Class<T> cast(final Class<?> aClass) {
        return (Class<T>) aClass;
    }

    protected static final ObjectMapper MAPPER = JsonMapper.builder().
            nodeFactory(new SortingNodeFactory()).build().
            registerModule(new SimpleModule().addSerializer(new SortedSetJsonSerializer(cast(Set.class)))).
            registerModule(new JavaTimeModule());

    protected EntityTO currentEntity;

    protected OpEvent.CategoryType type;

    protected String category;

    protected String op;

    protected Class<T> reference;

    protected final List<AuditEventTO> auditEntries = new ArrayList<>();

    protected AuditEventTO latestAuditEventTO;

    protected AuditEventTO after;

    protected AjaxDropDownChoicePanel<AuditEventTO> beforeVersionsPanel;

    protected AjaxDropDownChoicePanel<AuditEventTO> afterVersionsPanel;

    protected final AjaxLink<Void> restore;

    protected final AuditRestClient restClient;

    @SuppressWarnings("unchecked")
    public AuditHistoryDetails(
            final String id,
            final EntityTO currentEntity,
            final OpEvent.CategoryType type,
            final String category,
            final String op,
            final String auditRestoreEntitlement,
            final AuditRestClient restClient) {

        super(id);

        this.currentEntity = currentEntity;
        this.type = type;
        this.category = category;
        this.op = op;
        this.reference = (Class<T>) currentEntity.getClass();
        this.restClient = restClient;

        setOutputMarkupId(true);

        IChoiceRenderer<AuditEventTO> choiceRenderer = new IChoiceRenderer<>() {

            private static final long serialVersionUID = -3724971416312135885L;

            @Override
            public String getDisplayValue(final AuditEventTO value) {
                return SyncopeConsoleSession.get().getDateFormat().format(value.getWhen());
            }

            @Override
            public String getIdValue(final AuditEventTO value, final int i) {
                return Long.toString(value.getWhen().toInstant().toEpochMilli());
            }

            @Override
            public AuditEventTO getObject(
                    final String id, final IModel<? extends List<? extends AuditEventTO>> choices) {

                return choices.getObject().stream().
                        filter(c -> StringUtils.isNotBlank(id)
                        && Long.parseLong(id) == c.getWhen().toInstant().toEpochMilli()).
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
                AuditEventTO beforeEvent = beforeVersionsPanel.getModelObject() == null
                        ? latestAuditEventTO
                        : beforeVersionsPanel.getModelObject();
                AuditEventTO afterEvent = afterVersionsPanel.getModelObject() == null
                        ? after
                        : buildAfterAuditEventTO(beforeEvent);
                AuditHistoryDetails.this.addOrReplace(
                        new JsonDiffPanel(toJSON(beforeEvent, reference), toJSON(afterEvent, reference)));
                // change after audit entries in order to match only the ones newer than the current after one
                afterVersionsPanel.setChoices(auditEntries.stream().
                        filter(ae -> ae.getWhen().isAfter(beforeEvent.getWhen())
                        || ae.getWhen().isEqual(beforeEvent.getWhen())).
                        collect(Collectors.toList()));
                // set the new after entry
                afterVersionsPanel.setModelObject(afterEvent);
                target.add(AuditHistoryDetails.this);
            }
        });
        afterVersionsPanel =
                new AjaxDropDownChoicePanel<>("afterVersions", getString("afterVersions"), new Model<>(), true);
        afterVersionsPanel.setChoiceRenderer(choiceRenderer);
        afterVersionsPanel.add(new IndicatorAjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -6383712635009760397L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                AuditHistoryDetails.this.addOrReplace(new JsonDiffPanel(
                        toJSON(beforeVersionsPanel.getModelObject() == null
                                ? latestAuditEventTO
                                : beforeVersionsPanel.getModelObject(), reference),
                        toJSON(afterVersionsPanel.getModelObject() == null
                                ? after
                                : buildAfterAuditEventTO(afterVersionsPanel.getModelObject()), reference)));
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
                    AuditEventTO before = beforeVersionsPanel.getModelObject() == null
                            ? latestAuditEventTO
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
        auditEntries.clear();
        auditEntries.addAll(restClient.search(
                currentEntity.getKey(),
                1,
                50,
                type,
                category,
                op,
                OpEvent.Outcome.SUCCESS,
                REST_SORT));

        // the default selected is the newest one, if any
        latestAuditEventTO = auditEntries.isEmpty() ? null : auditEntries.getFirst();
        after = latestAuditEventTO == null ? null : buildAfterAuditEventTO(latestAuditEventTO);
        // add default diff panel
        addOrReplace(new JsonDiffPanel(toJSON(latestAuditEventTO, reference), toJSON(after, reference)));

        beforeVersionsPanel.setChoices(auditEntries);
        afterVersionsPanel.setChoices(auditEntries.stream().
                filter(ae -> ae.getWhen().isAfter(after.getWhen()) || ae.getWhen().isEqual(after.getWhen())).
                collect(Collectors.toList()));

        beforeVersionsPanel.setModelObject(latestAuditEventTO);
        afterVersionsPanel.setModelObject(after);

        restore.setEnabled(!auditEntries.isEmpty());
    }

    protected AuditEventTO buildAfterAuditEventTO(final AuditEventTO input) {
        AuditEventTO output = new AuditEventTO();
        output.setWho(input.getWho());
        output.setWhen(input.getWhen());
        // current by default is the output of the selected event
        output.setOutput(input.getOutput());
        output.setThrowable(input.getThrowable());
        return output;
    }

    protected Model<String> toJSON(final AuditEventTO auditEvent, final Class<T> reference) {
        if (auditEvent == null) {
            return Model.of();
        }

        try {
            String content;
            if (auditEvent.getBefore() == null) {
                JsonNode output = MAPPER.readTree(auditEvent.getOutput());
                if (output.has("entity")) {
                    content = output.get("entity").toPrettyString();
                } else {
                    content = output.toPrettyString();
                }
            } else {
                content = auditEvent.getBefore();
            }

            T entity = MAPPER.reader().
                    with(StreamReadFeature.STRICT_DUPLICATE_DETECTION).
                    readValue(content, reference);
            if (entity instanceof UserTO userTO) {
                userTO.setPassword(null);
                userTO.setSecurityAnswer(null);
            }

            return Model.of(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(entity));
        } catch (Exception e) {
            LOG.error("While (de)serializing entity {}", auditEvent, e);
            return Model.of();
        }
    }
}
