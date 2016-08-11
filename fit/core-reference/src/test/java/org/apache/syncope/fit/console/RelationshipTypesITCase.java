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

import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Test;

public class RelationshipTypesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToRelationshipType();

        Component result = findComponentByProp(KEY, DATATABLE_PATH, "inclusion");

        TESTER.assertComponent(
                result.getPageRelativePath() + ":cells:1:cell", Label.class);

        TESTER.assertComponent(
                result.getPageRelativePath() + ":cells:3:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        TESTER.clickLink(
                result.getPageRelativePath() + ":cells:3:cell:panelEdit:editLink");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", BaseModal.class);
    }

    @Test
    public void create() {
        final String name = "relationshipTypeTest";
        createRelationshipType(name);
        browsingToRelationshipType();

        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, name);

        TESTER.assertLabel(result.getPageRelativePath() + ":cells:1:cell", name);
        TESTER.assertLabel(result.getPageRelativePath() + ":cells:2:cell", "test relationshipType");
    }

    @Test
    public void update() {
        final String name = "relationshipTypeUpdate";
        createRelationshipType(name);
        browsingToRelationshipType();

        TESTER.assertComponent(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:3:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        TESTER.clickLink(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:3:cell:panelEdit:editLink");

        final FormTester formTester =
                TESTER.newFormTester("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:relationshipTypeDetails:container:form:description:textField", "new description");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        TESTER.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void delete() {
        final String name = "relationshipTypeDelete";
        createRelationshipType(name);
        browsingToRelationshipType();

        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNotNull(result);
        TESTER.assertComponent(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);

        TESTER.getRequest().addParameter("confirm", "true");
        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:3:cell:panelDelete:deleteLink"), "onclick");
        TESTER.assertInfoMessages("Operation executed successfully");

        TESTER.cleanupFeedbackMessages();
        result = findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNull(result);
    }
}
