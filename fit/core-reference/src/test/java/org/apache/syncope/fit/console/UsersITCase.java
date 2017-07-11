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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Test;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.junit.Before;

public class UsersITCase extends AbstractConsoleITCase {

    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        TESTER.clickLink("body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:title");

        TESTER.executeAjaxEvent("body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panelPlus:add",
                Constants.ON_CLICK);

        TESTER.assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panel:container:value:"
                + "textField", TextField.class);
    }

    @Test
    public void forceChangePassword() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "verdi");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:2:action:action");

        TESTER.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void clickToCloneUser() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:1:action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editRelationships() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:actions:"
                + "actionRepeater:0:action:action", Constants.ON_CLICK);

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:relationships:specification:type:dropDownChoiceField", "1");
        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:"
                + "specification:type:dropDownChoiceField", Constants.ON_CHANGE);

        // The ON_CHANGE above should enable this component, but it doesn't; doing it by hand
        Component rightType = findComponentById(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:specification",
                "rightType");
        assertNotNull(rightType);
        rightType.setEnabled(true);

        formTester.setValue("view:relationships:specification:rightType:dropDownChoiceField", "PRINTER");
        TESTER.executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:"
                + "specification:rightType:dropDownChoiceField", Constants.ON_CHANGE);

        component = findComponentByProp("name", TAB_PANEL + "outerObjectsRepeater:"
                + "0:outer:form:content:form:view:relationships:specification:searchPanelContainer:searchPanel:"
                + "searchResultPanel:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:"
                + "dataTable:body:rows:1:cells:2:cell", "Canon MF 8030cn");
        assertNotNull(component);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editUser() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);
    }

    @Test
    public void editUserMembership() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNull(component);

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "additional,root,otherchild");
        TESTER.executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        TESTER.executeAjaxEvent(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:title",
                Constants.ON_CLICK);

        formTester.setValue("view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:0:panel:spinner", "1");
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        // reset ....
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "root,otherchild");
        TESTER.executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:buttons:finish", Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void checkDeleteUsrLink() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.assertComponent(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:8:action:action", IndicatingOnConfirmAjaxLink.class);
    }

    @Test
    public void editDateTimeField() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:1:panel:field:datepicker", "1/19/17");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:1:panel:field:timepicker", "12:00 AM");

        formTester.setValue("view:plainSchemas:tabs:0:body:"
                + "content:schemas:8:panel:multiValueContainer:innerForm:content:view:0:panel:field", "1/19/17");

        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        Calendar cal = Calendar.getInstance();
        cal.set(2017, Calendar.JANUARY, 19, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TESTER.assertModelValue("body:content:body:container:content:"
                + "tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "0:outer:form:content:form:view:plainSchemas:tabs:0:"
                + "body:content:schemas:1:panel:field:datepicker", cal.getTime());

        assertEquals(TESTER.getComponentFromLastRenderedPage("body:content:body:"
                + "container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form:view:plainSchemas:"
                + "tabs:0:body:content:schemas:1:panel:field:timepicker").getDefaultModelObjectAsString(), "12:00 AM");

        TESTER.assertModelValue("body:content:body:container:content:"
                + "tabbedPanel:panel:searchResult:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:plainSchemas:tabs:0:body:content:schemas:8:panel:"
                + "multiValueContainer:innerForm:content:view:0:panel:field", cal.getTime());
    }

    @Test
    public void changePassword() {
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "vivaldi");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        TESTER.assertLabel(TAB_PANEL + "outerObjectsRepeater:3:outer:form:content:status:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", "syncope");

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:3:outer:form");
        formTester.setValue("content:passwordPanel:passwordInnerForm:password:passwordField", "Password345");
        formTester.setValue("content:passwordPanel:passwordInnerForm:confirmPassword:passwordField", "Password345");
        formTester.select("content:status:resources:firstLevelContainer:first:container:content:group", 0);

        TESTER.executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "vivaldi");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        TESTER.assertLabel(TAB_PANEL + "outerObjectsRepeater:3:outer:form:content:status:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", "syncope");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:3:outer:form");
        formTester.setValue("content:passwordPanel:passwordInnerForm:password:passwordField", "Password123");
        formTester.setValue("content:passwordPanel:passwordInnerForm:confirmPassword:passwordField", "Password123");
        formTester.select("content:status:resources:firstLevelContainer:first:container:content:group", 0);

        TESTER.executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }
}
