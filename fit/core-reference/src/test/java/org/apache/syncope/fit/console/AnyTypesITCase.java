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
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class AnyTypesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToAnyTypes();
        TESTER.assertComponent(
                DATATABLE_PATH
                + ":tablePanel:groupForm:"
                + "checkgroup:dataTable:body:rows:1:cells:1:cell", Label.class);

        Component component = findComponentByProp(KEY, DATATABLE_PATH, "GROUP");

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
        browsingToAnyTypes();
        final String anyTypeTest = "anyTypeTest2";

        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeDetailsPanel:container:form:key:textField", anyTypeTest);
        formTester.setValue("content:anyTypeDetailsPanel:container:form:classes:paletteField:recorder", "csv");

        TESTER.clearFeedbackMessages();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();

        TESTER.clearFeedbackMessages();
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component component = findComponentByProp(KEY, DATATABLE_PATH, anyTypeTest);

        TESTER.assertLabel(component.getPageRelativePath() + ":cells:1:cell", anyTypeTest);
        TESTER.assertLabel(component.getPageRelativePath() + ":cells:3:cell", "[csv]");

        // issue SYNCOPE-1111
        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.assertRenderedPage(Realms.class);
        TESTER.assertLabel(
                "body:content:body:container:content:tabbedPanel:tabs-container:tabs:4:link:title",
                anyTypeTest);
    }

    @Test
    public void update() {
        final String name = "anyTypeClassUpdate";
        createAnyTypeClassWithoutSchema(name);
        browsingToAnyTypes();

        Component component = findComponentByProp(KEY, DATATABLE_PATH, "GROUP");
        assertNotNull(component);

        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // click edit
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        final FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:anyTypeDetailsPanel:container:form:classes:paletteField:recorder", name + ",minimal group");

        TESTER.clearFeedbackMessages();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
    }

    private void createAnyType(final String name) {
        browsingToAnyTypes();

        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        final FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeDetailsPanel:container:form:key:textField", name);

        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
        TESTER.clearFeedbackMessages();
    }

    @Test
    public void delete() {
        String name = "anyTypeDelete";
        createAnyType(name);
        browsingToAnyTypes();

        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);
        Component component = findComponentByProp(KEY, DATATABLE_PATH, name);
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
}
