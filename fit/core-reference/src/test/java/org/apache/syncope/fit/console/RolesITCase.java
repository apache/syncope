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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Security;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RolesITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:securityLI:security", false);
        TESTER.assertRenderedPage(Security.class);
    }

    private static void createRole(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:key:textField", name);
        formTester.submit("content:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:entitlements:paletteField:recorder",
                "WORKFLOW_DEF_READ,NOTIFICATION_UPDATE,RELATIONSHIPTYPE_READ,RELATIONSHIPTYPE_LIST");
        formTester.submit("content:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:configurationLI:configurationUL:securityLI:security", false);
    }

    @Test
    public void read() {
        Component result = findComponentByProp(KEY,
                "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "Other");
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertModelValue(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:header:header-label",
                "Role 'Other' members");

        assertNotNull(findComponentByProp("username",
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:"
                + "content:searchResult:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "rossini"));

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);
    }

    @Test
    public void create() {
        createRole("testRole");
    }

    @Test
    public void update() {
        createRole("updateRole");
        Component result = findComponentByProp(KEY,
                "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateRole");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:"
                + "container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:key:textField", "updateRole");
        formTester.submit("content:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:entitlements:paletteField:recorder",
                "WORKFLOW_DEF_READ,NOTIFICATION_UPDATE");
        formTester.submit("content:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void delete() {
        createRole("deleteRole");
        Component result = findComponentByProp(KEY,
                "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteRole");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:"
                + "container:actions:actions:actionRepeater:4:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:"
                + "container:actions:actions:actionRepeater:4:action:action"), "onclick");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp(KEY,
                "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteRole"));
    }
}
