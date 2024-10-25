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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Parameters;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParametersITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:keymasterLI:keymasterUL:parametersLI:parameters", false);
        TESTER.assertRenderedPage(Parameters.class);
    }

    @Test
    public void readParameter() {
        TESTER.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);
        assertNotNull(findComponentByProp(SCHEMA, "body:content:parametersPanel", "authentication.statuses"));
    }

    @Test
    public void createParameter() {
        TESTER.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        TESTER.clickLink("body:content:parametersPanel:container:content:add");
        TESTER.assertComponent("body:content:parametersPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester =
                TESTER.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:parametersCreateWizardPanel:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:schema:textField", "testParam");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:attrs:0:panel:textField", "test");

        formTester.submit("content:parametersCreateWizardPanel:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
        TESTER.assertRenderedPage(Parameters.class);
    }

    @Test
    public void updateParameter() {
        TESTER.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        Component result = findComponentByProp(SCHEMA, "body:content:parametersPanel", "notification.maxRetries");
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:parametersPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester =
                TESTER.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:parametersCreateWizardPanel:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:parametersCreateWizardPanel:form:view:content:attrs:0:panel:numberTextField", "70");

        formTester.submit("content:parametersCreateWizardPanel:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        TESTER.assertRenderedPage(Parameters.class);
    }

    @Test
    public void deleteParameter() {
        TESTER.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        TESTER.clickLink("body:content:parametersPanel:container:content:add");
        TESTER.assertComponent("body:content:parametersPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester =
                TESTER.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:parametersCreateWizardPanel:form:buttons:next");

        formTester = TESTER.newFormTester(
                "body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:schema:textField", "deleteParam");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:attrs:0:panel:textField", "test");

        formTester.submit("content:parametersCreateWizardPanel:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        Component result = findComponentByProp(SCHEMA, "body:content:parametersPanel", "deleteParam");
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink("body:content:parametersPanel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }
}
