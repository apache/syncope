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

import static org.apache.syncope.fit.console.AbstractConsoleITCase.TESTER;
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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class BulkActionITCase extends AbstractConsoleITCase {

    private final String tabPanel = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private final String searchResultContainer = tabPanel + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void usersBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(searchResultContainer
                + "searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 2);

        TESTER.executeAjaxEvent(searchResultContainer + "searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(searchResultContainer
                + "searchContainer:resultTable:bulkModal:form:content:content:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("username", searchResultContainer
                + "searchContainer:resultTable:bulkModal:form:content:content:container", "rossini"));
    }

    @Test
    public void userResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath()
                + ":cells:6:cell:panelManageResources:manageResourcesLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "resource-csv");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 2);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resourceName", tabPanel + "outerObjectsRepeater:1:outer:"
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

    private void userStatusBulkAction(final int index, final String resourceName) {
        // suspend 
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEnable:enableLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resourceName);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.ACTIVE, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resourceName, StatusBean.class.cast(component.getDefaultModelObject()).getResourceName());

        FormTester formTester = TESTER.newFormTester(
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", index);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:"
                + "status:secondLevelContainer:second:container:actions:panelSuspend:suspendLink",
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container:selectedObjects:body:rows:1:cells:3:cell", "SUCCESS");

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:secondLevelContainer:back",
                Constants.ON_CLICK);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resourceName);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.SUSPENDED, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resourceName, StatusBean.class.cast(component.getDefaultModelObject()).getResourceName());

        // re-activate
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEnable:enableLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        formTester = TESTER.newFormTester(
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", index);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:"
                + "status:secondLevelContainer:second:container:actions:panelReactivate:reactivateLink",
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container:selectedObjects:body:rows:1:cells:3:cell", "SUCCESS");

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:secondLevelContainer:back",
                Constants.ON_CLICK);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:first:container:"
                + "content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", resourceName);

        component = TESTER.getComponentFromLastRenderedPage(component.getPageRelativePath() + ":cells:1:cell:check");
        assertEquals(Status.ACTIVE, StatusBean.class.cast(component.getDefaultModelObject()).getStatus());
        assertEquals(resourceName, StatusBean.class.cast(component.getDefaultModelObject()).getResourceName());

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);
    }

    @Test
    public void groupResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        Component component = findComponentByProp("name", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath()
                + ":cells:4:cell:panelManageResources:manageResourcesLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        TESTER.clickLink(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:first:"
                + "container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:2:header:orderByLink", true);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "ws-target-resource-1");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 7);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resourceName", tabPanel + "outerObjectsRepeater:1:outer:"
                + "form:content:status:secondLevelContainer:second:container:selectedObjects", "resource-testdb2"));
    }

    @Test
    public void printerResourceBulkAction() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

        Component component = findComponentByProp("key", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath()
                + ":cells:4:cell:panelManageResources:manageResourcesLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", WebMarkupContainer.class);

        TESTER.clickLink(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:first:"
                + "container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:2:header:orderByLink", true);

        component = findComponentByProp("resourceName",
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "ws-target-resource-1");
        assertNotNull(component);

        FormTester formTester = TESTER.newFormTester(
                tabPanel + "outerObjectsRepeater:1:outer:form:content:status:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm");
        assertNotNull(formTester);

        formTester.select("checkgroup", 7);

        TESTER.executeAjaxEvent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:bulkActionLink",
                Constants.ON_CLICK);

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:1:outer:form:content:status:"
                + "secondLevelContainer:second:container", WebMarkupContainer.class);

        assertNotNull(findComponentByProp("resourceName", tabPanel + "outerObjectsRepeater:1:outer:"
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
