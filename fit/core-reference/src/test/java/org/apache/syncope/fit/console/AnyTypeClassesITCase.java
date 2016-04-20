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
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.util.tester.FormTester;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AnyTypeClassesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToAnyTypeClasses();

        Component result = findComponentByProp(KEY, DATATABLE_PATH, "csv");
        wicketTester.assertLabel(
                result.getPageRelativePath() + ":cells:1:cell", "csv");

        wicketTester.assertComponent(
                result.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink", IndicatingAjaxLink.class);
        
        wicketTester.clickLink(result.getPageRelativePath() + ":cells:6:cell:panelEdit:editLink");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", BaseModal.class);
    }

    @Test
    public void create() {
        browsingToAnyTypeClasses();
        final String anyTypeClassTest = "anyTypeClassTest";

        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        final FormTester formTester
                = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", anyTypeClassTest);
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:derSchemas:paletteField:recorder", "mderiveddata");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        wicketTester.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);

        wicketTester.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassTest);

        wicketTester.assertLabel(result.getPageRelativePath() + ":cells:3:cell", "[mderiveddata]");
    }

    @Test
    public void update() {
        final String plainSchema = "anyPlainSchema";
        createPlainSchema(plainSchema);
        browsingToAnyTypeClasses();

        wicketTester.assertComponent(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:6:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        wicketTester.clickLink(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:6:cell:panelEdit:editLink");

        final FormTester formTester
                = wicketTester.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:anyTypeClassDetailsPanel:form:container:plainSchemas:paletteField:recorder", plainSchema);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void delete() {
        final String anyTypeClassName = "zStringDelete";
        createAnyTypeClassWithoutSchema(anyTypeClassName);
        browsingToAnyTypeClasses();
        wicketTester.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);

        assertNotNull(result);
        wicketTester.assertComponent(
                result.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:6:cell:panelDelete:deleteLink"), "click");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
        result = findComponentByProp(KEY, DATATABLE_PATH, anyTypeClassName);

        assertNull(result);
    }
}
