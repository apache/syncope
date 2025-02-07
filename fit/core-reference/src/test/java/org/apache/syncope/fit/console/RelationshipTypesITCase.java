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
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class RelationshipTypesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToRelationshipType();

        Component result = findComponentByProp(KEY, DATATABLE_PATH, "inclusion");

        TESTER.assertComponent(
                result.getPageRelativePath() + ":cells:1:cell", Label.class);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        TESTER.assertComponent(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", BaseModal.class);
    }

    private void createRelationshipType(final String name) {
        browsingToRelationshipType();

        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");

        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:relationshipTypeDetails:container:form:key:textField", name);
        formTester.setValue(
                "content:relationshipTypeDetails:container:form:description:textField", "test relationshipType");
        formTester.select("content:relationshipTypeDetails:container:form:leftEndAnyType:dropDownChoiceField", 0);
        formTester.select("content:relationshipTypeDetails:container:form:rightEndAnyType:dropDownChoiceField", 0);

        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        assertSuccessMessage();
        TESTER.clearFeedbackMessages();

        TESTER.assertRenderedPage(Types.class);
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

        Component result = findComponentByProp(KEY, DATATABLE_PATH, name);
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action");

        final FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue(
                "content:relationshipTypeDetails:container:form:description:textField", "new description");

        TESTER.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
    }

    @Test
    public void delete() {
        final String name = "relationshipTypeDelete";
        createRelationshipType(name);
        browsingToRelationshipType();

        TESTER.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, name);
        assertNotNull(result);

        TESTER.executeAjaxEvent(result.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.getRequest().addParameter("confirm", "true");

        TESTER.clickLink(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"));

        TESTER.executeAjaxEvent(TESTER.getComponentFromLastRenderedPage(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:1:outer:container:content:"
                + "togglePanelContainer:container:actions:actions:actionRepeater:1:action:action"), Constants.ON_CLICK);

        assertSuccessMessage();

        TESTER.cleanupFeedbackMessages();
        result = findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNull(result);
    }
}
