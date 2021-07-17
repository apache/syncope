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
package org.apache.syncope.client.console.pages;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.events.EventCategoryPanel;
import org.apache.syncope.client.console.events.SelectedEventsPanel;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Audit extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    public Audit(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        List<String> events = AuditRestClient.list().stream().
                map(audit -> AuditLoggerName.buildEvent(
                audit.getType(),
                audit.getCategory(),
                audit.getSubcategory(),
                audit.getEvent(),
                audit.getResult())).
                collect(Collectors.toList());

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);

        Form<?> form = new Form<>("auditForm");
        content.add(form);

        form.add(new EventCategoryPanel(
                "auditPanel",
                AuditRestClient.listEvents(),
                new ListModel<>(events)) {

            private static final long serialVersionUID = 6113164334533550277L;

            @Override
            protected List<String> getListAuthRoles() {
                return List.of(IdRepoEntitlement.AUDIT_LIST);
            }

            @Override
            protected List<String> getChangeAuthRoles() {
                return List.of(IdRepoEntitlement.AUDIT_UPDATE);
            }

            @Override
            public void onEventAction(final IEvent<?> event) {
                if (event.getPayload() instanceof SelectedEventsPanel.EventSelectionChanged) {
                    final SelectedEventsPanel.EventSelectionChanged eventSelectionChanged =
                            (SelectedEventsPanel.EventSelectionChanged) event.getPayload();

                    eventSelectionChanged.getToBeRemoved().forEach(toBeRemoved -> {
                        Pair<EventCategory, AuditElements.Result> eventCategory =
                                AuditLoggerName.parseEventCategory(toBeRemoved);

                        AuditLoggerName auditLoggerName = new AuditLoggerName(
                                eventCategory.getKey().getType(),
                                eventCategory.getKey().getCategory(),
                                eventCategory.getKey().getSubcategory(),
                                CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                ? null : eventCategory.getKey().getEvents().iterator().next(),
                                eventCategory.getValue());

                        AuditRestClient.disableAudit(auditLoggerName);
                    });

                    eventSelectionChanged.getToBeAdded().forEach(toBeAdded -> {
                        Pair<EventCategory, AuditElements.Result> eventCategory =
                                AuditLoggerName.parseEventCategory(toBeAdded);

                        AuditLoggerName auditLoggerName = new AuditLoggerName(
                                eventCategory.getKey().getType(),
                                eventCategory.getKey().getCategory(),
                                eventCategory.getKey().getSubcategory(),
                                CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                ? null : eventCategory.getKey().getEvents().iterator().next(),
                                eventCategory.getValue());

                        AuditRestClient.enableAudit(auditLoggerName);
                    });
                }
            }
        });

        body.add(content);
    }
}
