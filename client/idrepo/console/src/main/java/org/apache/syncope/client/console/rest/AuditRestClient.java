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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class AuditRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public List<AuditLoggerName> list() {
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

    public void enable(final AuditLoggerName auditLoggerName) {
        AuditConfTO audit = new AuditConfTO();
        audit.setKey(auditLoggerName.toAuditKey());
        audit.setActive(true);
        getService(AuditService.class).set(audit);
    }

    public void delete(final AuditLoggerName auditLoggerName) {
        try {
            getService(AuditService.class).delete(auditLoggerName.toAuditKey());
        } catch (SyncopeClientException e) {
            if (e.getType() != ClientExceptionType.NotFound) {
                LOG.error("Unexpected error when deleting {}", auditLoggerName.toAuditKey(), e);
            }
        }
    }

    public List<EventCategory> listEvents() {
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
