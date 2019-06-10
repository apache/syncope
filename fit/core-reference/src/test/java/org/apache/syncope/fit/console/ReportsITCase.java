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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.Reports;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        UTILITY_UI.getTester().clickLink("body:reportsLI:reports");
        UTILITY_UI.getTester().assertRenderedPage(Reports.class);
    }

    private void createReport(final String name) {
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:container:content:add");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:name:textField", name);
        formTester.setValue("content:form:view:template:dropDownChoiceField", "0");
        formTester.submit("content:form:buttons:next");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater"
                + ":0:outer:form:content:form:view:schedule:seconds:textField", TextField.class);

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink("body:reportsLI:reports");
    }

    private void delete(final String name) {
        UTILITY_UI.getTester().clickLink("body:reportsLI:reports");

        Component result = UTILITY_UI.findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:5:action:action");

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:5:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "deleteReport"));
    }

    @Test
    public void read() {
        Component result = UTILITY_UI.findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "test");

        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:3:action:action");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:tabbedPanel:panel:secondLevelContainer:title", "Executions of report 'test'");
        result = UTILITY_UI.findComponentByProp("status",
                "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "SUCCESS");

        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:firstLevelContainer:"
                + "first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:secondLevelContainer:second:executions:secondLevelContainer:back");

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:secondLevelContainer:back");

        assertNotNull(UTILITY_UI.findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "reconciliation"));
    }

    @Test
    public void update() {
        createReport("updateReport");
        Component result = UTILITY_UI.findComponentByProp(
                "name", "body:content:tabbedPanel:panel:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "updateReport");

        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:"
                + "0:outer:dialog:header:header-label", "Edit Report updateReport");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:template:dropDownChoiceField", "1");

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        delete("updateReport");
    }
}
