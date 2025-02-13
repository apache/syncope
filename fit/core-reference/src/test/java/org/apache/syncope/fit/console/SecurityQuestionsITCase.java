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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Security;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecurityQuestionsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:securityLI:security", false);
        TESTER.assertRenderedPage(Security.class);
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
    }

    private static void createSecurityQuestion(final String name) {
        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:securityQuestionDetailsPanel:container:form:content:textField",
                name);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:configurationLI:configurationUL:securityLI:security", false);
        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
    }

    @Test
    public void read() {
        Label label = (Label) TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:2:cell");
        assertTrue(label.getDefaultModelObjectAsString().startsWith("What&#039;s your "));

        TESTER.executeAjaxEvent(
                "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1", Constants.ON_CLICK);

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action",
                IndicatingAjaxLink.class);
    }

    @Test
    public void create() {
        createSecurityQuestion("What's your preferred team?");
    }

    @Test
    public void update() {
        createSecurityQuestion("What's your preferred color?");
        Component result = findComponentByProp("content", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "What's your preferred color?");

        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:securityQuestionDetailsPanel:container:form:content:textField",
                "What's your preferred car?");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void delete() {
        String name = "What's your preferred color?";
        createSecurityQuestion(name);

        Component result = findComponentByProp("content", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                name);
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"), "onclick");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("content",
                "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable", name));
    }
}
