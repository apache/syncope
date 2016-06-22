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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.util.tester.FormTester;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.junit.Before;

@FixMethodOrder(MethodSorters.JVM)
public class AnyObjectsITCase extends AbstractConsoleITCase {

    private final String tabPanel = "body:content:body:tabbedPanel:panel:searchResult:";

    private final String searchResultContainer = tabPanel + "container:content:";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");

        TESTER.clickLink("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:title");

        TESTER.executeAjaxEvent("body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:"
                + "searchFormContainer:search:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);

        TESTER.assertComponent(
                "body:content:body:tabbedPanel:panel:accordionPanel:tabs:0:body:content:searchFormContainer:search:"
                + "multiValueContainer:innerForm:content:view:0:panel:container:value:textField", TextField.class);
    }

    @Test
    public void clickToClonePrinter() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");

        Component component = findComponentByProp("key", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:4:cell:panelClone:cloneLink");

        FormTester formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editPrinter() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");

        Component component = findComponentByProp("key", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        TESTER.clickLink(component.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink");

        TESTER.assertComponent(tabPanel + "outerObjectsRepeater:0:outer:form:content:form:view:status:"
                + "resources:firstLevelContainer:first:container:content:group:beans:0:fields:0", ListItem.class);

        FormTester formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        TESTER.cleanupFeedbackMessages();

        formTester = TESTER.newFormTester(tabPanel + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.assertComponent(tabPanel
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", Label.class);

        TESTER.clickLink(tabPanel + "outerObjectsRepeater:0:outer:form:content:action:panelClose:closeLink");

        component = findComponentByProp("key", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);
    }

    @Test
    public void checkDeletePrinterLink() {
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");

        Component component = findComponentByProp("key", searchResultContainer
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        TESTER.assertComponent(component.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);
    }
}
