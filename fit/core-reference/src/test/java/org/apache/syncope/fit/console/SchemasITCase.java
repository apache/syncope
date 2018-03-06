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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Test;

public class SchemasITCase extends AbstractTypesITCase {

    @Test
    public void readPlainSchema() {
        browsingToPlainSchemas();
        TESTER.assertLabel(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:"
                + "checkgroup:dataTable:body:rows:1:cells:1:cell", "aLong");

        TESTER.executeAjaxEvent(
                PLAIN_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable:body:rows:1",
                Constants.ON_CLICK);

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:kindForm:kind:dropDownChoiceField", DropDownChoice.class);
    }

    @Test
    public void createPlainSchema() {
        browsingToPlainSchemas();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:key:textField", "zBoolean");
        formTester.setValue("content:details:form:type:dropDownChoiceField", "3");

        TESTER.clickLink("body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:"
                + "outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.cleanupFeedbackMessages();
        TESTER.assertRenderedPage(Types.class);
    }

    @Test
    public void updatePlainSchema() {
        browsingToPlainSchemas();

        Component result = findComponentByProp(KEY, PLAIN_DATATABLE_PATH, "ctype");

        TESTER.assertLabel(result.getPageRelativePath() + ":cells:1:cell", "ctype");
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:kindForm:kind:dropDownChoiceField", DropDownChoice.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:multivalue:checkboxField", "true");

        TESTER.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                true);

        TESTER.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void deletePlainSchema() {
        browsingToPlainSchemas();
        //create new Plain Schema
        final String schemaName = "zStringDelete";
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:key:textField", schemaName);
        formTester.setValue("content:details:form:type:dropDownChoiceField", "0");

        TESTER.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        TESTER.assertInfoMessages("Operation executed successfully");;

        TESTER.cleanupFeedbackMessages();

        //delete plain schema
        TESTER.clickLink(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:bottomToolbars:toolbars:3:span:navigator:last");

        TESTER.assertComponent(PLAIN_DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName);
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName));
    }

    @Test
    public void createVirtualSchema() {
        browsingToVirtualSchemas();
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:container:content:add");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = TESTER.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer:form");

        formTester.setValue("content:details:form:resource:dropDownChoiceField", "0");
        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:2:body:content:outerObjectsRepeater:0:outer:form:"
                + "content:details:form:resource:dropDownChoiceField", Constants.ON_CHANGE);

        formTester.setValue("content:details:form:key:textField", "mynewvir");
        formTester.setValue("content:details:form:resource:dropDownChoiceField", "0");
        formTester.setValue("content:details:form:anyType:dropDownChoiceField", "0");
        formTester.setValue("content:details:form:extAttrName:textField", "virattr");

        TESTER.executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:"
                + "outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit", Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();
        TESTER.assertRenderedPage(Types.class);

        Component result = findComponentByProp(KEY, VIRTUAL_DATATABLE_PATH, "mynewvir");
        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action");

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:2:body:content:outerObjectsRepeater:1:outer:"
                + "container:content:togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"),
                Constants.ON_CLICK);

        TESTER.assertInfoMessages("Operation executed successfully");
        TESTER.cleanupFeedbackMessages();

        assertNull(findComponentByProp(KEY, VIRTUAL_DATATABLE_PATH, "mynewvir"));
    }
}
