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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;

@FixMethodOrder(MethodSorters.JVM)
public class UsersITCase extends AbstractConsoleITCase {

    private final String tabPanel = "body:content:body:tabbedPanel:panel:searchResult:";

    private final String searchResultContainer = tabPanel + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:1:link");

        wicketTester.clickLink("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:title");

        wicketTester.executeAjaxEvent("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        wicketTester.assertComponent(
                "body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:searchFormContainer:search:"
                + "multiValueContainer:innerForm:content:view:0:panel:container:value:textField", TextField.class);
    }

    @Test
    public void forceChangePassword() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "verdi");
        assertNotNull(component);

        wicketTester.clickLink(component.getPageRelativePath()
                + ":cells:6:cell:panelMustChangePassword:MustChangePasswordLink");

        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void clickToCloneUser() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelClone:cloneLink");

        wicketTester.assertComponent(tabPanel + "modal:form:content:form:view:username:textField", TextField.class);

        FormTester formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editUser() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");

        wicketTester.assertComponent(tabPanel + "modal:form:content:form:view:username:textField", TextField.class);

        FormTester formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "modal:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.assertComponent(tabPanel
                + "modal:form:content:customResultBody:resources:firstLevelContainer:first:container:content:"
                + "group:beans:0:fields:1:field", Label.class);

        wicketTester.clickLink(tabPanel + "modal:form:content:action:panelClose:closeLink");

        component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini");
        assertNotNull(component);
    }

    @Test
    public void checkDeleteUsrLink() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "rossini");
        assertNotNull(component);

        wicketTester.assertComponent(component.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);
    }
}
