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
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PoliciesITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies", false);
        TESTER.assertRenderedPage(Policies.class);
    }

    private static void createAccountPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name);
        formTester.setValue("content:fields:1:field:numberTextField", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        TESTER.assertComponent("body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", name);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:numberTextField", 1);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);
        assertNotNull(component);
    }

    private static void createPasswordPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:5:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name);
        formTester.setValue("content:fields:1:field:numberTextField", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", name);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:numberTextField", 1);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));
    }

    private static void createInboundPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:4:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", name);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));
    }

    private static void deleteAccountPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));
    }

    private static void deletePasswordPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:5:link");
        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));
    }

    private static void deleteInboundPolicy(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:4:link");
        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:4:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));
    }

    @Test
    public void read() {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "an account policy"));
    }

    @Test
    public void createDeleteAccountPolicy() {
        String name = "My Test Account Policy";
        createAccountPolicy(name);
        deleteAccountPolicy(name);
    }

    @Test
    public void cloneDeleteAccountPolicy() {
        String name = "My Test Account Policy to be cloned";
        createAccountPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name + '2');

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));

        deleteAccountPolicy(name);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name + '2'));

        deleteAccountPolicy(name + '2');
    }

    @Test
    public void createDeletePasswordPolicy() {
        String name = "My Test Password Policy";
        createPasswordPolicy(name);
        deletePasswordPolicy(name);
    }

    @Test
    public void cloneDeletePasswordPolicy() {
        String name = "My Test Password Policy to be cloned";
        createPasswordPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name + '2');

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));

        deletePasswordPolicy(name);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name + '2'));

        deletePasswordPolicy(name + '2');
    }

    @Test
    public void createDeleteInboundPolicy() {
        String name = "My Test Pull Policy";
        createInboundPolicy(name);
        deleteInboundPolicy(name);
    }

    @Test
    public void cloneDeleteInboundPolicy() {
        String name = "My Test Pull Policy to be cloned";
        createInboundPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name + '2');

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name));

        deleteInboundPolicy(name);

        assertNotNull(findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name + '2'));

        deleteInboundPolicy(name + '2');
    }

    @Test
    public void createUpdateDeleteAccountPolicy() {
        String name = "Account Policy To Be Updated";
        createAccountPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:numberTextField", "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:numberTextField", 2);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteAccountPolicy(name);
    }

    private static void composeDefaultAccountPolicy(final String policyDescription) {
        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", policyDescription);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:rule:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:utility");

        closeCallBack(modal);
    }

    @Test
    public void createComposeDeleteAccountPolicy() {
        String name = "Account Policy To Be Composed";
        createAccountPolicy(name);
        composeDefaultAccountPolicy(name);
        deleteAccountPolicy(name);
    }

    @Test
    public void createUpdateDeletePasswordPolicy() {
        String name = "Password Policy To Be Updated";
        createPasswordPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:numberTextField", "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:numberTextField", 2);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePasswordPolicy(name);
    }

    @Test
    public void createComposeDeletePasswordPolicy() {
        String name = "Password Policy To Be Composed";
        createPasswordPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer",
                Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:rule:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:utility");

        closeCallBack(modal);

        deletePasswordPolicy(name);
    }

    @Test
    public void createUpdateDeleteInboundPolicy() {
        String name = "Pull Policy To Be Updated";
        createInboundPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", name + '2');

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name + '2');

        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", name + '2');

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteInboundPolicy(name + '2');
    }

    @Test
    public void createComposeDeleteInboundPolicy() {
        String name = "Pull Policy To Be Composed";
        createInboundPolicy(name);

        Component component = findComponentByProp("name",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", name);
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form");

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        component = findComponentById(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:view:0:panel:rule",
                "dropDownChoiceField");
        assertNotNull(component);

        formTester.setValue(component, "0");
        TESTER.executeAjaxEvent(component, Constants.ON_CHANGE);
        formTester.setValue(component, "0");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        deleteInboundPolicy(name);
    }

    @Test
    public void issueSYNCOPE1030() {
        String name = "SYNCOPE-1030";
        // Create account policy
        createAccountPolicy(name);
        composeDefaultAccountPolicy(name);

        // goto realms
        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.assertRenderedPage(Realms.class);

        // edit root realm
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        // set new account policy
        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:container:"
                + "policies:1:field-label", "Account Policy");

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.select("view:details:container:policies:1:dropDownChoiceField", 0);
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action",
                Constants.ON_CLICK);

        // create user with a valid account name
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:10:"
                + "action:action");

        TESTER.assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        formTester = TESTER.newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:7:panel:textField", "rossini 1030");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:15:panel:textField", "ross1030@apache.org");
        formTester.submit("buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini_clone");
        assertNotNull(component);

        // delete the new user
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:8:"
                + "action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:11:"
                + "action:action"), Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini_clone");
        assertNull(component);

        // delete default policy
        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies", false);
        TESTER.assertRenderedPage(Policies.class);
        deleteAccountPolicy(name);
    }
}
