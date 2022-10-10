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
import org.apache.syncope.client.console.pages.Engagements;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.pages.Logs;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.pages.Reports;
import org.apache.syncope.client.console.pages.Security;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.topology.Topology;
import org.junit.jupiter.api.Test;

// Please, keep the class name as is in order to respect the execution order. It seems that from wicket 7.5.0 the 
// session created never expire and the unsuccessfulLogin test fail
public class AjaxBrowseITCase extends AbstractConsoleITCase {

    @Test
    public void loginPage() {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);
    }

    @Test
    public void successfulLogin() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.assertRenderedPage(Dashboard.class);
    }

    @Test
    public void unsuccessfulLogin() {
        doLogin(ADMIN_UNAME, ADMIN_PWD + 1);
        TESTER.assertRenderedPage(Login.class);
    }

    @Test
    public void browsingBookmarkablePageLink() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);

        TESTER.assertRenderedPage(Dashboard.class);

        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.assertRenderedPage(Realms.class);

        TESTER.clickLink("body:idmPages:0:idmPageLI:idmPage", false);
        TESTER.assertRenderedPage(Topology.class);

        TESTER.clickLink("body:engagementsLI:engagements", false);
        TESTER.assertRenderedPage(Engagements.class);

        TESTER.clickLink("body:reportsLI:reports", false);
        TESTER.assertRenderedPage(Reports.class);

        TESTER.clickLink("body:configurationLI:configurationUL:logsLI:logs", false);
        TESTER.assertRenderedPage(Logs.class);

        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:configurationLI:configurationUL:securityLI:security", false);
        TESTER.assertRenderedPage(Security.class);

        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies", false);
        TESTER.assertRenderedPage(Policies.class);

        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications", false);
        TESTER.assertRenderedPage(Notifications.class);
    }
}
