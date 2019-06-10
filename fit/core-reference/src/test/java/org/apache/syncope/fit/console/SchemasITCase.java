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
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class SchemasITCase extends AbstractTypesITCase {

    @Test
    public void readPlainSchema() {
        browsingToPlainSchemas();
        UTILITY_UI.getTester().assertLabel(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:"
                + "checkgroup:dataTable:body:rows:1:cells:1:cell", "aLong");

        UTILITY_UI.getTester().executeAjaxEvent(
                PLAIN_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable:body:rows:1",
                Constants.ON_CLICK);

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:form:view:kind:dropDownChoiceField", DropDownChoice.class);
    }

    @Test
    public void createPlainSchema() {
        browsingToPlainSchemas();
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:details:key:textField", "zBoolean");
        formTester.setValue("content:form:view:details:type:dropDownChoiceField", "3");
        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:0:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
        UTILITY_UI.getTester().assertRenderedPage(Types.class);
    }

    @Test
    public void updatePlainSchema() {
        browsingToPlainSchemas();

        Component result = UTILITY_UI.findComponentByProp(KEY, PLAIN_DATATABLE_PATH, "ctype");

        UTILITY_UI.getTester().assertLabel(result.getPageRelativePath() + ":cells:1:cell", "ctype");
        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:form:view:kind:dropDownChoiceField", DropDownChoice.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:details:multivalue:checkboxField", "true");

        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:0:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
    }

    @Test
    public void deletePlainSchema() {
        browsingToPlainSchemas();
        //create new Plain Schema
        final String schemaName = "zStringDelete";
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:details:key:textField", schemaName);
        formTester.setValue("content:form:view:details:type:dropDownChoiceField", "0");
        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:0:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");;

        UTILITY_UI.getTester().cleanupFeedbackMessages();

        //delete plain schema
        UTILITY_UI.getTester().clickLink(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:bottomToolbars:toolbars:3:span:navigator:last");

        UTILITY_UI.getTester().assertComponent(PLAIN_DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = UTILITY_UI.findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName);
        assertNotNull(result);

        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName));
    }

    @Test
    public void createVirtualSchema() {
        browsingToVirtualSchemas();
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:container:content:add");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer:form");

        formTester.setValue("content:form:view:details:resource:dropDownChoiceField", "0");
        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer:form:"
                + "content:form:view:details:resource:dropDownChoiceField", Constants.ON_CHANGE);

        formTester.setValue("content:form:view:details:key:textField", "mynewvir");
        formTester.setValue("content:form:view:details:resource:dropDownChoiceField", "0");
        formTester.setValue("content:form:view:details:anyType:dropDownChoiceField", "0");
        formTester.setValue("content:form:view:details:extAttrName:textField", "virattr");

        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:2:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        Component result = UTILITY_UI.findComponentByProp(KEY, VIRTUAL_DATATABLE_PATH, "mynewvir");
        UTILITY_UI.getTester().executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        UTILITY_UI.getTester().getRequest().addParameter("confirm", "true");
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        UTILITY_UI.getTester().executeAjaxEvent(UTILITY_UI.getTester().getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");
        UTILITY_UI.getTester().cleanupFeedbackMessages();

        assertNull(UTILITY_UI.findComponentByProp(KEY, VIRTUAL_DATATABLE_PATH, "mynewvir"));
    }
}
