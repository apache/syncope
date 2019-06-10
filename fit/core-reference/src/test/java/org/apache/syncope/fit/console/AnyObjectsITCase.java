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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.junit.jupiter.api.BeforeEach;

public class AnyObjectsITCase extends AbstractConsoleITCase {

    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void filteredSearch() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");

        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

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
    }

    @Test
    public void clickToClonePrinter() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

        Component component = UTILITY_UI.findComponentByProp("key", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:1:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);

        formTester.submit("buttons:cancel");
    }

    @Test
    public void editPrinter() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

        Component component = UTILITY_UI.findComponentByProp("key", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
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
        formTester.submit("buttons:next");

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        UTILITY_UI.getTester().cleanupFeedbackMessages();

        formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:"
                + "firstLevelContainer:first:container:content:group:beans:0:fields:0:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("key", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);
    }

    @Test
    public void checkDeletePrinterLink() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");

        Component component = UTILITY_UI.findComponentByProp("key", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:5:action:action",
                IndicatingOnConfirmAjaxLink.class);
    }
}
