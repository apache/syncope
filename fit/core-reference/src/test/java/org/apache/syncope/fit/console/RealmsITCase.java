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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RealmsITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        wicketTester.assertLabel("body:content:body:tabbedPanel:panel:container:name:field-label", "Name");
    }

    @Test
    public void create() {
        wicketTester.clickLink("body:content:body:tabbedPanel:panel:actions:actions:panelCreate:createLink");

        wicketTester.assertComponent("body:content:modal", Modal.class);

        FormTester formTester = wicketTester.newFormTester("body:content:modal:form");
        formTester.setValue("content:details:container:name:textField", "testRealm");

        wicketTester.clickLink("body:content:modal:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void update() {
        wicketTester.clickLink("body:content:body:tabbedPanel:panel:actions:actions:panelEdit:editLink");
        wicketTester.assertComponent("body:content:modal", Modal.class);

        FormTester formTester = wicketTester.newFormTester("body:content:modal:form");
        wicketTester.clickLink("body:content:modal:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    @Test
    public void addUserTemplate() {
        wicketTester.clickLink("body:content:body:tabbedPanel:panel:actions:actions:panelTemplate:templateLink");
        wicketTester.assertComponent("body:content:toggleTemplates", TogglePanel.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        wicketTester.assertComponent("body:content:templateModal", Modal.class);

        formTester = wicketTester.newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "'k' + firstname");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:content:body:tabbedPanel:panel:actions:actions:panelTemplate:templateLink");
        wicketTester.assertComponent("body:content:toggleTemplates", TogglePanel.class);

        formTester = wicketTester.newFormTester(
                "body:content:toggleTemplates:container:content:togglePanelContainer:templatesForm");
        formTester.setValue("type:dropDownChoiceField", "0");
        formTester.submit("changeit");

        wicketTester.assertComponent("body:content:templateModal", Modal.class);

        wicketTester.assertModelValue("body:content:templateModal:form:content:form:view:username:textField",
                "'k' + firstname");

        formTester = wicketTester.newFormTester("body:content:templateModal:form:content:form");
        formTester.setValue("view:username:textField", "");
        formTester.submit("buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }
}
