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
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class SchemasITCase extends AbstractTypesITCase {

    @Test
    public void readPlainSchema() {
        browsingToPlainSchemas();
        wicketTester.assertLabel(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:"
                + "checkgroup:dataTable:body:rows:1:cells:1:cell", "aLong");

        wicketTester.assertComponent(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:7:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        wicketTester.clickLink(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:7:cell:panelEdit:editLink");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:kindForm:kind:dropDownChoiceField", DropDownChoice.class);
    }

    @Test
    public void createPlainSchema() {
        browsingToPlainSchemas();
        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:key:textField", "zBoolean");
        formTester.setValue("content:details:form:type:dropDownChoiceField", "3");

        wicketTester.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
        wicketTester.assertRenderedPage(Types.class);
    }

    @Test
    public void updatePlainSchema() {
        browsingToPlainSchemas();

        Component result = findComponentByProp(KEY, PLAIN_DATATABLE_PATH, "ctype");

        wicketTester.assertLabel(
                result.getPageRelativePath() + ":cells:1:cell", "ctype");

        wicketTester.clickLink(
                result.getPageRelativePath() + ":cells:7:cell:panelEdit:editLink");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:"
                + "panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:"
                + "form:content:kindForm:kind:dropDownChoiceField", DropDownChoice.class);

        FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:multivalue:checkboxField", "true");

        wicketTester.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit",
                true);

        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void deletePlainSchema() {
        browsingToPlainSchemas();
        //create new Plain Schema
        final String schemaName = "zStringDelete";
        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:key:textField", schemaName);
        formTester.setValue("content:details:form:type:dropDownChoiceField", "0");

        wicketTester.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");;

        wicketTester.cleanupFeedbackMessages();

        //delete plain schema
        wicketTester.clickLink(
                PLAIN_DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:bottomToolbars:toolbars:3:span:navigator:last");

        wicketTester.assertComponent(PLAIN_DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName);

        wicketTester.assertComponent(
                result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:7:cell:panelDelete:deleteLink"), "onclick");
        wicketTester.assertInfoMessages("Operation executed successfully");
        wicketTester.cleanupFeedbackMessages();

        assertNull(findComponentByProp(KEY, PLAIN_DATATABLE_PATH, schemaName));
    }
}
