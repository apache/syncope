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
import org.apache.syncope.client.console.pages.Administration;
import org.apache.syncope.client.console.pages.SecurityQuestions;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.pages.Workflow;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.fit.FlowableDetector;
import org.junit.Test;

// Please, keep the class name as is in order to respect the execution order. It seems that from wicket 7.5.0 the 
// session creted never expire and the unsuccessfulLogin test fail
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

        TESTER.clickLink("body:realmsLI:realms");
        TESTER.assertRenderedPage(Realms.class);

        TESTER.clickLink("body:topologyLI:topology");
        TESTER.assertRenderedPage(Topology.class);

        TESTER.clickLink("body:reportsLI:reports");
        TESTER.assertRenderedPage(Reports.class);

        if (FlowableDetector.isFlowableEnabledForUsers(SYNCOPE_SERVICE)) {
            TESTER.clickLink("body:configurationLI:configurationUL:workflowLI:workflow");
            TESTER.assertRenderedPage(Workflow.class);
        }

        TESTER.clickLink("body:configurationLI:configurationUL:logsLI:logs");
        TESTER.assertRenderedPage(Logs.class);

        TESTER.clickLink("body:configurationLI:configurationUL:securityquestionsLI:securityquestions");
        TESTER.assertRenderedPage(SecurityQuestions.class);

        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types");
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:configurationLI:configurationUL:administrationLI:administration");
        TESTER.assertRenderedPage(Administration.class);

        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        TESTER.assertRenderedPage(Policies.class);

        TESTER.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
        TESTER.assertRenderedPage(Notifications.class);
    }
}
