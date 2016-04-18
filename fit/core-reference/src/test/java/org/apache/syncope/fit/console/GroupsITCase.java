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
public class GroupsITCase extends AbstractConsoleITCase {

    private final String tabPanel = "body:content:body:tabbedPanel:panel:searchResult:";

    private final String searchResultContainer = tabPanel + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:2:link");

        wicketTester.clickLink("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:title");

        wicketTester.executeAjaxEvent("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        wicketTester.assertComponent(
                "body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:searchFormContainer:search:"
                + "multiValueContainer:innerForm:content:view:0:panel:container:value:textField", TextField.class);
    }

    @Test
    public void clickToCloneGroup() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:2:link");

        Component component = findComponentByProp("name", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "root");
        assertNotNull(component);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:4:cell:panelClone:cloneLink");

        wicketTester.assertComponent(tabPanel + "outerObjectsRepeater:0:outer:form:content:form:view:name:textField",
                TextField.class);

        FormTester formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        
        formTester.submit("buttons:cancel");
    }

    @Test
    public void editGroup() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:2:link");

        Component component = findComponentByProp("name", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "root");
        assertNotNull(component);

        wicketTester.clickLink(component.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink");

        wicketTester.assertComponent(tabPanel + "outerObjectsRepeater:0:outer:form:content:form:view:name:textField",
                TextField.class);

        FormTester formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = wicketTester.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.assertComponent(tabPanel
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:0:field", Label.class);

        wicketTester.clickLink(tabPanel + "outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink");

        component = findComponentByProp("name", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "root");
        assertNotNull(component);

    }

    @Test
    public void checkDeleteGroupLink() {
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:2:link");

        Component component = findComponentByProp("name", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "root");
        assertNotNull(component);

        wicketTester.assertComponent(component.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);
    }
}
