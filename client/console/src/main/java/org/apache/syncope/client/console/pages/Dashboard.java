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

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.widgets.NumberWidget;
import org.apache.syncope.client.console.widgets.AnyByRealmWidget;
import org.apache.syncope.client.console.widgets.CompletenessWidget;
import org.apache.syncope.client.console.widgets.LoadWidget;
import org.apache.syncope.client.console.widgets.UsersByStatusWidget;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Dashboard extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    public Dashboard(final PageParameters parameters) {
        super(parameters);

        NumbersInfo numbers = SyncopeConsoleSession.get().getService(SyncopeService.class).numbers();

        body.add(new NumberWidget(
                "totalUsers", "bg-yellow", numbers.getTotalUsers(), getString("users"), "ion ion-person"));
        body.add(new NumberWidget(
                "totalGroups", "bg-red", numbers.getTotalGroups(), getString("groups"), "ion ion-person-stalker"));

        int number;
        String label;
        String icon;
        if (numbers.getAnyType1() == null) {
            number = numbers.getTotalRoles();
            label = getString("roles");
            icon = "fa fa-users";
        } else {
            number = numbers.getTotalAny1();
            label = numbers.getAnyType1();
            icon = "ion ion-gear-a";
        }
        body.add(new NumberWidget("totalAny1OrRoles", "bg-green", number, label, icon));

        if (numbers.getAnyType2() == null) {
            number = numbers.getTotalResources();
            label = getString("resources");
            icon = "fa fa-database";
        } else {
            number = numbers.getTotalAny2();
            label = numbers.getAnyType2();
            icon = "ion ion-gear-a";
        }
        body.add(new NumberWidget("totalAny1OrResources", "bg-aqua", number, label, icon));

        body.add(new UsersByStatusWidget("usersByStatus", numbers.getUsersByStatus()));
        body.add(new CompletenessWidget("completeness", numbers.getConfCompleteness()));
        body.add(new AnyByRealmWidget(
                "anyByRealm",
                numbers.getUsersByRealm(),
                numbers.getGroupsByRealm(),
                numbers.getAnyType1(),
                numbers.getAny1ByRealm(),
                numbers.getAnyType2(),
                numbers.getAny2ByRealm()));

        body.add(new LoadWidget("load", SyncopeConsoleSession.get().getService(SyncopeService.class).system()));
    }
}
