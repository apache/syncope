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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.events.EventCategoryPanel;
import org.apache.syncope.client.console.events.SelectedEventsPanel;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.StandardEntitlement;
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

        final LoggerRestClient loggerRestClient = new LoggerRestClient();

        List<String> events = new ArrayList<>();
        for (AuditLoggerName audit : loggerRestClient.listAudits()) {
            events.add(AuditLoggerName.buildEvent(
                    audit.getType(),
                    audit.getCategory(),
                    audit.getSubcategory(),
                    audit.getEvent(),
                    audit.getResult()));
        }

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);

        Form<?> form = new Form<>("auditForm");
        content.add(form);

        form.add(new EventCategoryPanel(
                "auditPanel",
                loggerRestClient.listEvents(),
                new ListModel<>(events)) {

            private static final long serialVersionUID = 6113164334533550277L;

            @Override
            protected List<String> getListAuthRoles() {
                return Collections.singletonList(StandardEntitlement.AUDIT_LIST);
            }

            @Override
            protected List<String> getChangeAuthRoles() {
                return Arrays.asList(
                        new String[] { StandardEntitlement.AUDIT_ENABLE, StandardEntitlement.AUDIT_DISABLE });
            }

            @Override
            public void onEventAction(final IEvent<?> event) {
                if (event.getPayload() instanceof SelectedEventsPanel.EventSelectionChanged) {
                    final SelectedEventsPanel.EventSelectionChanged eventSelectionChanged =
                            (SelectedEventsPanel.EventSelectionChanged) event.getPayload();

                    for (String toBeRemoved : eventSelectionChanged.getToBeRemoved()) {
                        Pair<EventCategoryTO, AuditElements.Result> eventCategory =
                                AuditLoggerName.parseEventCategory(toBeRemoved);

                        AuditLoggerName auditLoggerName = new AuditLoggerName(
                                eventCategory.getKey().getType(),
                                eventCategory.getKey().getCategory(),
                                eventCategory.getKey().getSubcategory(),
                                CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                ? null : eventCategory.getKey().getEvents().iterator().next(),
                                eventCategory.getValue());

                        loggerRestClient.disableAudit(auditLoggerName);
                    }

                    for (String toBeAdded : eventSelectionChanged.getToBeAdded()) {
                        Pair<EventCategoryTO, AuditElements.Result> eventCategory =
                                AuditLoggerName.parseEventCategory(toBeAdded);

                        AuditLoggerName auditLoggerName = new AuditLoggerName(
                                eventCategory.getKey().getType(),
                                eventCategory.getKey().getCategory(),
                                eventCategory.getKey().getSubcategory(),
                                CollectionUtils.isEmpty(eventCategory.getKey().getEvents())
                                ? null : eventCategory.getKey().getEvents().iterator().next(),
                                eventCategory.getValue());

                        loggerRestClient.enableAudit(auditLoggerName);
                    }
                }
            }
        });

        body.add(content);
    }
}
