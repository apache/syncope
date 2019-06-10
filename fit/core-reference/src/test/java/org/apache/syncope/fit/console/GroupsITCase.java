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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.junit.jupiter.api.BeforeEach;

public class GroupsITCase extends AbstractConsoleITCase {

    private final static String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private final static String CONTAINER = TAB_PANEL + "container:content:";

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void read() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        Component component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "artDirector");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:3:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:7:outer:container:content:togglePanelContainer:membersForm");

        formTester.select("type:dropDownChoiceField", 0);
        formTester.submit("changeit");

        UTILITY_UI.getTester().assertModelValue(TAB_PANEL
                + "outerObjectsRepeater:6:outer:dialog:header:header-label", "USER members of artDirector");
        assertNotNull(UTILITY_UI.findComponentByProp("username", TAB_PANEL
                + "outerObjectsRepeater:6:outer:form:content:searchResult:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "puccini"));

        UTILITY_UI.getTester().executeAjaxEvent(TAB_PANEL
                + "outerObjectsRepeater:6:outer:dialog:footer:buttons:0:button", Constants.ON_CLICK);
    }

    @Test
    public void filteredSearch() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:title");

        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panelPlus:add",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:accordionPanel:tabs:0:body:"
                + "content:searchFormContainer:search:multiValueContainer:innerForm:content:view:0:panel:container:"
                + "value:textField", TextField.class);
    }

    private void cloneGroup(final String group) {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        Component component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", group);
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:name:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.setValue("view:name:textField", group + "_clone");
        formTester.submit("buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", group + "_clone");
        assertNotNull(component);
    }

    @Test
    public void clickToCloneGroup() {
        cloneGroup("director");

        Component component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director_clone");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:9:action:action");

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:9:action:action"), Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void editGroup() {
        cloneGroup("director");

        Component component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director_clone");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form:view:name:textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:form");
        assertNotNull(formTester);
        formTester.submit("buttons:next");

        // -------------------------
        // SYNCOPE-1026
        // -------------------------
        assertEquals(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:"
                + "searchResult:outerObjectsRepeater:0:outer:form:content:form:view:ownerContainer:search:userOwner:"
                + "textField").getDefaultModelObjectAsString(), "[823074dc-d280-436d-a7dd-07399fae48ec] puccini");

        UTILITY_UI.getTester().clickLink(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:0:"
                + "outer:form:content:form:view:ownerContainer:search:userOwnerReset");

        assertEquals(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:body:container:content:tabbedPanel:panel:"
                + "searchResult:outerObjectsRepeater:0:outer:form:content:form:view:ownerContainer:search:userOwner:"
                + "textField").getDefaultModelObjectAsString(), StringUtils.EMPTY);
        // -------------------------

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
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:customResultBody:resources:firstLevelContainer:first:"
                + "container:content:group:beans:0:fields:0:field", Label.class);

        UTILITY_UI.getTester().clickLink(TAB_PANEL
                + "outerObjectsRepeater:0:outer:form:content:action:actionRepeater:0:action:action");

        component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director_clone");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:9:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:9:action:action"), Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void checkDeleteGroupLink() {
        UTILITY_UI.getTester().clickLink("body:realmsLI:realms");
        UTILITY_UI.getTester().clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:2:link");

        Component component = UTILITY_UI.findComponentByProp("name", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "director");
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(TAB_PANEL
                + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:9:action:action", IndicatingOnConfirmAjaxLink.class);
    }
}
