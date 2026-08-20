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
package org.apache.syncope.core.persistence.common.dao;

import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.springframework.data.domain.Sort;
import org.springframework.util.ReflectionUtils;

public abstract class AbstractAuditEventDAO {

    protected static void checkEntityKey(final String entityKey) {
        if (!SyncopeConstants.UUID_PATTERN.matcher(entityKey).matches()) {
            throw new IllegalArgumentException("Invalid entityKey: " + entityKey);
        }
    }

    // The free-form value is bound as a query parameter and its LIKE metacharacters are escaped, 
    // so '%'/'_' in the value match literally and there is no injection surface.
    // '#' is used as the LIKE escape char (instead of '\') and applied uniformly: a single '\' in
    // native SQL is mishandled by MySQL/MariaDB and Oracle has no default LIKE escape, whereas '#'
    // works across every supported database.
    protected static String escapeForLike(final String value) {
        return value == null ? null : value.replace("#", "##").replace("%", "#%").replace("_", "#_");
    }

    protected static String toOpEvent(
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome) {

        return OpEvent.toString(type, escapeForLike(category), escapeForLike(subcategory), escapeForLike(op), outcome);
    }

    protected static List<Sort.Order> filterOrderBy(
            final Stream<Sort.Order> orderByClauses,
            final Class<? extends AuditEvent> clazz) {

        return orderByClauses.
                filter(clause -> ReflectionUtils.findField(clazz, clause.getProperty().trim()) != null).
                toList();
    }
}
