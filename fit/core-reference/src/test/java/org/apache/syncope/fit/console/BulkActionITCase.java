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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.Status;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.Test;

public class BulkActionITCase extends AbstractConsoleITCase {

    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void usersBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(CONTAINER + "searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 2);

        TESTER.executeAjaxEvent(CONTAINER + "searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(CONTAINER
                + "searchContainer:resultTable:bulkModal:form:content:content:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("username", CONTAINER
                + "searchContainer:resultTable:bulkModal:form:content:content:container", "rossini"));
    }

    @Test
    public void userResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        // manage resource
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:1"
                + ":outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:5:"
                + "action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "resource-csv");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 2);

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resource", TAB_PANEL + "outerObjectsRepeater:2:outer:"
                + "form:content:status:secondLevelContainer:second:container", "resource-csv"));
    }

    @Test
    public void userStatusBulkAction() {
        userStatusBulkAction(1, "resource-testdb2");
    }

    @Test
    public void userStatusOnSyncopeOnlyBulkAction() {
        userStatusBulkAction(0, "Syncope");
    }

    private void userStatusBulkAction(final int index, final String resource) {
        // suspend 
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        // enable
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:1"
                + ":outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:4:"
                + "action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resource);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.ACTIVE, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resource, StatusBean.class.cast(component.getDefaultModelObject()).getResource());

        FormTester formTester = TESTER.newFormTester(
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", index);

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        // suspend link
        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:secondLevelContainer:"
                + "second:container:actions:actionRepeater:0:action:action",
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container:selectedObjects:body:rows:1:cells:3:cell", "SUCCESS");

        TESTER.executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:2:outer:form:content:status:secondLevelContainer:back",
                Constants.ON_CLICK);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resource);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.SUSPENDED, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resource, StatusBean.class.cast(component.getDefaultModelObject()).getResource());

        // re-activate
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        // enable
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:1"
                + ":outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:4:"
                + "action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        formTester = TESTER.newFormTester(
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", index);

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        // suspend link
        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:secondLevelContainer:"
                + "second:container:actions:actionRepeater:1:action:action",
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container:selectedObjects:body:rows:1:cells:3:cell", "SUCCESS");

        TESTER.executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:2:outer:form:content:status:secondLevelContainer:back",
                Constants.ON_CLICK);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resource);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.ACTIVE, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resource, StatusBean.class.cast(component.getDefaultModelObject()).getResource());

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);
    }

    @Test
    public void groupResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        Component component = findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        // manage resource
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:1"
                + ":outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:6:"
                + "action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:first:"
                + "container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:2:header:orderByLink", true);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "ws-target-resource-1");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 7);

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resource", TAB_PANEL + "outerObjectsRepeater:2:outer:"
                + "form:content:status:secondLevelContainer:second:container:selectedObjects", "resource-testdb2"));
    }

    @Test
    public void printerResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

        Component component = findComponentByProp("key", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        // manage resource
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:1"
                + ":outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:2:"
                + "action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:first:"
                + "container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:2:header:orderByLink", true);

        component = findComponentByProp("resource",
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "ws-target-resource-1");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 7);

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:2:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resource", TAB_PANEL + "outerObjectsRepeater:2:outer:"
                + "form:content:status:secondLevelContainer:second:container:selectedObjects", "resource-testdb2"));
    }

    @Test
    public void executePropagationTask() {
        TESTER.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 0);

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:"
                + "second:container:selectedObjects:body:rows:1:cells:1:cell", Label.class);
    }
}
