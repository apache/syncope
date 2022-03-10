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
import java.io.IOException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonDiffPanel;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AuditHistoryDetails<T extends Serializable> extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -7400543686272100483L;

    private static final Logger LOG = LoggerFactory.getLogger(AuditHistoryDetails.class);

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
            registerModule(new SimpleModule().addSerializer(new SortedSetJsonSerializer(cast(Set.class))));

    public AuditHistoryDetails(
            final MultilevelPanel mlp,
            final AuditEntry selected,
            final EntityTO currentEntity,
            final String auditRestoreEntitlement) {

        super();

        AuditEntry current = new AuditEntry();
        if (currentEntity instanceof AnyTO) {
            current.setWho(((AnyTO) currentEntity).getCreator());
            current.setDate(((AnyTO) currentEntity).getCreationDate());
        } else {
            current.setWho(SyncopeConsoleSession.get().getSelfTO().getUsername());
            current.setDate(OffsetDateTime.now());
        }
        try {
            current.setBefore(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(currentEntity));
        } catch (JsonProcessingException e) {
            LOG.error("While serializing current entity", e);
            throw new WicketRuntimeException(e);
        }

        add(new Label("current", getString("current")));
        add(new Label("previous", getString("previous")));

        @SuppressWarnings("unchecked")
        Class<T> reference = (Class<T>) currentEntity.getClass();
        add(new JsonDiffPanel(null, toJSON(current, reference), toJSON(selected, reference), null) {

            private static final long serialVersionUID = 2087989787864619493L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        });

        AjaxLink<Void> restore = new AjaxLink<>("restore") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    String json = selected.getBefore() == null
                            ? MAPPER.readTree(selected.getOutput()).get("entity").toPrettyString()
                            : selected.getBefore();
                    restore(json, target);

                    mlp.prev(target);
                } catch (JsonProcessingException e) {
                    throw new WicketRuntimeException(e);
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(restore, ENABLE, auditRestoreEntitlement);
        add(restore);
    }

    protected abstract void restore(String json, AjaxRequestTarget target);

    private Model<String> toJSON(final AuditEntry auditEntry, final Class<T> reference) {
        try {
            String content = auditEntry.getBefore() == null
                    ? MAPPER.readTree(auditEntry.getOutput()).get("entity").toPrettyString()
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
