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
import static org.junit.Assert.assertNotNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RealmsITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        TESTER.assertLabel("body:content:body:container:content:tabbedPanel:panel:container:accountPolicy:field-label",
                "Account Policy");
    }

    @Test
    public void create() {
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelCreate:createLink");

        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:generics:name:textField", "testRealm");

        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink",
                Constants.ON_CLICK);

        // remove the new realm just created
        TESTER.clickLink("body:realmsLI:realms");

        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:4:button",
                Constants.ON_CLICK);

        TESTER.assertLabel("body:content:realmChoicePanel:container:realm", "/testRealm");

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelDelete:deleteLink");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertLabel("body:content:body:container:content:tabbedPanel:panel:container:accountPolicy:field-label",
                "Account Policy");

        TESTER.assertLabel("body:content:realmChoicePanel:container:realm", "/");
    }

    @Test
    public void update() {
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelEdit:editLink");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink",
                Constants.ON_CLICK);
    }

    @Test
    public void addUserTemplate() {
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelTemplate:templateLink");
        TESTER.assertComponent("body:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        TESTER.assertComponent("body:content:templateModal", Modal.class);

        formTester = TESTER.newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "'k' + firstname");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelTemplate:templateLink");
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

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void verifyPropagation() {
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelEdit:editLink");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "two");

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "resource-ldap-orgunit");

        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation rsults
        // ----------------------------------
        Component component = findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:panelView:viewLink");

        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:oldAttribute:field-label", "ou");

        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:oldAttribute:textField", null);
        
        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:1:value:oldAttribute:textField", null);
        
        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:oldAttribute:textField", null);

        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:newAttribute:field-label", "ou");

        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:newAttribute:textField", "two");

        TESTER.clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink",
                Constants.ON_CLICK);

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:panelEdit:editLink");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);
        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:"
                + "container:generics:name:textField", "two");

        formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.setValue("view:details:container:resources:paletteField:recorder", "");

        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        // ----------------------------------
        // Check for propagation rsults
        // ----------------------------------
        component = findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit");

        TESTER.clickLink(component.getPageRelativePath() + ":actions:panelView:viewLink");

        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:oldAttribute:field-label", "ou");

        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:oldAttribute:textField", "two");

        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:newAttribute:field-label", "ou");

        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:0:value:newAttribute:textField", null);

        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:1:value:newAttribute:textField", null);
        
        TESTER.assertModelValue("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:second:remoteObject:propView:2:value:newAttribute:textField", null);
        
        TESTER.clickLink("body:content:body:outerObjectsRepeater:0:outer:form:content:customResultBody:"
                + "secondLevelContainer:back");

        assertNotNull(findComponentByProp("resource", "body:content:body:outerObjectsRepeater:0:outer:form:"
                + "content:customResultBody:firstLevelContainer:first:container", "resource-ldap-orgunit"));
        // ----------------------------------

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink",
                Constants.ON_CLICK);
    }
}
