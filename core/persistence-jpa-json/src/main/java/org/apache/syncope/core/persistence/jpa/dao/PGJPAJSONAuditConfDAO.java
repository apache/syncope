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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.jpa.dao.AbstractJPAJSONLoggerDAO.JSONMessageCriteriaBuilder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class PGJPAJSONAuditConfDAO extends AbstractJPAJSONLoggerDAO {

    private static class PGMessageCriteriaBuilder extends JSONMessageCriteriaBuilder {

        @Override
        protected String doBuild(final List<ObjectNode> containers) {
            if (entityKey != null) {
                query.append('(').
                        append(AUDIT_ENTRY_MESSAGE_COLUMN).append(" ->> 'before' LIKE '%").append(entityKey).
                        append("%' OR ").
                        append(AUDIT_ENTRY_MESSAGE_COLUMN).append(" ->> 'input' LIKE '%").append(entityKey).
                        append("%' OR ").
                        append(AUDIT_ENTRY_MESSAGE_COLUMN).append(" ->> 'output' LIKE '%").append(entityKey).
                        append("%')");
            }

            if (!containers.isEmpty()) {
                if (entityKey != null) {
                    query.append(" AND (");
                }
                query.append(containers.stream().
                        map(container -> AUDIT_ENTRY_MESSAGE_COLUMN + "::jsonb @> '"
                        + POJOHelper.serialize(container).replace("'", "''")
                        + "'::jsonb").collect(Collectors.joining(" OR ")));
                if (entityKey != null) {
                    query.append(')');
                }
            }

            return query.toString();
        }
    }

    @Override
    protected String select() {
        return AUDIT_ENTRY_MESSAGE_COLUMN + "::text";
    }

    @Override
    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new PGMessageCriteriaBuilder().entityKey(entityKey);
    }
}
