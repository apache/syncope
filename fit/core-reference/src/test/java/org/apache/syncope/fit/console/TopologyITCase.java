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
import static org.junit.Assert.fail;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import java.util.UUID;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.wicket.markup.html.form.NonI18nPalette;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.Test;

public class TopologyITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:topologyLI:topology");
        TESTER.assertRenderedPage(Topology.class);
    }

    @Test
    public void showTopology() {
        TESTER.assertComponent("body:syncope", WebMarkupContainer.class);
        TESTER.assertComponent("body:resources:1", WebMarkupContainer.class);
        TESTER.assertComponent("body:resources:2:resources:0", WebMarkupContainer.class);
    }

    @Test
    public void showTopologyToggleMenu() {
        TESTER.executeAjaxEvent("body:resources:2:resources:0:res", Constants.ON_CLICK);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:propagation",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:pull",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:push",
                AjaxLink.class);
        TESTER.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:tasks",
                AjaxLink.class);
        TESTER.executeAjaxEvent("body:conns:0:conns:3:conn", Constants.ON_CLICK);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:create",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
    }

    @Test
    public void resourceBulkAction() {
        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:status");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:1:outer:form");
        formTester.setValue("content:anyTypes:dropDownChoiceField", "0");
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:anyTypes:dropDownChoiceField",
                Constants.ON_CHANGE);
        formTester.setValue("content:anyTypes:dropDownChoiceField", "0");

        component = findComponentByProp("anyKey", "body:toggle:outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");

        assertNotNull(component);

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:1:outer:dialog:footer:buttons:0:button", Constants.ON_CLICK);
    }

    @Test
    public void editProvisioning() {
        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        // ------------------------------------------
        // Check for realm provisioning feature availability (SYNCOPE-874)
        // ------------------------------------------
        FormTester formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:3:outer:form");
        formTester.setValue("content:aboutRealmProvison:enableRealmsProvision:checkboxField", true);

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:aboutRealmProvison:"
                + "enableRealmsProvision:checkboxField", Constants.ON_CHANGE);

        assertNotNull(findComponentById(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:aboutRealmProvison:realmsProvisionContainer",
                "connObjectLink"));

        TESTER.assertLabel("body:toggle:outerObjectsRepeater:3:outer:form:content:aboutRealmProvison:"
                + "realmsProvisionContainer:connObjectLink:field-label", "Object Link");

        formTester.setValue("content:aboutRealmProvison:enableRealmsProvision:checkboxField", false);

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:aboutRealmProvison:"
                + "enableRealmsProvision:checkboxField", Constants.ON_CHANGE);

        try {
            findComponentById(
                    "body:toggle:outerObjectsRepeater:3:outer:form:content:aboutRealmProvison:realmsProvisionContainer",
                    "connObjectLink");
            fail();
        } catch (NullPointerException e) {
            // correct
        }
        // ------------------------------------------

        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:actions:panelMapping:mappingLink");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:1", WebMarkupContainer.class);

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:view:mapping:mappingContainer:mappings:1:mappingItemTransformers:icon",
                Constants.ON_CLICK);

        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:"
                + "wizard:form:view:mapping:mappingContainer:mappings:0:mappingItemTransformers:alertsLink");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:outerObjectsRepeater:0:outer:container:content:togglePanelContainer:"
                + "form:classes:paletteField", NonI18nPalette.class);
    }

    @Test
    public void createNewResurceAndProvisionRules() {
        final String res = UUID.randomUUID().toString();

        TESTER.executeAjaxEvent(
                "body:conns:0:conns:0:conn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent(
                "body:toggle:container:content:togglePanelContainer:container:actions:create", Constants.ON_CLICK);

        FormTester formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");

        formTester.setValue("view:container:key:textField", res);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:next");

        // click on finish to create the external resource 
        TESTER.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:0:outer:form:content:form:buttons:finish", Constants.ON_CLICK);
        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.cleanupFeedbackMessages();
        TESTER.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", res);
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:add");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");

        formTester.setValue("view:container:type:dropDownChoiceField", "0");
        formTester.setValue("view:container:class", "__ACCOUNT__");
        formTester.submit("buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:view:mapping:mappingContainer:addMappingBtn", Constants.ON_CLICK);

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");

        formTester.setValue("view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", "true");
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form"
                + ":view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", Constants.ON_CHANGE);

        formTester.setValue("view:mapping:mappingContainer:mappings:0:intAttrName:textField", "key");
        formTester.setValue("view:mapping:mappingContainer:mappings:0:extAttrName:textField", "ID");
        formTester.setValue("view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", "true");

        formTester.submit("buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        TESTER.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:buttons:finish", Constants.ON_CLICK);
        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", AjaxSubmitLink.class);

        TESTER.cleanupFeedbackMessages();
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);
        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:actions:panelMapping:mappingLink");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:0", WebMarkupContainer.class);

        TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:buttons:cancel", Constants.ON_CLICK);
        
        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:dialog:footer:buttons:0:button");

        TESTER.cleanupFeedbackMessages();
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:delete");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:topologyLI:topology");
        component = findComponentByProp("key", "body:resources", res);
        assertNull(component);
    }

    @Test
    public void executePullTask() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        component = findComponentByProp("name", "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        TESTER.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelExecute:executeLink");

        TESTER.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:startAt:container:content:togglePanelContainer:startAtForm:startAt");
        TESTER.assertInfoMessages("Operation executed successfully");

        component = findComponentByProp("name", "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        TESTER.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelView:viewLink");

        TESTER.assertLabel(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:title",
                "Executions of task &#039;TestDB Task&#039;");

        int iteration = 0;
        do {
            try {
                TESTER.assertComponent(
                        "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:"
                        + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                        + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelView:viewLink",
                        AjaxLink.class);
                iteration = 10;
            } catch (AssertionError e) {
                try {
                    // requires a short delay
                    Thread.sleep(1000);
                } catch (Exception ignore) {
                }

                component = findComponentById(
                        "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:second:"
                        + "executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                        + "tablePanel:groupForm:checkgroup:dataTable:topToolbars:toolbars:1:headers", "panelReload");

                TESTER.executeAjaxEvent(component.getPageRelativePath() + ":reloadLink", Constants.ON_CLICK);
                iteration++;
            }
        } while (iteration < 10);

        TESTER.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelView:viewLink");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void readPropagationTaskExecutions() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                WebMarkupContainer.class);

        component = findComponentByProp("operation", "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", ResourceOperation.CREATE);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelExecute:executeLink");

        TESTER.clickLink("body:topologyLI:topology");

        component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        component = findComponentByProp("operation", "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", ResourceOperation.CREATE);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelView:viewLink");

        TESTER.assertLabel(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:title",
                "CREATE task about USER");

        TESTER.clickLink("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelView:viewLink");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void editPushTask() {
        Component component = findComponentByProp("key", "body:resources", "resource-ldap");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:push");
        TESTER.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:9:cell:panelEdit:editLink");

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:description:textField", "test");
        formTester.submit("buttons:next");

        TESTER.assertModelValue("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form:view:filters:0:filters:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panel:container:"
                + "value:textField", "_NO_ONE_");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void createSchedTask() {
        TESTER.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:tasks");
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:add");

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:wizard:form");
        formTester.setValue("view:name:textField", "test");
        formTester.select("view:jobDelegateClassName:dropDownChoiceField", 0);

        formTester.submit("buttons:next");
        TESTER.cleanupFeedbackMessages();

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:wizard:form");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:wizard:form:view:schedule:seconds:textField", TextField.class);

        formTester.submit("buttons:finish");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void addGroupTemplate() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        TESTER.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelTemplate:templateLink");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:container:"
                + "content:toggleTemplates:container:content:togglePanelContainer:templatesForm");

        formTester.setValue("type:dropDownChoiceField", "1");
        formTester.submit("changeit");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer", Modal.class);

        formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:name:textField", "'k' + name");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelTemplate:templateLink");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:container:"
                + "content:toggleTemplates:container:content:togglePanelContainer:templatesForm");

        formTester.setValue("type:dropDownChoiceField", "1");
        formTester.submit("changeit");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer", Modal.class);

        TESTER.assertModelValue("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form:view:name:textField",
                "'k' + name");

        formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:name:textField", "");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void reloadConnectors() {
        TESTER.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        TESTER.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:reload",
                AjaxLink.class);

        TESTER.cleanupFeedbackMessages();
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:reload");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }
}
