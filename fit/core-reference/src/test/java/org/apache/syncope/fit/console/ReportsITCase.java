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
import org.apache.syncope.client.console.pages.Reports;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:reportsLI:reports", false);
        TESTER.assertRenderedPage(Reports.class);
    }

    private static void createReport(final String name) {
        TESTER.clickLink(
                "body:content:reportsPanel:firstLevelContainer:first:container:content:add");

        TESTER.assertComponent(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:name:textField", name);
        formTester.setValue("content:form:view:mimeType:textField", "application/pdf");
        formTester.setValue("content:form:view:fileExt:textField", "pdf");
        formTester.select("content:form:view:jobDelegate:dropDownChoiceField", 0);
        formTester.submit("content:form:buttons:next");

        TESTER.assertComponent(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater"
                + ":0:outer:form:content:form:view:schedule:seconds:textField", TextField.class);

        formTester = TESTER.newFormTester(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:reportsLI:reports", false);
    }

    private static void delete(final String name) {
        TESTER.clickLink("body:reportsLI:reports", false);

        Component result = findComponentByProp(
                "name", "body:content:reportsPanel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:4:action:action");

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp(
                "name", "body:content:reportsPanel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteReport"));
    }

    @Test
    public void read() {
        Component result = findComponentByProp(
                "name", "body:content:reportsPanel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "test");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertModelValue(
                "body:content:reportsPanel:secondLevelContainer:title", "Executions of report 'test'");
        result = findComponentByProp("status",
                "body:content:reportsPanel:secondLevelContainer:second:executions:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "SUCCESS");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:reportsPanel:secondLevelContainer:second:executions:firstLevelContainer:"
                + "first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:0:action:action");

        TESTER.clickLink(
                "body:content:reportsPanel:secondLevelContainer:second:executions:secondLevelContainer:back");

        TESTER.clickLink("body:content:reportsPanel:secondLevelContainer:back");

        assertNotNull(findComponentByProp(
                "name", "body:content:reportsPanel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "test"));
    }

    @Test
    public void update() {
        createReport("updateReport");
        Component result = findComponentByProp(
                "name", "body:content:reportsPanel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateReport");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertModelValue(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:"
                + "0:outer:dialog:header:header-label", "Edit Report updateReport");

        FormTester formTester = TESTER.newFormTester(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:mimeType:textField", "application/csv");
        formTester.setValue("content:form:view:fileExt:textField", "csv");

        formTester = TESTER.newFormTester(
                "body:content:reportsPanel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        delete("updateReport");
    }
}
