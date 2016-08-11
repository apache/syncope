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
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:panelPlus:add",
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

        TESTER.clickLink(component.getPageRelativePath()
                + ":cells:6:cell:panelMustChangePassword:MustChangePasswordLink");

        TESTER.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void clickToCloneUser() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelClone:cloneLink");

        TESTER.
                assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
                        TextField.class);

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editUser() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");

        TESTER.
                assertComponent(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form:view:username:textField",
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

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);
    }

    @Test
    public void editUserMembership() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:btn", Constants.ON_CLICK);
        TESTER.executeAjaxEvent("body:content:realmChoicePanel:container:realms:dropdown-menu:buttons:1:button",
                Constants.ON_CLICK);

        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNull(component);

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");

        FormTester formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.setValue("view:groups:paletteField:recorder", "additional,root,otherchild");
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

        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink");

        component = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        // reset ....
        TESTER.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:groups:paletteField:recorder", "root,otherchild");
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

        TESTER.assertComponent(component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);
    }
}
