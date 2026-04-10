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
package org.apache.syncope.client.console.panels;

import java.util.concurrent.TimeUnit;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.ws.BasePageWebSocketBehavior;
import org.apache.syncope.client.console.widgets.AnyByRealmWidget;
import org.apache.syncope.client.console.widgets.CompletenessWidget;
import org.apache.syncope.client.console.widgets.LoadWidget;
import org.apache.syncope.client.console.widgets.NumberWidget;
import org.apache.syncope.client.console.widgets.UsersByStatusWidget;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;

public class DashboardOverviewPanel extends Panel {

    private record TotalInfo(Long number, String label, String icon) {

    }

    private static TotalInfo buildTotalAny1OrRoles(final NumbersInfo numbers) {
        long number;
        String label;
        String icon;
        if (numbers.anyType1() == null) {
            number = numbers.totalRoles();
            label = new ResourceModel("roles").getObject();
            icon = "fas fa-users";
        } else {
            number = numbers.totalAny1();
            label = numbers.anyType1();
            icon = "ion ion-gear-a";
        }
        return new TotalInfo(number, label, icon);
    }

    private static TotalInfo buildTotalAny2OrResources(final NumbersInfo numbers) {
        long number;
        String label;
        String icon;
        if (numbers.anyType2() == null) {
            number = numbers.totalResources();
            label = new ResourceModel("resources").getObject();
            icon = "fa fa-database";
        } else {
            number = numbers.totalAny2();
            label = numbers.anyType2();
            icon = "ion ion-gear-a";
        }
        return new TotalInfo(number, label, icon);
    }

    private static final long serialVersionUID = 5989039374050260225L;

    private final NumberWidget totalUsers;

    private final NumberWidget totalGroups;

    private final NumberWidget totalAny1OrRoles;

    private final NumberWidget totalAny2OrResources;

    private final UsersByStatusWidget usersByStatus;

    private final CompletenessWidget completeness;

    private final AnyByRealmWidget anyByRealm;

    private final LoadWidget load;

    public DashboardOverviewPanel(final String id, final PageReference pageRef) {
        super(id);

        NumbersInfo numbers = SyncopeConsoleSession.get().getAnonymousClient().numbers();

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        totalUsers = new NumberWidget("totalUsers", "text-bg-warning", numbers.totalUsers(),
                new ResourceModel("users").getObject(), "ion ion-person");
        container.add(totalUsers);
        totalGroups = new NumberWidget(
                "totalGroups", "text-bg-danger", numbers.totalGroups(),
                new ResourceModel("groups").getObject(), "ion ion-person-stalker");
        container.add(totalGroups);

        TotalInfo built = buildTotalAny1OrRoles(numbers);
        totalAny1OrRoles = new NumberWidget(
                "totalAny1OrRoles", "text-bg-success", built.number(), built.label(), built.icon());
        container.add(totalAny1OrRoles);

        built = buildTotalAny2OrResources(numbers);
        totalAny2OrResources = new NumberWidget(
                "totalAny2OrResources", "bg-info", built.number(), built.label(), built.icon());
        container.add(totalAny2OrResources);

        usersByStatus = new UsersByStatusWidget("usersByStatus", numbers.usersByStatus());
        container.add(usersByStatus);

        completeness = new CompletenessWidget("completeness", numbers.confCompleteness());
        container.add(completeness);

        anyByRealm = new AnyByRealmWidget(
                "anyByRealm",
                numbers.usersByRealm(),
                numbers.groupsByRealm(),
                numbers.anyType1(),
                numbers.any1ByRealm(),
                numbers.anyType2(),
                numbers.any2ByRealm());
        container.add(anyByRealm);

        load = new LoadWidget("load", SyncopeConsoleSession.get().getAnonymousClient().system());
        container.add(load);

        pageRef.getPage().getBehaviors().stream().
                filter(BasePageWebSocketBehavior.class::isInstance).map(BasePageWebSocketBehavior.class::cast).
                findFirst().ifPresent(wsb -> wsb.add(new BasePageWebSocketBehavior.OnTimerChild(60, TimeUnit.SECONDS) {

            private static final long serialVersionUID = -7095269057058900157L;

            @Override
            protected void onTimer(final WebSocketRequestHandler handler) {
                NumbersInfo numbers = SyncopeConsoleSession.get().getAnonymousClient().numbers();

                if (totalUsers.refresh(numbers.totalUsers())) {
                    handler.add(totalUsers);
                }
                if (totalGroups.refresh(numbers.totalGroups())) {
                    handler.add(totalGroups);
                }

                TotalInfo updatedBuild = buildTotalAny1OrRoles(numbers);
                if (totalAny1OrRoles.refresh(updatedBuild.number())) {
                    handler.add(totalAny1OrRoles);
                }
                updatedBuild = buildTotalAny2OrResources(numbers);
                if (totalAny2OrResources.refresh(updatedBuild.number())) {
                    handler.add(totalAny2OrResources);
                }

                if (usersByStatus.refresh(numbers.usersByStatus())) {
                    handler.add(usersByStatus);
                }

                if (completeness.refresh(numbers.confCompleteness())) {
                    handler.add(completeness);
                }

                if (anyByRealm.refresh(
                        numbers.usersByRealm(),
                        numbers.groupsByRealm(),
                        numbers.anyType1(),
                        numbers.any1ByRealm(),
                        numbers.anyType2(),
                        numbers.any2ByRealm())) {

                    handler.add(anyByRealm);
                }

                load.refresh(SyncopeConsoleSession.get().getAnonymousClient().system());
                handler.add(load);
            }
        }));
    }
}
