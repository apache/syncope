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
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class AnyTypeClassesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToAnyTypeClasses();

        Component component = findComponentByProp(KEY, DATATABLE_PATH, "csv");
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // click edit
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                BaseModal.class);
    }

    @Test
    public void create() {
        browsingToAnyTypeClasses();
        final String anyTypeClassTest = "anyTypeClassTest";

        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", anyTypeClassTest);
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:derSchemas:paletteField:recorder", "mderiveddata");

        TESTER.clearFeedbackMessages();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();

        TESTER.clearFeedbackMessages();
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        TESTER.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);

        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassTest);

        TESTER.assertLabel(result.getPageRelativePath() + ":cells:3:cell", "[mderiveddata]");
    }

    private void createPlainSchema(final String key) {
        browsingToPlainSchemas();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:details:key:textField", key);
        formTester.setValue("content:form:view:details:type:dropDownChoiceField", "3");
        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:0:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void update() {
        String plainSchema = "anyPlainSchema";
        String name = "anyTypeClassToUpdate";
        createAnyTypeClassWithoutSchema(name);
        createPlainSchema(plainSchema);
        browsingToAnyTypeClasses();

        Component component = findComponentByProp(KEY, DATATABLE_PATH, name);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // click edit
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:plainSchemas:paletteField:recorder", plainSchema);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp(KEY, DATATABLE_PATH, name);
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");

        // click delete
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp(KEY, DATATABLE_PATH, name);
        assertNull(component);
    }

    @Test
    public void delete() {
        final String anyTypeClassName = "zStringDelete";
        createAnyTypeClassWithoutSchema(anyTypeClassName);
        browsingToAnyTypeClasses();
        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component component = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");

        // click delete
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        assertSuccessMessage();
        TESTER.cleanupFeedbackMessages();

        component = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);
        assertNull(component);
    }
}
