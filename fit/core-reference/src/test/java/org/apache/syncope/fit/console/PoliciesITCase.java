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
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PoliciesITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        TESTER.assertRenderedPage(Policies.class);
    }

    private void createAccountPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);
        formTester.setValue("content:fields:3:field:paletteField:recorder", "ws-target-resource-nopropagation4");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        TESTER.assertComponent("body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:3:field:paletteField:recorder", "ws-target-resource-nopropagation4");

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);
        Assert.assertNotNull(component);
    }

    private void createPasswordPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);
        formTester.setValue("content:fields:1:field:spinner", "1");
        formTester.setValue("content:fields:2:field:checkboxField", true);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void createPullPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deleteAccountPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePasswordPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    private void deletePullPolicy(final String description) {
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:3:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        Assert.assertNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
    }

    @Test
    public void read() {
        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
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

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deleteAccountPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
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

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePasswordPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
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

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));

        deletePullPolicy(description);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2"));

        deletePullPolicy(description + "2");
    }

    @Test
    public void createUpdateDeleteAccountPolicy() {
        final String description = "Account Policy To Be Updated";
        createAccountPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteAccountPolicy(description);
    }

    private void composeDefaultAccountPolicy(final String policyDescription, final String ruleName) {

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", policyDescription);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", ruleName);
        formTester.setValue("view:configuration:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:1:value:spinner", 0);

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:bean:propView:1:value:spinner", "6");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                ruleName);

        Assert.assertNotNull(component);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:exit");

        closeCallBack(modal);
    }

    @Test
    public void createComposeDeleteAccountPolicy() {
        final String description = "Account Policy To Be Composed";
        createAccountPolicy(description);
        composeDefaultAccountPolicy(description, "myrule");
        deleteAccountPolicy(description);
    }

    @Test
    public void createUpdateDeletePasswordPolicy() {
        final String description = "Password Policy To Be Updated";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:1:field:spinner", "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePasswordPolicy(description);
    }

    @Test
    public void createComposeDeletePasswordPolicy() {
        final String description = "Password Policy To Be Composed";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", "myrule");
        formTester.setValue("view:configuration:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:0:value:spinner", 0);

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:4:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "myrule");

        Assert.assertNotNull(component);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:container:content:exit");

        closeCallBack(modal);

        deletePasswordPolicy(description);
    }

    @Test
    public void createUpdateDeletePullPolicy() {
        final String description = "Pull Policy To Be Updated";
        createPullPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer");

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:fields:0:field:textField", description + "2");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description + "2");

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description + "2");

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deletePullPolicy(description + "2");
    }

    @Test
    public void createComposeDeletePullPolicy() {
        final String description = "Pull Policy To Be Composed";
        createPullPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form");

        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        formTester.setValue("content:conflictResolutionAction:dropDownChoiceField", "1");
        formTester.setValue("content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:"
                + "content:conflictResolutionAction:dropDownChoiceField", ConflictResolutionAction.FIRSTMATCH);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:form:"
                + "content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:5:outer:dialog:footer:buttons:0:button");

        closeCallBack(modal);

        deletePullPolicy(description);
    }

    @Test
    public void issueSYNCOPE1030() {
        final String description = "SYNCOPE-1030";
        // Create account policy
        createAccountPolicy(description);
        composeDefaultAccountPolicy(description, "issue");

        // goto realms
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.assertRenderedPage(Realms.class);

        // edit root realm
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:actions:actions:actionRepeater:1:action:action");
        TESTER.assertComponent("body:content:body:outerObjectsRepeater:0:outer", Modal.class);

        // set new account policy
        TESTER.assertLabel("body:content:body:outerObjectsRepeater:0:outer:form:content:form:view:details:container:"
                + "accountPolicy:field-label", "Account Policy");

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:outerObjectsRepeater:0:outer:form:content:form");
        formTester.select("view:details:container:accountPolicy:dropDownChoiceField", 0);
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
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
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:"
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

        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:6:panel:textField", "rossini 1030");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:14:panel:textField", "ross1030@apace.org");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
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
                + "1:outer:container:content:togglePanelContainer:container:actions:actions:actionRepeater:8:"
                + "action:action"), Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("username",
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini_clone");
        assertNull(component);

        // delete default policy
        TESTER.clickLink("body:configurationLI:configurationUL:policiesLI:policies");
        TESTER.assertRenderedPage(Policies.class);
        deleteAccountPolicy(description);
    }
}
