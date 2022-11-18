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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OJPAJSONAuditConfDAO extends AbstractJPAJSONLoggerDAO {

    protected static class OMessageCriteriaBuilder extends JSONMessageCriteriaBuilder {

        protected Optional<String> jsonExprItem(final JsonNode logger, final String field) {
            return logger.has(field)
                    ? Optional.of("@.logger." + field + " == \"" + logger.get(field).asText() + "\"")
                    : Optional.empty();
        }

        @Override
        protected String doBuild(final List<ObjectNode> containers) {
            if (entityKey != null) {
                query.append(andIfNeeded()).append('(').
                        append("JSON_VALUE(").append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(", '$.before' RETURNING VARCHAR2(32767)) LIKE '%").
                        append(entityKey).append("%' OR ").
                        append("JSON_VALUE(").append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(", '$.input' RETURNING VARCHAR2(32767)) LIKE '%").
                        append(entityKey).append("%' OR ").
                        append("JSON_VALUE(").append(AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(", '$.output' RETURNING VARCHAR2(32767)) LIKE '%").
                        append(entityKey).append("%')");
            }

            if (!containers.isEmpty()) {
                query.append(andIfNeeded()).append('(').
                        append(containers.stream().filter(container -> container.has("logger")).map(container -> {
                            JsonNode logger = container.get("logger");

                            List<String> clauses = new ArrayList<>();
                            jsonExprItem(logger, "type").ifPresent(clauses::add);
                            jsonExprItem(logger, "category").ifPresent(clauses::add);
                            jsonExprItem(logger, "subcategory").ifPresent(clauses::add);
                            jsonExprItem(logger, "result").ifPresent(clauses::add);
                            jsonExprItem(logger, "event").ifPresent(clauses::add);

                            return "JSON_EXISTS(MESSAGE, '$[*]?(" + String.join(" && ", clauses) + ")')";
                        }).filter(Objects::nonNull).collect(Collectors.joining(" OR "))).
                        append(')');
            }

            return query.toString();
        }
    }

    @Override
    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new OMessageCriteriaBuilder().entityKey(entityKey);
    }
}
