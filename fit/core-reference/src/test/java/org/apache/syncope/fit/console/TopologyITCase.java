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
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import java.util.UUID;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.TogglePanel;
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
    }

    @Test
    public void showTopology() {
        wicketTester.clickLink("body:topologyLI:topology");
        wicketTester.assertComponent("body:syncope", WebMarkupContainer.class);
        wicketTester.assertComponent("body:resources:1", WebMarkupContainer.class);
        wicketTester.assertComponent("body:resources:2:resources:0", WebMarkupContainer.class);
    }

    @Test
    public void showTopologyToggleMenu() {
        wicketTester.clickLink("body:topologyLI:topology");
        wicketTester.executeAjaxEvent("body:resources:2:resources:0:res", Constants.ON_CLICK);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:propagation",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:pull",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:push",
                AjaxLink.class);
        wicketTester.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:tasks",
                AjaxLink.class);
        wicketTester.executeAjaxEvent("body:conns:0:conns:3:conn", Constants.ON_CLICK);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:create",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:delete",
                AjaxLink.class);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:edit",
                AjaxLink.class);
    }

    @Test
    public void resourceBulkAction() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:status");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:1:outer", Modal.class);

        FormTester formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:1:outer:form");
        formTester.setValue("content:type:dropDownChoiceField", "2");
        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:type:dropDownChoiceField",
                Constants.ON_CHANGE);
        formTester.setValue("content:type:dropDownChoiceField", "2");

        component = findComponentByProp("anyKey", "body:toggle:outerObjectsRepeater:1:outer:form:content:status:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee");

        assertNotNull(component);

        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:1:outer:dialog:footer:buttons:0:button", Constants.ON_CLICK);
    }

    @Test
    public void editProvisioning() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:actions:panelMapping:mappingLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:1", WebMarkupContainer.class);

        wicketTester.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:view:mapping:mappingContainer:mappings:1:mappingItemTransformers:icon",
                Constants.ON_CLICK);

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:"
                + "wizard:form:view:mapping:mappingContainer:mappings:0:mappingItemTransformers:alertsLink");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:outerObjectsRepeater:0:outer:container:content:togglePanelContainer:"
                + "form:classes:paletteField", NonI18nPalette.class);
    }

    @Test
    public void createNewResurceAndProvisionRules() {
        final String res = UUID.randomUUID().toString();
        wicketTester.clickLink("body:topologyLI:topology");

        wicketTester.executeAjaxEvent(
                "body:conns:0:conns:0:conn", Constants.ON_CLICK);
        wicketTester.executeAjaxEvent(
                "body:toggle:container:content:togglePanelContainer:container:actions:create", Constants.ON_CLICK);

        FormTester formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");

        formTester.setValue("view:container:key:textField", res);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:next");

        // click on finish to create the external resource 
        wicketTester.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:0:outer:form:content:form:buttons:finish", Constants.ON_CLICK);
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", res);
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:add");

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");

        formTester.setValue("view:container:type:dropDownChoiceField", "0");
        formTester.setValue("view:container:class", "__ACCOUNT__");
        formTester.submit("buttons:next");
        wicketTester.assertNoErrorMessage();
        wicketTester.assertNoInfoMessage();

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");
        wicketTester.assertNoErrorMessage();
        wicketTester.assertNoInfoMessage();

        wicketTester.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:view:mapping:mappingContainer:addMappingBtn", Constants.ON_CLICK);

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");

        formTester.setValue("view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", "true");
        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form"
                + ":view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", Constants.ON_CHANGE);

        formTester.setValue("view:mapping:mappingContainer:mappings:0:intAttrName:textField", "key");
        formTester.setValue("view:mapping:mappingContainer:mappings:0:extAttrName:textField", "ID");
        formTester.setValue("view:mapping:mappingContainer:mappings:0:connObjectKey:checkboxField", "true");

        formTester.submit("buttons:next");
        wicketTester.assertNoErrorMessage();
        wicketTester.assertNoInfoMessage();

        wicketTester.cleanupFeedbackMessages();
        // ajax event required to retrieve AjaxRequestTarget (used into finish custom event)
        wicketTester.executeAjaxEvent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:wizard:form:buttons:finish", Constants.ON_CLICK);
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.assertComponent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", AjaxSubmitLink.class);

        wicketTester.cleanupFeedbackMessages();
        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:provision");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:"
                + "content:group:beans:0:actions:panelMapping:mappingLink");

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:3:outer:form:content:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:0", WebMarkupContainer.class);

        wicketTester.executeAjaxEvent(
                "body:toggle:outerObjectsRepeater:3:outer:form:content:provision:container:content:"
                + "wizard:form:buttons:cancel", Constants.ON_CLICK);

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:3:outer:dialog:footer:buttons:0:button");

        wicketTester.cleanupFeedbackMessages();
        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:delete");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:topologyLI:topology");
        component = findComponentByProp("key", "body:resources", res);
        assertNull(component);
    }

    @Test
    public void executePullTask() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        component = findComponentByProp("name", "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelExecute:executeLink");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:startAt:container:content:togglePanelContainer:startAtForm:startAt");
        wicketTester.assertInfoMessages("Operation executed successfully");

        component = findComponentByProp("name", "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "TestDB Task");

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelView:viewLink");

        wicketTester.assertLabel(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:title",
                "Executions of task &#039;TestDB Task&#039;");

        int iteration = 0;
        do {
            try {
                wicketTester.assertComponent(
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

                wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":reloadLink", Constants.ON_CLICK);
                iteration++;
            }
        } while (iteration < 10);

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelView:viewLink");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void readPropagationTaskExecutions() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                WebMarkupContainer.class);

        component = findComponentByProp("operation", "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", ResourceOperation.CREATE);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelExecute:executeLink");

        wicketTester.clickLink("body:topologyLI:topology");

        component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:propagation");

        component = findComponentByProp("operation", "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", ResourceOperation.CREATE);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:10:cell:panelView:viewLink");

        wicketTester.assertLabel(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:title",
                "CREATE task about USER");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:secondLevelContainer:"
                + "second:executions:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelView:viewLink");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:"
                + "secondLevelContainer:second:executions:secondLevelContainer:title", Label.class);
    }

    @Test
    public void editPushTask() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-ldap");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:push");
        wicketTester.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:9:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:description:textField", "test");
        formTester.submit("buttons:next");

        wicketTester.assertModelValue("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form:view:filters:0:filters:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panel:container:"
                + "value:textField", "_NO_ONE_");

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void createSchedTask() {
        wicketTester.clickLink("body:topologyLI:topology");
        wicketTester.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:tasks");
        wicketTester.clickLink(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:add");

        FormTester formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:wizard:form");
        formTester.setValue("view:name:textField", "test");
        formTester.select("view:jobDelegateClassName:dropDownChoiceField", 0);

        formTester.submit("buttons:next");
        wicketTester.cleanupFeedbackMessages();

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:wizard:form");

        wicketTester.assertComponent(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:wizard:form:view:schedule:seconds:textField", TextField.class);

        formTester.submit("buttons:finish");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void addGroupTemplate() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelTemplate:templateLink");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:container:"
                + "content:toggleTemplates:container:content:togglePanelContainer:templatesForm");

        formTester.setValue("type:dropDownChoiceField", "1");
        formTester.submit("changeit");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer", Modal.class);

        formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:name:textField", "'k' + name");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelTemplate:templateLink");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:toggleTemplates", TogglePanel.class);

        formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:firstLevelContainer:first:container:"
                + "content:toggleTemplates:container:content:togglePanelContainer:templatesForm");

        formTester.setValue("type:dropDownChoiceField", "1");
        formTester.submit("changeit");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:2:outer", Modal.class);

        wicketTester.assertModelValue("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form:view:name:textField",
                "'k' + name");

        formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:2:outer:form:content:tasks:"
                + "firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:name:textField", "");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void reloadConnectors() {
        wicketTester.clickLink("body:topologyLI:topology");
        wicketTester.executeAjaxEvent("body:syncope", Constants.ON_CLICK);
        wicketTester.assertComponent("body:toggle:container:content:togglePanelContainer:container:actions:reload",
                AjaxLink.class);

        wicketTester.cleanupFeedbackMessages();
        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:reload");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }
}
