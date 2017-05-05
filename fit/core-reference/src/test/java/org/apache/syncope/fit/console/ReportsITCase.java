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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Reports;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.Test;

public class ReportsITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:reportsLI:reports");
        TESTER.assertRenderedPage(Reports.class);
    }

    private void createReport(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:name:textField", name);
        formTester.setValue("content:form:view:template:dropDownChoiceField", "0");
        formTester.submit("content:form:buttons:next");

        TESTER.assertComponent("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater"
                + ":0:outer:form:content:form:view:schedule:seconds:textField", TextField.class);

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:reportsLI:reports");
    }

    private void delete(final String name) {
        TESTER.clickLink("body:reportsLI:reports");

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:5:action:action");

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:5:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteReport"));
    }

    private void deleteReportlet(final String report, final String reportlet) {
        TESTER.clickLink("body:reportsLI:reports");

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", report);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        result = findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", reportlet);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");

        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:"
                + "form:content:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:2:action:action"));

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", reportlet));
    }

    @Test
    public void read() {
        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "test");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:3:action:action");

        TESTER.assertModelValue(
                "body:content:tabbedPanel:panel:secondLevelContainer:title", "Executions of report 'test'");
        result = findComponentByProp("status", "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "SUCCESS");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:secondLevelContainer:second:executions:firstLevelContainer:"
                + "first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:0:action:action");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:secondLevelContainer:back");

        TESTER.clickLink("body:content:tabbedPanel:panel:secondLevelContainer:back");

        assertNotNull(findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "reconciliation"));
    }

    @Test
    public void cloneReportlets() {
        final String report = "test";
        final String reportlet = "myClone";

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", report);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        result = findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "testUserReportlet");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form:"
                + "content:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");

        formTester.setValue("view:name:textField", reportlet);
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        deleteReportlet(report, reportlet);

        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:exit");

        TESTER.assertRenderedPage(Reports.class);
    }

    @Test
    public void createReportlets() {
        final String report = "test";
        final String reportlet = "myNewReportlet";

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", report);

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");

        formTester.setValue("view:name:textField", reportlet);
        formTester.setValue("view:configuration:dropDownChoiceField", "1");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        deleteReportlet(report, reportlet);

        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:exit");

        TESTER.assertRenderedPage(Reports.class);
    }

    @Test
    public void update() {
        createReport("updateReport");
        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateReport");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertModelValue("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:"
                + "0:outer:dialog:header:header-label", "Edit Report updateReport");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:template:dropDownChoiceField", "1");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        delete("updateReport");
    }
}
