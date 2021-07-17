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
package org.apache.syncope.client.console.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.syncope.common.rest.api.service.AuditService;

public class AuditRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public static List<AuditLoggerName> list() {
        return getService(AuditService.class).list().stream().
                map(a -> {
                    try {
                        return AuditLoggerName.fromAuditKey(a.getKey());
                    } catch (Exception e) {
                        LOG.error("Unexpected when parsing {}", a.getKey(), e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    public static Map<String, Set<AuditLoggerName>> listAuditsByCategory() {
        Map<String, Set<AuditLoggerName>> result = new HashMap<>();
        list().forEach(audit -> {
            if (!result.containsKey(audit.getCategory())) {
                result.put(audit.getCategory(), new HashSet<>());
            }

            result.get(audit.getCategory()).add(audit);
        });

        return result;
    }

    public static void enableAudit(final AuditLoggerName auditLoggerName) {
        AuditConfTO audit = new AuditConfTO();
        audit.setKey(auditLoggerName.toAuditKey());
        audit.setActive(true);
        getService(AuditService.class).update(audit);
    }

    public static void disableAudit(final AuditLoggerName auditLoggerName) {
        AuditConfTO audit = new AuditConfTO();
        audit.setKey(auditLoggerName.toAuditKey());
        audit.setActive(false);
        getService(AuditService.class).update(audit);
    }

    public static List<EventCategory> listEvents() {
        try {
            return getService(AuditService.class).events();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<AuditEntry> search(
            final String key,
            final int page,
            final int size,
            final AuditElements.EventCategoryType type,
            final String category,
            final List<String> events,
            final AuditElements.Result result,
            final SortParam<String> sort) {

        AuditQuery query = new AuditQuery.Builder().
                entityKey(key).
                size(size).
                page(page).
                type(type).
                category(category).
                events(events).
                result(result).
                orderBy(toOrderBy(sort)).
                build();

        return getService(AuditService.class).search(query).getResult();
    }

    public int count(
            final String key,
            final AuditElements.EventCategoryType type,
            final String category,
            final List<String> events,
            final AuditElements.Result result) {

        AuditQuery query = new AuditQuery.Builder().
                entityKey(key).
                page(1).
                size(0).
                type(type).
                category(category).
                events(events).
                result(result).
                build();
        return getService(AuditService.class).search(query).getTotalCount();
    }
}
