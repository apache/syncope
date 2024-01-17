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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.widgets.AnyByRealmWidget;
import org.apache.syncope.client.console.widgets.CompletenessWidget;
import org.apache.syncope.client.console.widgets.LoadWidget;
import org.apache.syncope.client.console.widgets.NumberWidget;
import org.apache.syncope.client.console.widgets.UsersByStatusWidget;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class DashboardOverviewPanel extends Panel {

    private static final long serialVersionUID = 5989039374050260225L;

    private final NumberWidget totalUsers;

    private final NumberWidget totalGroups;

    private final NumberWidget totalAny1OrRoles;

    private final NumberWidget totalAny2OrResources;

    private final UsersByStatusWidget usersByStatus;

    private final CompletenessWidget completeness;

    private final AnyByRealmWidget anyByRealm;

    private final LoadWidget load;

    public DashboardOverviewPanel(final String id) {
        super(id);

        NumbersInfo numbers = SyncopeConsoleSession.get().getAnonymousClient().numbers();

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        totalUsers = new NumberWidget("totalUsers", "text-bg-warning", numbers.getTotalUsers(),
                new ResourceModel("users").getObject(), "ion ion-person");
        container.add(totalUsers);
        totalGroups = new NumberWidget(
                "totalGroups", "text-bg-danger", numbers.getTotalGroups(),
                new ResourceModel("groups").getObject(), "ion ion-person-stalker");
        container.add(totalGroups);

        Triple<Long, String, String> built = buildTotalAny1OrRoles(numbers);
        totalAny1OrRoles = new NumberWidget(
                "totalAny1OrRoles", "text-bg-success", built.getLeft(), built.getMiddle(), built.getRight());
        container.add(totalAny1OrRoles);

        built = buildTotalAny2OrResources(numbers);
        totalAny2OrResources = new NumberWidget(
                "totalAny2OrResources", "bg-info", built.getLeft(), built.getMiddle(), built.getRight());
        container.add(totalAny2OrResources);

        usersByStatus = new UsersByStatusWidget("usersByStatus", numbers.getUsersByStatus());
        container.add(usersByStatus);

        completeness = new CompletenessWidget("completeness", numbers.getConfCompleteness());
        container.add(completeness);

        anyByRealm = new AnyByRealmWidget(
                "anyByRealm",
                numbers.getUsersByRealm(),
                numbers.getGroupsByRealm(),
                numbers.getAnyType1(),
                numbers.getAny1ByRealm(),
                numbers.getAnyType2(),
                numbers.getAny2ByRealm());
        container.add(anyByRealm);

        load = new LoadWidget("load", SyncopeConsoleSession.get().getAnonymousClient().system());
        container.add(load);

        container.add(new IndicatorAjaxTimerBehavior(Duration.of(60, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = -4426283634345968585L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                NumbersInfo numbers = SyncopeConsoleSession.get().getAnonymousClient().numbers();

                if (totalUsers.refresh(numbers.getTotalUsers())) {
                    target.add(totalUsers);
                }
                if (totalGroups.refresh(numbers.getTotalGroups())) {
                    target.add(totalGroups);
                }

                Triple<Long, String, String> updatedBuild = buildTotalAny1OrRoles(numbers);
                if (totalAny1OrRoles.refresh(updatedBuild.getLeft())) {
                    target.add(totalAny1OrRoles);
                }
                updatedBuild = buildTotalAny2OrResources(numbers);
                if (totalAny2OrResources.refresh(updatedBuild.getLeft())) {
                    target.add(totalAny2OrResources);
                }

                if (usersByStatus.refresh(numbers.getUsersByStatus())) {
                    target.add(usersByStatus);
                }

                if (completeness.refresh(numbers.getConfCompleteness())) {
                    target.add(completeness);
                }

                if (anyByRealm.refresh(
                        numbers.getUsersByRealm(),
                        numbers.getGroupsByRealm(),
                        numbers.getAnyType1(),
                        numbers.getAny1ByRealm(),
                        numbers.getAnyType2(),
                        numbers.getAny2ByRealm())) {

                    target.add(anyByRealm);
                }

                load.refresh(SyncopeConsoleSession.get().getAnonymousClient().system());
                target.add(load);
            }
        });
    }

    private static Triple<Long, String, String> buildTotalAny1OrRoles(final NumbersInfo numbers) {
        long number;
        String label;
        String icon;
        if (numbers.getAnyType1() == null) {
            number = numbers.getTotalRoles();
            label = new ResourceModel("roles").getObject();
            icon = "fas fa-users";
        } else {
            number = numbers.getTotalAny1();
            label = numbers.getAnyType1();
            icon = "ion ion-gear-a";
        }
        return Triple.of(number, label, icon);
    }

    private static Triple<Long, String, String> buildTotalAny2OrResources(final NumbersInfo numbers) {
        long number;
        String label;
        String icon;
        if (numbers.getAnyType2() == null) {
            number = numbers.getTotalResources();
            label = new ResourceModel("resources").getObject();
            icon = "fa fa-database";
        } else {
            number = numbers.getTotalAny2();
            label = numbers.getAnyType2();
            icon = "ion ion-gear-a";
        }
        return Triple.of(number, label, icon);
    }
}
