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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Realms;
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
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        UTILITY_UI.getTester().assertRenderedPage(Policies.class);
    }

    private void createAccountPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");
        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);
        formTester.setValue("content:fields:3:field:paletteField:recorder", "ws-target-resource-nopropagation4");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);
        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:3:field:paletteField:recorder", "ws-target-resource-nopropagation4");

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        component = UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);
        assertNotNull(component);
    }

    private void createPasswordPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");
        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void createPullPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");
        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deleteAccountPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePasswordPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePullPolicy(final String description) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    @Test
    public void read() {
        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "an account policy"));
    }

    @Test
    public void createDeleteAccountPolicy() {
        final String description = "My Test Account Policy";
        createAccountPolicy(description);
        deleteAccountPolicy(description);
    }

    @Test
    public void cloneDeleteAccountPolicy() {
        final String description = "My Test Account Policy to be cloned";
        createAccountPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deleteAccountPolicy(description);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deleteAccountPolicy(description + "2");
    }

    @Test
    public void createDeletePasswordPolicy() {
        final String description = "My Test Password Policy";
        createPasswordPolicy(description);
        deletePasswordPolicy(description);
    }

    @Test
    public void cloneDeletePasswordPolicy() {
        final String description = "My Test Password Policy to be cloned";
        createPasswordPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePasswordPolicy(description);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deletePasswordPolicy(description + "2");
    }

    @Test
    public void createDeletePullPolicy() {
        final String description = "My Test Pull Policy";
        createPullPolicy(description);
        deletePullPolicy(description);
    }

    @Test
    public void cloneDeletePullPolicy() {
        final String description = "My Test Pull Policy to be cloned";
        createPullPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePullPolicy(description);

        assertNotNull(UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deletePullPolicy(description + "2");
    }

    @Test
    public void createUpdateDeleteAccountPolicy() {
        final String description = "Account Policy To Be Updated";
        createAccountPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        component = UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteAccountPolicy(description);
    }

    private void composeDefaultAccountPolicy(final String policyDescription) {
        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", policyDescription);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:rule:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:utility");

        UTILITY_UI.closeCallBack(modal);
    }

    @Test
    public void createComposeDeleteAccountPolicy() {
        final String description = "Account Policy To Be Composed";
        createAccountPolicy(description);
        composeDefaultAccountPolicy(description);
        deleteAccountPolicy(description);
    }

    @Test
    public void createUpdateDeletePasswordPolicy() {
        final String description = "Password Policy To Be Updated";
        createPasswordPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        component = UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePasswordPolicy(description);
    }

    @Test
    public void createComposeDeletePasswordPolicy() {
        final String description = "Password Policy To Be Composed";
        createPasswordPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:rule:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:utility");

        UTILITY_UI.closeCallBack(modal);

        deletePasswordPolicy(description);
    }

    @Test
    public void createUpdateDeletePullPolicy() {
        final String description = "Pull Policy To Be Updated";
        createPullPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        component = UTILITY_UI.findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2");

        assertNotNull(component);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        UTILITY_UI.getTester().assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description + "2");

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePullPolicy(description + "2");
    }

    @Test
    public void createComposeDeletePullPolicy() {
        final String description = "Pull Policy To Be Composed";
        createPullPolicy(description);

        Component component = UTILITY_UI.findComponentByProp("description",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer",
                Modal.class);

        Component modal = UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form");

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        component = UTILITY_UI.findComponentById(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:view:0:panel:rule",
                "dropDownChoiceField");
        assertNotNull(component);

        formTester.setValue(component, "0");
        UTILITY_UI.getTester().executeAjaxEvent(component, Constants.ON_CHANGE);
        formTester.setValue(component, "0");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:footer:inputs:0:submit");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.closeCallBack(modal);

        deletePullPolicy(description);
    }

    @Test
    public void issueSYNCOPE1030() {
        final String description = "SYNCOPE-1030";
        // Create account policy
        createAccountPolicy(description);
        composeDefaultAccountPolicy(description);

        // goto realms
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().assertRenderedPage(Realms.class);

        // edit root realm
        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        UTILITY_UI.getTester().assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        // set new account policy
        UTILITY_UI.getTester().assertLabel(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:container:"
                + "accountPolicy:field-label", "Account Policy");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.select("view:details:container:accountPolicy:dropDownChoiceField", 0);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action",
                Constants.ON_CLICK);

        // create user with a valid account name
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:"
                + "action:action");

        UTILITY_UI.getTester().assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:6:panel:textField", "rossini 1030");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:14:panel:textField", "ross1030@apace.org");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini_clone");
        assertNotNull(component);

        // delete the new user
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:8:"
                + "action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:8:"
                + "action:action"), Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        component = UTILITY_UI.findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini_clone");
        assertNull(component);

        // delete default policy
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        UTILITY_UI.getTester().assertRenderedPage(Policies.class);
        deleteAccountPolicy(description);
    }
}
