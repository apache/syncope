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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RealmsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        UTILITY_UI.getTester().assertLabel(
                "body:content:body:container:content:tabbedPanel:panel:container:accountPolicy:field-label",
                "Account Policy");
    }

    @Test
    public void create() {
        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:0:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:generics:name:textField", "testRealm");

        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);

        // remove the new realm just created
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");

        UTILITY_UI.getTester().
                executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:5:button",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertLabel("body:content:realmChoicePanel:container:realm", "/testRealm");

        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:3:action:action");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertLabel(
                "body:content:body:container:content:tabbedPanel:panel:container:accountPolicy:field-label",
                "Account Policy");

        UTILITY_UI.getTester().assertLabel("body:content:realmChoicePanel:container:realm", "/");
    }

    @Test
    public void update() {
        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);
    }

    @Test
    public void addUserTemplate() {
        UTILITY_UI.getTester().
                executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:4:button",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertLabel("body:content:realmChoicePanel:container:realm", "/odd");

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:2:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        UTILITY_UI.getTester().assertComponent("body:content:templateModal", Modal.class);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "'k' + firstname");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertLabel("body:content:realmChoicePanel:container:realm", "/odd");

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:2:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:toggleTemplates", TogglePanel.class);

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        UTILITY_UI.getTester().assertComponent("body:content:templateModal", Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:templateModal:form:content:form:view:username:textField",
                "'k' + firstname");

        formTester = UTILITY_UI.getTester().newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void verifyPropagation() {
        UTILITY_UI.getTester().
                executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "even");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "resource-ldap-orgunit");

        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        Component component = UTILITY_UI.findComponentByProp("resource",
                "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        UTILITY_UI.getTester().clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:field-label", "__NAME__");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:leftAttribute:textField", null);

        UTILITY_UI.getTester().assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:field-label", "__NAME__");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:rightAttribute:textField",
                "ou=even,o=isp");

        UTILITY_UI.getTester().clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(UTILITY_UI.findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "even");

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "");

        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation results
        // ----------------------------------
        component = UTILITY_UI.findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        UTILITY_UI.getTester().clickLink(component.getPageRelativePath() + ":actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:leftAttribute:field-label", "ou");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:leftAttribute:textField", "even");

        UTILITY_UI.getTester().assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:rightAttribute:field-label", "ou");

        UTILITY_UI.getTester().assertModelValue(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:4:value:rightAttribute:textField", null);

        UTILITY_UI.getTester().clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(UTILITY_UI.findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:"
                + "0:action:action", Constants.ON_CLICK);
    }
}
