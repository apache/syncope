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
package org.apache.syncope.core.persistence.jpa.dao;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AuditElements;
import org.springframework.util.CollectionUtils;

public abstract class AbstractJPAJSONLoggerDAO extends JPAAuditConfDAO {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected abstract static class JSONMessageCriteriaBuilder extends MessageCriteriaBuilder {

        protected String entityKey;

        private AuditElements.EventCategoryType type;

        private String category;

        private String subcategory;

        private List<String> events;

        private AuditElements.Result result;

        @Override
        protected MessageCriteriaBuilder entityKey(final String entityKey) {
            this.entityKey = entityKey;
            return this;
        }

        @Override
        public MessageCriteriaBuilder type(final AuditElements.EventCategoryType type) {
            this.type = type;
            return this;
        }

        @Override
        public MessageCriteriaBuilder category(final String category) {
            this.category = category;
            return this;
        }

        @Override
        public MessageCriteriaBuilder subcategory(final String subcategory) {
            this.subcategory = subcategory;
            return this;
        }

        @Override
        public MessageCriteriaBuilder events(final List<String> events) {
            this.events = events;
            return this;
        }

        @Override
        public MessageCriteriaBuilder result(final AuditElements.Result result) {
            this.result = result;
            return this;
        }

        private Optional<ObjectNode> buildContainer() {
            ObjectNode logger = MAPPER.createObjectNode();
            if (type != null) {
                logger.put("type", type.name());
            }
            if (StringUtils.isNotBlank(category)) {
                logger.put("category", category);
            }
            if (StringUtils.isNotBlank(subcategory)) {
                logger.put("subcategory", subcategory);
            }
            if (result != null) {
                logger.put("result", result.name());
            }

            if (!logger.isEmpty()) {
                ObjectNode container = MAPPER.createObjectNode();
                container.set("logger", logger);
                return Optional.of(container);
            }

            return Optional.empty();
        }

        protected abstract String doBuild(List<ObjectNode> containers);

        @Override
        public String build() {
            List<ObjectNode> containers = new ArrayList<>();
            if (CollectionUtils.isEmpty(events)) {
                buildContainer().ifPresent(containers::add);
            } else {
                events.forEach(event -> buildContainer().ifPresent(container -> {
                    ((ObjectNode) container.get("logger")).put("event", event);
                    containers.add(container);
                }));
            }

            return doBuild(containers);
        }
    }
}
