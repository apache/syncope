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

import static org.junit.Assert.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Roles;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RolesITCase extends AbstractConsoleITCase {

    private void createRole(final String name) {
        wicketTester.clickLink("body:content:rolesPanel:container:content:add");

        wicketTester.assertComponent("body:content:rolesPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:key:textField", name);
        formTester.submit("content:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:entitlements:paletteField:recorder",
                "WORKFLOW_DEF_READ,NOTIFICATION_UPDATE,RELATIONSHIPTYPE_READ,RELATIONSHIPTYPE_LIST");
        formTester.submit("content:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:configurationLI:configurationUL:rolesLI:roles");
    }

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:configurationLI:configurationUL:rolesLI:roles");
        wicketTester.assertRenderedPage(Roles.class);
    }

    @Test
    public void read() {
        assertNull(findComponentByProp(KEY, "body:content:rolesPanel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "OTHER"));
    }

    @Test
    public void create() {
        createRole("testRole");
    }

    @Test
    public void update() {
        createRole("updateRole");
        Component result = findComponentByProp(KEY, "body:content:rolesPanel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateRole");

        wicketTester.assertLabel(
                result.getPageRelativePath() + ":cells:1:cell", "updateRole");

        wicketTester.clickLink(
                result.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:key:textField", "updateRole");
        formTester.submit("content:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:entitlements:paletteField:recorder",
                "WORKFLOW_DEF_READ,NOTIFICATION_UPDATE");
        formTester.submit("content:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:rolesPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void delete() {
        createRole("deleteRole");
        Component result = findComponentByProp(KEY, "body:content:rolesPanel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteRole");

        wicketTester.assertLabel(
                result.getPageRelativePath() + ":cells:1:cell", "deleteRole");

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"), "onclick");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        assertNull(findComponentByProp(KEY, "body:content:rolesPanel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteRole"));
    }
}
