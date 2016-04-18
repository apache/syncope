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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Parameters;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ParametersITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        browsingToParameters();
    }

    @Test
    public void readParameter() {
        wicketTester.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);
        assertNotNull(findComponentByProp(SCHEMA, "body:content:parametersPanel", "token.expireTime"));
    }

    @Test
    public void createParameter() {
        wicketTester.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        wicketTester.clickLink("body:content:parametersPanel:container:content:add");
        wicketTester.assertComponent("body:content:parametersPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester
                = wicketTester.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:parametersCreateWizardPanel:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:schema:textField", "testParam");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:attrs:0:panel:textField", "test");

        formTester.submit("content:parametersCreateWizardPanel:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
        wicketTester.assertRenderedPage(Parameters.class);
    }

    @Test
    public void updateParameter() {
        wicketTester.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        Component result = findComponentByProp(SCHEMA, "body:content:parametersPanel", "token.expireTime");
        assertNotNull(result);
        wicketTester.clickLink(result.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink");

        FormTester formTester = wicketTester.newFormTester(
                "body:content:parametersPanel:container:content:modalDetails:form");

        formTester.setValue("content:parametersDetailsPanel:container:parametersForm:panel:spinner", "70");
        wicketTester.clickLink("body:content:parametersPanel:"
                + "container:content:modalDetails:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
        wicketTester.assertRenderedPage(Parameters.class);
    }

    @Test
    public void deleteParameter() {
        wicketTester.assertComponent("body:content:parametersPanel", WebMarkupContainer.class);

        wicketTester.clickLink("body:content:parametersPanel:container:content:add");
        wicketTester.assertComponent("body:content:parametersPanel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester
                = wicketTester.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:parametersCreateWizardPanel:form:buttons:next");

        formTester = wicketTester.newFormTester("body:content:parametersPanel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:schema:textField", "deleteParam");
        formTester.setValue("content:parametersCreateWizardPanel:form:view:content:attrs:0:panel:textField", "test");

        formTester.submit("content:parametersCreateWizardPanel:form:buttons:finish");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        wicketTester.clickLink("body:content:parametersPanel:"
                + "container:content:searchContainer:resultTable:tablePanel:"
                + "groupForm:checkgroup:dataTable:bottomToolbars:toolbars:3:span:navigator:last");

        Component result = findComponentByProp(SCHEMA, "body:content:parametersPanel", "deleteParam");
        assertNotNull(result);
        wicketTester.clickLink(result.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink");

        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();
    }

    private void browsingToParameters() {
        wicketTester.clickLink("body:configurationLI:configurationUL:parametersLI:parameters");
        wicketTester.assertRenderedPage(Parameters.class);
    }
}
