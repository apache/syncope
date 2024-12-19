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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.UUID;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.NonI18nPalette;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TopologyITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:idmPages:0:idmPageLI:idmPage", false);
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
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:propagation",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:pull",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:push",
                AjaxLink.class);
        TESTER.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:conns:0:conns:3:conn", Constants.ON_CLICK);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:create",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
    }

    @Test
    public void resourceBatchAction() {
        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:container:content:togglePanelContainer:container:actions:reconciliation");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:1:outer:form");
        formTester.setValue("content:anyTypes:dropDownChoiceField", "0");
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:anyTypes:dropDownChoiceField",
                Constants.ON_CHANGE);
        formTester.setValue("content:anyTypes:dropDownChoiceField", "0");

        component = findComponentByProp("key",
                "body:toggle:outerObjectsRepeater:1:outer:form:content:status:"
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
        TESTER.clickLink(
                "body:toggle:container:content:togglePanelContainer:container:actions:provision");

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:fields:0", Constants.ON_CLICK);

        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:toggle:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:1", WebMarkupContainer.class);

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:view:mapping:mappingContainer:mappings:1:itemTransformers:icon",
                Constants.ON_CLICK);

        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:"
                + "wizard:form:view:mapping:mappingContainer:mappings:0:itemTransformers:alertsLink");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:outerObjectsRepeater:0:outer:container:content:togglePanelContainer:"
                + "form:classes:paletteField", NonI18nPalette.class);
    }

    @Test
    public void createNewResurceAndProvisionRules() {
        // ConnInstance100
        Component connector = findComponentByProp(
                "key", "body:conns:0:conns", "88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull(connector);

        int bodyIdx = connector.getPath().indexOf("body");
        assertTrue(bodyIdx != -1);

        TESTER.executeAjaxEvent(
                connector.getPath().substring(bodyIdx) + ":conn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent(
                "body:toggle:container:content:togglePanelContainer:container:actions:create", Constants.ON_CLICK);

        FormTester formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:0:outer:form:content:form");

        String res = UUID.randomUUID().toString();
        formTester.setValue("view:container:key:textField", res);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:next");

        // click on finish to create the external resource 
        TESTER.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:0:outer:form:content:form:buttons:finish", Constants.ON_CLICK);
        assertSuccessMessage();

        TESTER.cleanupFeedbackMessages();
        TESTER.clickLink("body:idmPages:0:idmPageLI:idmPage", false);

        Component component = findComponentByProp("key", "body:resources", res);
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:container:content:togglePanelContainer:container:actions:provision");

        // -- create new provision rules for the current resource
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:add");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:objectTypeToggle:container:"
                + "content:togglePanelContainer:objectTypeForm");
        formTester.select("type:dropDownChoiceField", 1);
        formTester.submit("changeit");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        // choose object class
        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        // aux classes
        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");
        TESTER.assertNoErrorMessage();
        TESTER.assertNoInfoMessage();

        // set a new mapping rule
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
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

        // finish
        TESTER.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:buttons:finish", Constants.ON_CLICK);
        assertSuccessMessage();

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", AjaxSubmitLink.class);

        // save
        TESTER.cleanupFeedbackMessages();
        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);
        TESTER.assertNoErrorMessage();
        assertSuccessMessage();

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:fields:0", Constants.ON_CLICK);

        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:toggle:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:0", WebMarkupContainer.class);

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:buttons:cancel", Constants.ON_CLICK);

        TESTER.clickLink("body:toggle:outerObjectsRepeater:3:outer:dialog:footer:buttons:0:button");

        TESTER.cleanupFeedbackMessages();
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:delete");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:idmPages:0:idmPageLI:idmPage", false);
        component = findComponentByProp("key", "body:resources", res);
        assertNull(component);
    }

    @Test
    public void executePullTask() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        component = findComponentByProp("name",
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:4:action:action");

        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:startAt:container:content:togglePanelContainer:startAtForm:startAt");
        assertSuccessMessage();

        component = findComponentByProp("name",
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:title",
                "Executions of task &#039;TestDB Task&#039;");

        int iteration = 0;
        do {
            try {
                component = findComponentByProp("status",
                        "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:"
                        + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                        + "tablePanel:groupForm:checkgroup:dataTable", "SUCCESS");
                assertNotNull(component);
                iteration = 10;
            } catch (AssertionError e) {
                try {
                    // requires a short delay
                    Thread.sleep(1000);
                } catch (Exception ignore) {
                }

                TESTER.executeAjaxEvent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                        + "secondLevelContainer:second:executions:firstLevelContainer:first:container:content:"
                        + "searchContainer:tablehandling:actionRepeater:0:action:action", Constants.ON_CLICK);

                iteration++;
            }
        } while (iteration < 10);

        component = findComponentByProp("status",
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable", "SUCCESS");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:second:"
                + "executions:firstLevelContainer:first:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void readPropagationTaskExecutions() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                WebMarkupContainer.class);

        component = findComponentByProp("operation",
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", ResourceOperation.CREATE);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:2:action:action");

        TESTER.clickLink("body:idmPages:0:idmPageLI:idmPage", false);

        component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        component = findComponentByPropNotNull("start",
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable");

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:title",
                "CREATE __ACCOUNT__");

        component = findComponentByProp("status",
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:firstLevelContainer:first:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", "FAILURE");

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void editPushTask() {
        Component component = findComponentByProp("key", "body:resources", "resource-ldap");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:push");

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:1:action:action");

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

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void addGroupTemplate() {
        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
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

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:2", Constants.ON_CLICK);
        TESTER.clickLink(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:4:action:action");

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        TESTER.assertComponent(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
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

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void reloadConnectors() {
        TESTER.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        TESTER.assertComponent(
                "body:toggle:container:content:togglePanelContainer:container:actions:reload",
                AjaxLink.class);

        TESTER.cleanupFeedbackMessages();
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:reload");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }
}
