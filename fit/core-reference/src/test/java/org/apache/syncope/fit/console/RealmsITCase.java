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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RealmsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        TESTER.assertLabel(
                "body:content:body:container:content:tabbedPanel:panel:container:policies:1:field-label",
                "Account Policy");
    }

    @Test
    public void create() {
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:0:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:generics:name:textField", "testRealm");

        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);

        // remove the new realm just created
        TESTER.clickLink("body:realmsLI:realms", false);

        TESTER.executeAjaxEvent(
                "body:content:realmChoicePanel:container:realmsFragment:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent(
                "body:content:realmChoicePanel:container:realmsFragment:realms:dropdown-menu:buttons:5:button",
                Constants.ON_CLICK);

        assertTrue(TESTER.getLastResponseAsString().contains(">/</a>"));
        assertTrue(TESTER.getLastResponseAsString().contains(">testRealm</a>"));

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:3:action:action");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel(
                "body:content:body:container:content:tabbedPanel:panel:container:policies:1:field-label",
                "Account Policy");

        assertTrue(TESTER.getLastResponseAsString().contains(">/</a>"));
        assertFalse(TESTER.getLastResponseAsString().contains(">testRealm</a>"));
    }

    @Test
    public void update() {
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);
    }

    @Test
    public void addUserTemplate() {
        TESTER.executeAjaxEvent(
                "body:content:realmChoicePanel:container:realmsFragment:realms:btn",
                Constants.ON_CLICK);
        TESTER.executeAjaxEvent(
                "body:content:realmChoicePanel:container:realmsFragment:realms:dropdown-menu:buttons:4:button",
                Constants.ON_CLICK);

        assertTrue(TESTER.getLastResponseAsString().contains(">/</a>"));
        assertTrue(TESTER.getLastResponseAsString().contains(">odd</a>"));

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:2:action:action");
        TESTER.assertComponent("body:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        TESTER.assertComponent("body:content:templateModal", Modal.class);

        formTester = TESTER.newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "'k' + firstname");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertTrue(TESTER.getLastResponseAsString().contains(">/</a>"));
        assertTrue(TESTER.getLastResponseAsString().contains(">odd</a>"));

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:2:action:action");
        TESTER.assertComponent("body:content:toggleTemplates", TogglePanel.class);

        formTester = TESTER.newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        TESTER.assertComponent("body:content:templateModal", Modal.class);

        TESTER.assertModelValue("body:content:templateModal:form:content:form:view:username:textField",
                "'k' + firstname");

        formTester = TESTER.newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void verifyPropagation() {
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container"
                + ":realmsFragment:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container"
                + ":realmsFragment:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "even");

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "resource-ldap-orgunit");

        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        Component component = findComponentByProp("resource",
                "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:field-label", "__NAME__");

        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:textField", null);

        TESTER.assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:field-label", "__NAME__");

        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:textField",
                "ou=even,o=isp");

        TESTER.clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "even");

        formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "");

        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        component = findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        TESTER.assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:leftAttribute:field-label", "ou");

        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:leftAttribute:textField", "even");

        TESTER.assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:rightAttribute:field-label", "ou");

        TESTER.assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:rightAttribute:textField", null);

        TESTER.clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);
    }
}
