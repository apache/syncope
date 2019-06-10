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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.junit.jupiter.api.BeforeEach;

public class UsersITCase extends AbstractConsoleITCase {

    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:title");

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panelPlus:add",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panel:container:value:"
                + "textField", TextField.class);
        UTILITY_UI.getTester().assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:view:1:panel:container:value:"
                + "textField", TextField.class);
    }

    @Test
    public void forceChangePassword() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "verdi");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:2:action:action");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
    }

    @Test
    public void clickToCloneUser() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editRelationships() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:actions:"
                + "actionRepeater:0:action:action", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:relationships:specification:type:dropDownChoiceField", "1");
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:"
                + "specification:type:dropDownChoiceField", Constants.ON_CHANGE);
        // The ON_CHANGE above should enable this component, but it doesn't; doing it by hand
        Component otherType = UTILITY_UI.findComponentById(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:specification",
                "otherType");
        assertNotNull(otherType);
        otherType.setEnabled(true);

        formTester.setValue("view:relationships:specification:otherType:dropDownChoiceField", "PRINTER");
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:relationships:"
                + "specification:otherType:dropDownChoiceField", Constants.ON_CHANGE);

        component = UTILITY_UI.findComponentByProp("name", TAB_PANEL + "outerObjectsRepeater:"
                + "0:outer:form:content:form:view:relationships:specification:searchPanelContainer:searchPanel:"
                + "searchResultPanel:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:"
                + "dataTable:body:rows:1:cells:2:cell", "Canon MF 8030cn");
        assertNotNull(component);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editUser() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);
    }

    @Test
    public void editUserMembership() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().
                executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNull(component);

        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "additional,root,otherchild");
        UTILITY_UI.getTester().executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:title",
                Constants.ON_CLICK);

        formTester.setValue("view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:0:panel:spinner", "1");
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        // reset ....
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "root,otherchild");
        UTILITY_UI.getTester().executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:buttons:finish", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void editUserMemberships() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().
                executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:2:button",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        // click on "edit"
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // add "additional" group in order to show membership attributes
        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "additional,root,otherchild");
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:buttons:next",
                Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // open membership attributes accordion
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:title",
                Constants.ON_CLICK);

        // edit multivalue text field, set 2 elements in total
        UTILITY_UI.getTester().assertComponent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:0:panel:field",
                TextField.class);
        formTester.setValue("view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:5:panel:multiValueContainer:innerForm:content:view:0:panel:field", "2019-03-05");

        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(TAB_PANEL
                + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:0:panelPlus:add"));

        UTILITY_UI.getTester().assertComponent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:1:panel:field",
                TextField.class);
        formTester.setValue("view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:5:panel:multiValueContainer:innerForm:content:view:1:panel:field", "2019-03-06");

        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(TAB_PANEL
                + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:1:panelPlus:add"));

        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        // reopen form and go to Plain Attributes page...
        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // add "additional" group in order to show membership attributes
        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "additional,root,otherchild");
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:buttons:next",
                Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // open membership attributes accordion
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:title",
                Constants.ON_CLICK);

        // ... check multivalue field values has been saved
        UTILITY_UI.getTester().assertComponent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:0:panel:field",
                TextField.class);

        UTILITY_UI.getTester().assertComponent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:"
                + "body:content:schemas:5:panel:multiValueContainer:innerForm:content:view:1:panel:field",
                TextField.class);

        Calendar cal = Calendar.getInstance();
        cal.set(2019, Calendar.MARCH, 5, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2019, Calendar.MARCH, 6, 0, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        UTILITY_UI.getTester().assertModelValue(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:5:panel:multiValueContainer:innerForm:content:view:0:panel:field", cal.getTime());
        UTILITY_UI.getTester().assertModelValue(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:5:panel:multiValueContainer:innerForm:content:view:1:panel:field", cal2.getTime());

        // ... remove all values from multivalue field
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:"
                + "content:schemas:5:panel:multiValueContainer:innerForm:content:view:1:drop",
                Constants.ON_CLICK);
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:"
                + "content:schemas:5:panel:multiValueContainer:innerForm:content:view:0:drop",
                Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        // reopen form and go to Plain Attributes page...
        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // add "additional" group in order to show membership attributes
        formTester.setValue("view:groupsContainer:groups:paletteField:recorder", "additional,root,otherchild");
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:buttons:next",
                Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        // open membership attributes accordion
        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:title",
                Constants.ON_CLICK);

        // ... check multivalue field is now empty
        UTILITY_UI.getTester().assertModelValue(TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:content:"
                + "schemas:5:panel:multiValueContainer:innerForm:content:view:0:panel:field", null);
        component = UTILITY_UI.findComponentByProp("syncope-path", TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:"
                + "content:schemas:5:panel:multiValueContainer:innerForm",
                TAB_PANEL + "outerObjectsRepeater:0:"
                + "outer:form:content:form:view:membershipsPlainSchemas:0:membershipPlainSchemas:tabs:0:body:"
                + "content:schemas:5:panel:multiValueContainer:innerForm:content:view:1:panel:field");
        assertNull(component);

        // close the wizard
        formTester.submit("buttons:cancel");
    }

    @Test
    public void checkDeleteUsrLink() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:8:action:action", IndicatingOnConfirmAjaxLink.class);
    }

    @Test
    public void editDateTimeField() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:1:panel:field:datepicker", "1/19/17");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:1:panel:field:timepicker", "12:00 AM");

        formTester.setValue("view:plainSchemas:tabs:0:body:"
                + "content:schemas:8:panel:multiValueContainer:innerForm:content:view:0:panel:field", "2017-01-19");

        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:1:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                TextField.class);

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        Calendar cal = Calendar.getInstance();
        cal.set(2017, Calendar.JANUARY, 19, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        UTILITY_UI.getTester().assertModelValue("body:content:body:container:content:"
                + "tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "0:outer:form:content:form:view:plainSchemas:tabs:0:"
                + "body:content:schemas:1:panel:field:datepicker", cal.getTime());

        assertEquals(UTILITY_UI.getTester().getComponentFromLastRenderedPage("body:content:body:"
                + "container:content:tabbedPanel:panel:searchResult:"
                + "outerObjectsRepeater:0:outer:form:content:form:view:plainSchemas:"
                + "tabs:0:body:content:schemas:1:panel:field:timepicker").getDefaultModelObjectAsString(), "12:00 AM");

        UTILITY_UI.getTester().assertModelValue("body:content:body:container:content:"
                + "tabbedPanel:panel:searchResult:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:plainSchemas:tabs:0:body:content:schemas:8:panel:"
                + "multiValueContainer:innerForm:content:view:0:panel:field", cal.getTime());
    }

    @Test
    public void changePassword() {
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "vivaldi");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        UTILITY_UI.getTester().assertLabel(TAB_PANEL + "outerObjectsRepeater:3:outer:form:content:status:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", "syncope");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:3:outer:form");
        formTester.setValue("content:passwordPanel:passwordInnerForm:password:passwordField", "Password345");
        formTester.setValue("content:passwordPanel:passwordInnerForm:confirmPassword:passwordField", "Password345");
        formTester.select("content:status:resources:firstLevelContainer:first:container:content:group", 0);

        UTILITY_UI.getTester().executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        component = UTILITY_UI.findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "vivaldi");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        UTILITY_UI.getTester().assertLabel(TAB_PANEL + "outerObjectsRepeater:3:outer:form:content:status:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", "syncope");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:3:outer:form");
        formTester.setValue("content:passwordPanel:passwordInnerForm:password:passwordField", "Password123");
        formTester.setValue("content:passwordPanel:passwordInnerForm:confirmPassword:passwordField", "Password123");
        formTester.select("content:status:resources:firstLevelContainer:first:container:content:group", 0);

        UTILITY_UI.getTester().executeAjaxEvent(
                TAB_PANEL + "outerObjectsRepeater:3:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }
}
