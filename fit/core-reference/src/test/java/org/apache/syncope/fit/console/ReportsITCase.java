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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ReportsITCase extends AbstractConsoleITCase {

    private void createReport(final String name) {
        wicketTester.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:name:textField", name);
        formTester.setValue("content:form:view:template:dropDownChoiceField", "0");
        formTester.submit("content:form:buttons:next");

        wicketTester.assertComponent("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater"
                + ":0:outer:form:content:form:view:schedule:seconds:textField", TextField.class);

        formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:reportsLI:reports");
    }

    private void delete(final String name) {
        wicketTester.clickLink("body:reportsLI:reports");

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(result);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:10:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:10:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        assertNull(findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteReport"));
    }

    private void deleteReportlet(final String report, final String reportlet) {
        wicketTester.clickLink("body:reportsLI:reports");

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", report);

        assertNotNull(result);

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:10:cell:panelCompose:composeLink");

        result = findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", reportlet);

        assertNotNull(result);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"));

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        assertNull(findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", reportlet));
    }

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:reportsLI:reports");
        wicketTester.assertRenderedPage(Reports.class);
    }

    @Test
    public void read() {
        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "test");

        assertNotNull(result);

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:10:cell:panelView:viewLink");

        result = findComponentByProp("status", "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "SUCCESS");

        assertNotNull(result);

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:6:cell:panelView:viewLink");
        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:secondLevelContainer:back");

        wicketTester.clickLink("body:content:tabbedPanel:panel:secondLevelContainer:back");

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

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:10:cell:panelCompose:composeLink");

        result = findComponentByProp("name", "body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "testUserReportlet");

        assertNotNull(result);
        wicketTester.clickLink(result.getPageRelativePath() + ":cells:3:cell:panelClone:cloneLink");

        FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");

        formTester.setValue("view:name:textField", reportlet);
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        deleteReportlet(report, reportlet);

        wicketTester.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:exit");

        wicketTester.assertRenderedPage(Reports.class);
    }

    @Test
    public void createReportlets() {
        final String report = "test";
        final String reportlet = "myNewReportlet";

        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", report);

        assertNotNull(result);

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:10:cell:panelCompose:composeLink");

        wicketTester.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:add");

        FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");

        formTester.setValue("view:name:textField", reportlet);
        formTester.setValue("view:configuration:dropDownChoiceField", "1");
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:firstLevelContainer:first:"
                + "outerObjectsRepeater:0:outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        deleteReportlet(report, reportlet);

        wicketTester.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:"
                + "outer:form:content:container:content:exit");

        wicketTester.assertRenderedPage(Reports.class);
    }

    @Test
    public void update() {
        createReport("updateReport");
        Component result = findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateReport");

        wicketTester.clickLink(result.getPageRelativePath() + ":cells:10:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:template:dropDownChoiceField", "1");

        formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        delete("updateReport");
    }
}
