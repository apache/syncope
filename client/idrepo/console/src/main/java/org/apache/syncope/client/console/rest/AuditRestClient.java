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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.events.EventCategory;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public class AuditRestClient extends BaseRestClient {

    private static final long serialVersionUID = 4579786978763032240L;

    public List<OpEvent> confs() {
        return getService(AuditService.class).confs().stream().
                map(conf -> {
                    try {
                        return OpEvent.fromString(conf.getKey());
                    } catch (Exception e) {
                        LOG.error("Unexpected when parsing {}", conf.getKey(), e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    public void enableConf(final OpEvent opEvent) {
        AuditConfTO audit = new AuditConfTO();
        audit.setKey(opEvent.toString());
        audit.setActive(true);
        getService(AuditService.class).setConf(audit);
    }

    public void deleteConf(final OpEvent opEvent) {
        try {
            getService(AuditService.class).deleteConf(opEvent.toString());
        } catch (SyncopeClientException e) {
            if (e.getType() != ClientExceptionType.NotFound) {
                LOG.error("Unexpected error when deleting {}", opEvent.toString(), e);
            }
        }
    }

    public List<EventCategory> events() {
        List<EventCategory> eventCategories = new ArrayList<>();

        try {
            getService(AuditService.class).events().forEach(opEvent -> {
                EventCategory eventCategory = eventCategories.stream().
                        filter(ec -> opEvent.getType() == ec.getType()
                        && Objects.equals(opEvent.getCategory(), ec.getCategory())
                        && Objects.equals(opEvent.getSubcategory(), ec.getSubcategory())).
                        findFirst().orElseGet(() -> {
                            EventCategory ec = new EventCategory(opEvent.getType());
                            ec.setCategory(opEvent.getCategory());
                            ec.setSubcategory(opEvent.getSubcategory());
                            eventCategories.add(ec);
                            return ec;
                        });
                if (!eventCategory.getOps().contains(opEvent.getOp())) {
                    eventCategory.getOps().add(opEvent.getOp());
                }
            });
        } catch (Exception e) {
            LOG.error("Unexpected error when listing Audit events", e);
        }

        return eventCategories;
    }

    public long count(
            final String key,
            final OpEvent.CategoryType type,
            final String category,
            final String op,
            final OpEvent.Outcome outcome) {

        AuditQuery query = new AuditQuery.Builder().
                entityKey(key).
                page(1).
                size(0).
                type(type).
                category(category).
                op(op).
                outcome(outcome).
                build();
        return getService(AuditService.class).search(query).getTotalCount();
    }

    public List<AuditEventTO> search(
            final String key,
            final int page,
            final int size,
            final OpEvent.CategoryType type,
            final String category,
            final String op,
            final OpEvent.Outcome outcome,
            final SortParam<String> sort) {

        AuditQuery query = new AuditQuery.Builder().
                entityKey(key).
                size(size).
                page(page).
                type(type).
                category(category).
                op(op).
                outcome(outcome).
                orderBy(toOrderBy(sort)).
                build();

        return getService(AuditService.class).search(query).getResult();
    }
}
