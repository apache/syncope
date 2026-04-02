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
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RealmsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink(REALM_PAGE, false);
        TESTER.assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        assertNotNull(findComponentByProp("name", "body:directoryPanel:container:content:searchContainer"
                + ":resultTable:tablePanel:groupForm:checkgroup:dataTable", SyncopeConstants.ROOT_REALM));
    }

    @Test
    public void create() {
        String newRealmName = "testRealm";
        TESTER.clickLink("body:directoryPanel:container:content:add");
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                Label.class);
        TESTER.assertLabel("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                "New Realm");

        FormTester formTester = TESTER.newFormTester(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:generics:name:textField", newRealmName);
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent("body:directoryPanel:outerObjectsRepeater:0:outer:form:content:"
                + "action:actionRepeater:0:action:action", Constants.ON_CLICK);

        // remove the new realm just created
        Component component = findComponentByProp("name",
                "body:directoryPanel:container:content:searchContainer:resultTable:tablePanel:groupForm"
                        + ":checkgroup:dataTable", newRealmName);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:7:action:action");

        TESTER.getRequest().addParameter("confirm", "true");

        component = findComponentByProp("name",
                "body:directoryPanel:container:content:searchContainer:resultTable:tablePanel:groupForm"
                        + ":checkgroup:dataTable", newRealmName);
        assertNull(component);
    }

    @Test
    public void update() {
        Component component = findComponentByProp("name",
                "body:directoryPanel:container:content:searchContainer:resultTable:tablePanel:groupForm"
                        + ":checkgroup:dataTable", "odd");
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                Label.class);
        TESTER.assertLabel("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                "Edit Realm /odd");

        FormTester formTester = TESTER.newFormTester(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent("body:directoryPanel:outerObjectsRepeater:0:outer:form:content:"
                + "action:actionRepeater:0:action:action", Constants.ON_CLICK);
    }

    @Test
    public void addUserTemplate() {
        Component component = findComponentByProp("name",
                "body:directoryPanel:container:content:searchContainer:resultTable:tablePanel:groupForm"
                        + ":checkgroup:dataTable", "odd");
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:5:action:action");

        TESTER.assertComponent("body:toggleTemplates", TogglePanel.class);

        FormTester formTester = TESTER.newFormTester(
                "body:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        TESTER.assertComponent("body:templateModal", Modal.class);

        formTester = TESTER.newFormTester("body:templateModal:form:content:form");
        formTester.setValue("view:usernameInnerForm:username:textField", "'k' + firstname");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:5:action:action");
        TESTER.assertComponent("body:toggleTemplates", TogglePanel.class);

        formTester = TESTER.newFormTester(
                "body:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        TESTER.assertComponent("body:templateModal", Modal.class);

        TESTER.assertModelValue("body:templateModal:form:content:form:view:usernameInnerForm:username:textField",
                "'k' + firstname");

        formTester = TESTER.newFormTester("body:templateModal:form:content:form");
        formTester.setValue("view:usernameInnerForm:username:textField", "");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void verifyPropagation() {
        // ----------------------------------
        // Associate the resource with a realm
        // ----------------------------------
        Component component = findComponentByProp("name",
                "body:directoryPanel:container:content:searchContainer:resultTable:tablePanel:groupForm"
                        + ":checkgroup:dataTable", "even");
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                Label.class);
        TESTER.assertLabel("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                "Edit Realm /even");

        FormTester formTester = TESTER.newFormTester(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "resource-ldap-orgunit");

        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        component = findComponentByProp("resource",
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody"
                        + ":firstLevelContainer:first:container:content:group", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:field-label", "__NAME__");

        TESTER.assertModelValue(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:textField", null);

        TESTER.assertLabel(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:field-label", "__NAME__");

        TESTER.assertModelValue(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:textField",
                "ou=even,o=isp");

        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:directoryPanel:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));

        TESTER.executeAjaxEvent(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);
        // ----------------------------------
        // Dissociate the resource from the realm
        // ----------------------------------
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                Label.class);
        TESTER.assertLabel("body:directoryPanel:outerObjectsRepeater:0:outer:dialog:header:header-label",
                "Edit Realm /even");

        formTester = TESTER.newFormTester(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "");

        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        component = findComponentByProp("resource", "body:directoryPanel:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:5:value:leftAttribute:field-label", "ou");

        TESTER.assertModelValue(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:5:value:leftAttribute:textField", "even");

        TESTER.assertLabel(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:5:value:rightAttribute:field-label", "ou");

        TESTER.assertModelValue(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:5:value:rightAttribute:textField", null);

        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:directoryPanel:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));

        TESTER.executeAjaxEvent(
                "body:directoryPanel:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);

        // ----------------------------------
        // Check that 2 propagations have been recorded
        // ----------------------------------

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action");

        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:4:outer", Modal.class);
        TESTER.assertComponent("body:directoryPanel:outerObjectsRepeater:4:outer:dialog:header:header-label",
                Label.class);
        TESTER.assertLabel("body:directoryPanel:outerObjectsRepeater:4:outer:dialog:header:header-label",
                "Propagation tasks for realm /even");

        component = findComponentByProp("operation", "body:directoryPanel:outerObjectsRepeater:4:outer:"
                + "form:content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable", ResourceOperation.CREATE);

        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:4:outer:form:content:tasks:firstLevelContainer:"
                + "first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:3:action:action");
        TESTER.getRequest().addParameter("confirm", "true");

        component = findComponentByProp("operation", "body:directoryPanel:outerObjectsRepeater:4:outer:"
                + "form:content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable", ResourceOperation.DELETE);

        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:directoryPanel:outerObjectsRepeater:4:outer:form:content:tasks:firstLevelContainer:"
                + "first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:actions:"
                + "actions:actionRepeater:3:action:action");
        TESTER.getRequest().addParameter("confirm", "true");

        TESTER.executeAjaxEvent("body:directoryPanel:outerObjectsRepeater:4:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);
    }
}
