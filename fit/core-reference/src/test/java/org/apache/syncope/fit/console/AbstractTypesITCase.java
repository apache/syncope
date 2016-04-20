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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;

public abstract class AbstractTypesITCase extends AbstractConsoleITCase {

    protected static final String PLAIN_DATATABLE_PATH = "body:content:tabbedPanel:panel:"
            + "accordionPanel:tabs:0:body:content:container:content:searchContainer:resultTable";

    protected static final String DATATABLE_PATH =
            "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable";

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    protected void browsingToRelationshipType() {
        wicketTester.clickLink("body:configurationLI:configurationUL:typesLI:types");
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        wicketTester.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypes() {
        wicketTester.clickLink("body:configurationLI:configurationUL:typesLI:types");
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        wicketTester.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypeClasses() {
        wicketTester.clickLink("body:configurationLI:configurationUL:typesLI:types");
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        wicketTester.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToPlainSchemas() {
        wicketTester.clickLink("body:configurationLI:configurationUL:typesLI:types");
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
        wicketTester.assertComponent(PLAIN_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void createPlainSchema(final String key) {
        browsingToPlainSchemas();
        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:details:form:key:textField", key);
        formTester.setValue("content:details:form:type:dropDownChoiceField", "3");

        wicketTester.clickLink("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");

        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
    }

    protected void createAnyTypeClassWithoutSchema(final String name) {
        browsingToAnyTypeClasses();

        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        final FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", name);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
    }

    protected void createAnyType(final String name) {
        browsingToAnyTypes();

        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");
        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        final FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeDetailsPanel:container:form:key:textField", name);

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
    }

    protected void createRelationshipType(final String name) {
        browsingToRelationshipType();

        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");

        wicketTester.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        final FormTester formTester = wicketTester.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:relationshipTypeDetails:container:form:key:textField", name);
        formTester.setValue(
                "content:relationshipTypeDetails:container:form:description:textField", "test relationshipType");

        wicketTester.clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
        wicketTester.assertRenderedPage(Types.class);
    }
}
