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
import static org.junit.Assert.assertTrue;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.SecurityQuestions;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SecurityQuestionsITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:configurationLI:configurationUL:securityquestionsLI:securityquestions");
        TESTER.assertRenderedPage(SecurityQuestions.class);
    }

    private void createRealm(final String name) {
        TESTER.clickLink("body:content:securityQuestionPanel:container:content:add");

        TESTER.assertComponent(
                "body:content:securityQuestionPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester =
                TESTER.newFormTester("body:content:securityQuestionPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:securityQuestionDetailsPanel:container:form:content:textField",
                name);

        TESTER.clickLink(
                "body:content:securityQuestionPanel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        TESTER.clickLink("body:configurationLI:configurationUL:securityquestionsLI:securityquestions");
    }

    @Test
    public void read() {
        Label label = (Label) TESTER.getComponentFromLastRenderedPage(
                "body:content:securityQuestionPanel:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:2:cell");
        assertTrue(label.getDefaultModelObjectAsString().startsWith("What&#039;s your "));

        TESTER.assertComponent(
                "body:content:securityQuestionPanel:container:content:"
                + "searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable:body:rows:"
                + "1:cells:3:cell:panelEdit:editLink", IndicatingAjaxLink.class);
    }

    @Test
    public void create() {
        createRealm("What's your preferred team?");
    }

    @Test
    public void update() {
        createRealm("What's your preferred color?");
        Component result = findComponentByProp("content", "body:content:securityQuestionPanel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                "What's your preferred color?");

        assertNotNull(result);

        TESTER.clickLink(result.getPageRelativePath() + ":cells:3:cell:panelEdit:editLink");

        FormTester formTester =
                TESTER.newFormTester("body:content:securityQuestionPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:securityQuestionDetailsPanel:container:form:content:textField",
                "What's your preferred car?");

        TESTER.clickLink(
                "body:content:securityQuestionPanel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void delete() {
        String name = "What's your preferred color?";
        createRealm(name);

        Component result = findComponentByProp("content", "body:content:securityQuestionPanel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable",
                name);
        assertNotNull(result);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"), "onclick");

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp("content",
                "body:content:securityQuestionPanel:container:content:"
                + "searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable", name));
    }
}
