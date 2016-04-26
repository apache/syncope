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
package org.apache.syncope.fit.console;

import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.pages.Logs;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.pages.Reports;
import org.apache.syncope.client.console.pages.Roles;
import org.apache.syncope.client.console.pages.SecurityQuestions;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.pages.Workflow;
import org.apache.syncope.client.console.topology.Topology;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class BaseITCase extends AbstractConsoleITCase {

    @Test
    public void loginPage() {
        wicketTester.startPage(Login.class);
        wicketTester.assertRenderedPage(Login.class);
    }

    @Test
    public void successfullyLogin() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.assertRenderedPage(Dashboard.class);
    }

    @Test
    public void unsuccessfullyLogin() {
        doLogin(ADMIN_UNAME, ADMIN_PWD + 1);
        wicketTester.assertRenderedPage(Login.class);
    }

    @Test
    public void browsingBookmarkablePageLink() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.assertRenderedPage(Dashboard.class);

        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.assertRenderedPage(Realms.class);

        wicketTester.clickLink("body:topologyLI:topology");
        wicketTester.assertRenderedPage(Topology.class);

        wicketTester.clickLink("body:reportsLI:reports");
        wicketTester.assertRenderedPage(Reports.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:workflowLI:workflow");
        wicketTester.assertRenderedPage(Workflow.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:logsLI:logs");
        wicketTester.assertRenderedPage(Logs.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:securityquestionsLI:securityquestions");
        wicketTester.assertRenderedPage(SecurityQuestions.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:typesLI:types");
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:rolesLI:roles");
        wicketTester.assertRenderedPage(Roles.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        wicketTester.assertRenderedPage(Policies.class);

        wicketTester.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
        wicketTester.assertRenderedPage(Notifications.class);
    }
}
