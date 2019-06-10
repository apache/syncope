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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NotificationsITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
        UTILITY_UI.getTester().assertRenderedPage(Notifications.class);
    }

    private void createNotification(final String sender, final String subject) {
        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");

        formTester.select("content:form:view:template:dropDownChoiceField", 2);
        formTester.select("content:form:view:traceLevel:dropDownChoiceField", 0);
        formTester.setValue("content:form:view:sender:textField", sender);
        formTester.setValue("content:form:view:subject:textField", subject);

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        UTILITY_UI.getTester().assertNoErrorMessage();

        // -------------------------------
        // recipients
        // -------------------------------
        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:form:"
                + "view:staticRecipients:multiValueContainer:innerForm:content:panelPlus:add", Constants.ON_CLICK);
        formTester.setValue("content:form:view:staticRecipients:multiValueContainer:innerForm:content:view:0:panel:"
                + "textField", "recipient@syncope.org");
        formTester.setValue("content:form:view:selfAsRecipient:checkboxField", true);
        formTester.setValue("content:form:view:recipientAttrName:textField", "email");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        UTILITY_UI.getTester().assertNoErrorMessage();

        // -------------------------------
        // generate event to populate eventsPanel
        // -------------------------------
        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:"
                + "content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField",
                Constants.ON_CHANGE);
        // -------------------------------

        // -------------------------------
        // select event template
        // -------------------------------
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");
        UTILITY_UI.getTester().executeAjaxEvent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form:content:"
                + "form:view:eventSelection:eventsContainer:eventsPanel:successGroup",
                Constants.ON_CLICK);
        // -------------------------------

        formTester.setValue("content:form:view:eventSelection:categoryContainer:category:dropDownChoiceField", "0");
        formTester.setValue("content:form:view:eventSelection:eventsContainer:eventsPanel:successGroup", "check0");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:next");
        UTILITY_UI.getTester().assertNoErrorMessage();

        formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        formTester.submit("content:form:buttons:finish");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");
    }

    @Test
    public void read() {
        assertNull(UTILITY_UI.findComponentByProp(KEY,
                "body:content:tabbedPanel:panel:container:content:searchContainer:"
                + "resultTable:tablePanel:groupForm:checkgroup:dataTable", 1));
    }

    @Test
    public void create() {
        createNotification("create@syncope.org", "create");
    }

    @Test
    public void update() {
        createNotification("update@syncope.org", "createToUpdate");
        Component result = UTILITY_UI.findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToUpdate");

        // edit notification
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void execute() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");

        Component result = UTILITY_UI.findComponentByProp("subject",
                "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable:tablePanel:groupForm:"
                + "checkgroup:dataTable", "Password Reset request");

        // notification tasks link
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", WebMarkupContainer.class);

        result = UTILITY_UI.findComponentByProp("subject",
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81");

        // execute task
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:tasks:"
                + "firstLevelContainer:first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:"
                + "container:actions:actions:actionRepeater:3:action:action");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:notificationsLI:notifications");

        result = UTILITY_UI.findComponentByProp("subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "Password Reset request");

        // notification tasks link
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        result = UTILITY_UI.findComponentByProp("subject",
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81");

        // view task
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:tasks:"
                + "firstLevelContainer:first:outerObjectsRepeater:1:outer:container:content:togglePanelContainer:"
                + "container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertLabel(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:tasks:"
                + "secondLevelContainer:title", "Executions");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:content:tasks:"
                + "secondLevelContainer:back");

        assertNotNull(UTILITY_UI.findComponentByProp("subject",
                "body:content:tabbedPanel:panel:outerObjectsRepeater:4:outer:form:"
                + "content:tasks:firstLevelContainer:first:container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable", "Notification for SYNCOPE-81"));
    }

    @Test
    public void delete() {
        createNotification("delete@syncope.org", "createToDelete");
        Component result = UTILITY_UI.findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete");

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");

        // delete task
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:2:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp("Subject", "body:content:tabbedPanel:panel:container:content:"
                + "searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "createToDelete"));
    }
}
