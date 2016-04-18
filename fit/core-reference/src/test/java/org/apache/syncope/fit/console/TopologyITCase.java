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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
    public void editProvisioning() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "ws-target-resource-1");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:edit");

        FormTester formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:next");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:0:outer:form:content:form:view:provision:container:"
                + "content:group:beans:0:actions:panelMapping:mappingLink");

        formTester = wicketTester.newFormTester("body:toggle:outerObjectsRepeater:0:outer:form:content:form:view:"
                + "provision:container:content:wizard:form");
        formTester.submit("buttons:next");

        wicketTester.assertComponent("body:toggle:outerObjectsRepeater:0:outer:form:content:form:view:provision:"
                + "container:content:wizard:form:view:mapping:mappingContainer:mappings:1", WebMarkupContainer.class);
    }

    @Test
    public void executePullTask() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-testdb");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:pull");
        wicketTester.clickLink("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelExecute:executeLink");

        wicketTester.clickLink("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:first:"
                + "container:content:startAt:container:content:togglePanelContainer:startAtForm:startAt");
        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void editPushTask() {
        wicketTester.clickLink("body:topologyLI:topology");

        Component component = findComponentByProp("key", "body:resources", "resource-ldap");
        assertNotNull(component);
        wicketTester.executeAjaxEvent(component.getPageRelativePath() + ":res", Constants.ON_CLICK);
        wicketTester.clickLink("body:toggle:container:content:togglePanelContainer:container:actions:push");
        wicketTester.clickLink("body:toggle:outerObjectsRepeater:1:outer:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:9:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:toggle:outerObjectsRepeater:1:outer:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");
        formTester.setValue("view:description:textField", "test");
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
}
