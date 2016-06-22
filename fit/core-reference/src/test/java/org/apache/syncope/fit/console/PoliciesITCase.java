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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
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
        formTester.setValue("content:fields:3:field:paletteField:recorder", "resource-csv");

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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:0:field:textField", description);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 1);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:2:field:checkboxField", true);
        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:3:field:paletteField:recorder", "resource-csv");

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        Assert.assertNotNull(findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description));
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
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

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:9:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:9:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

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

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                TESTER.getComponentFromLastRenderedPage(
                        component.getPageRelativePath() + ":cells:8:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:8:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

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

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                TESTER.getComponentFromLastRenderedPage(
                        component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"), Constants.ON_CLICK);

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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelClone:cloneLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelClone:cloneLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelClone:cloneLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
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
        TESTER.assertLabel(component.getPageRelativePath() + ":cells:7:cell", "2");

        TESTER.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelEdit:editLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form"
                + ":content:fields:1:field:spinner", 2);

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:buttons:0:button",
                Constants.ON_CLICK);

        deleteAccountPolicy(description);
    }

    @Test
    public void createComposeDeleteAccountPolicy() {
        final String description = "Account Policy To Be Composed";
        createAccountPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.clickLink(component.getPageRelativePath() + ":cells:9:cell:panelCompose:composeLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", "myrule");
        formTester.setValue("view:configuration:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:1:value:spinner", 0);

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "myrule");

        Assert.assertNotNull(component);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:exit");

        closeCallBack(modal);

        deleteAccountPolicy(description);
    }

    @Test
    public void createUpdateDeletePasswordPolicy() {
        final String description = "Password Policy To Be Updated";
        createPasswordPolicy(description);

        Component component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
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
        TESTER.assertLabel(component.getPageRelativePath() + ":cells:6:cell", "2");

        TESTER.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelEdit:editLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:8:cell:panelCompose:composeLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:add");

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.setValue("view:name:textField", "myrule");
        formTester.setValue("view:configuration:dropDownChoiceField", "0");
        formTester.submit("buttons:next");

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:"
                + "container:content:wizard:form:view:bean:propView:0:value:spinner", 0);

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:3:"
                + "outer:form:content:container:content:wizard:form");
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp("name", "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:"
                + "content:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "myrule");

        Assert.assertNotNull(component);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:3:outer:form:content:container:content:exit");

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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");
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
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelCompose:composeLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer", Modal.class);

        Component modal = TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form");

        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:"
                + "correlationRules:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        formTester.setValue("content:conflictResolutionAction:dropDownChoiceField", "1");
        formTester.setValue("content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        closeCallBack(modal);

        component = findComponentByProp("description", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", description);

        Assert.assertNotNull(component);
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelCompose:composeLink");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer", Modal.class);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:conflictResolutionAction:dropDownChoiceField", ConflictResolutionAction.FIRSTMATCH);

        TESTER.assertModelValue("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:correlationRules:multiValueContainer:innerForm:content:view:0:panel:"
                + "jsonRule:paletteField:recorder", "fullname");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:dialog:footer:buttons:0:button");

        closeCallBack(modal);

        deletePullPolicy(description);
    }
}
