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
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class AnyTypeClassesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToAnyTypeClasses();

        Component component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, "csv");
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // click edit
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                BaseModal.class);
    }

    @Test
    public void create() {
        browsingToAnyTypeClasses();
        final String anyTypeClassTest = "anyTypeClassTest";

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", anyTypeClassTest);
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:derSchemas:paletteField:recorder", "mderiveddata");

        UTILITY_UI.getTester().clearFeedbackMessages();
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().clearFeedbackMessages();
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);

        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassTest);

        UTILITY_UI.getTester().assertLabel(result.getPageRelativePath() + ":cells:3:cell", "[mderiveddata]");
    }

    @Test
    public void update() {
        final String plainSchema = "anyPlainSchema";
        final String name = "anyTypeClassToUpdate";
        createAnyTypeClassWithoutSchema(name);
        createPlainSchema(plainSchema);
        browsingToAnyTypeClasses();

        Component component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, name);
        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // click edit
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:plainSchemas:paletteField:recorder", plainSchema);

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, name);
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");

        // click delete
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
        component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNull(component);
    }

    @Test
    public void delete() {
        final String anyTypeClassName = "zStringDelete";
        createAnyTypeClassWithoutSchema(anyTypeClassName);
        browsingToAnyTypeClasses();
        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);
        assertNotNull(component);

        UTILITY_UI.getTester().executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");

        // click delete
        UTILITY_UI.getTester().clickLink(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"));

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        component = UTILITY_UI.findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);

        assertNull(component);
    }
}
