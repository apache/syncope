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

import static org.junit.Assert.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class NotificationsITCase extends AbstractConsoleITCase {

    private void createNotification(final String sender, final String subject) {
        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");

        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");

        // -------------------------------
        // generate event to populate recipientAttrName
        // -------------------------------
        formTester.setValue("content:form:view:recipientAttrType:dropDownChoiceField", "3");
        wicketTester.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:recipientAttrType:dropDownChoiceField", Constants.ON_CHANGE);
        // -------------------------------

        formTester.select("content:form:view:recipientAttrType:dropDownChoiceField", 3);
        formTester.setValue("content:form:view:recipientAttrType:dropDownChoiceField", "3");
        formTester.setValue("content:form:view:recipientAttrName:dropDownChoiceField", "0");
        formTester.select("content:form:view:template:dropDownChoiceField", 2);
        formTester.select("content:form:view:traceLevel:dropDownChoiceField", 0);
        formTester.setValue("content:form:view:sender:textField", sender);
        formTester.setValue("content:form:view:subject:textField", subject);

        wicketTester.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        wicketTester.assertNoErrorMessage();

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");

        // -------------------------------
        // generate event to populate eventsPanel
        // -------------------------------
        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        wicketTester.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:"
                + "content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField",
                Constants.ON_CHANGE);
        // -------------------------------

        // -------------------------------
        // select event template
        // -------------------------------
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");
        wicketTester.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:eventSelection:eventsContainer:eventsPanel:successGroup",
                Constants.ON_CLICK);
        // -------------------------------

        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");

        wicketTester.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        wicketTester.assertNoErrorMessage();

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        wicketTester.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        wicketTester.assertNoErrorMessage();
        wicketTester.assertNoInfoMessage();

        formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        wicketTester.executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:form:"
                + "view:staticRecipients:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);
        formTester.setValue("content:form:view:staticRecipients:multiValueContainer:innerForm:content:view:0:panel:"
                + "textField", "recipient@syncope.org");
        formTester.setValue("content:form:view:selfAsRecipient:checkboxField", true);

        wicketTester.cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:finish");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
    }

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
        wicketTester.assertRenderedPage(Notifications.class);
    }

    @Test
    public void read() {
        assertNull(findComponentByProp(KEY, "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", 1));
    }

    @Test
    public void create() {
        createNotification("create@syncope.org", "create");
    }

    @Test
    public void update() {
        createNotification("update@syncope.org", "createToUpdate");
        Component result = findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToUpdate");

        wicketTester.clickLink(
                result.getPageRelativePath() + ":cells:7:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void delete() {
        createNotification("delete@syncope.org", "createToDelete");
        Component result = findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete");

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"), "onclick");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        assertNull(findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete"));
    }
}
