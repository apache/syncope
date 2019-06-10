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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractTypesITCase extends AbstractConsoleITCase {

    protected static final String PLAIN_DATATABLE_PATH = "body:content:tabbedPanel:panel:"
            + "accordionPanel:tabs:0:body:content:container:content:searchContainer:resultTable";

    protected static final String VIRTUAL_DATATABLE_PATH = "body:content:tabbedPanel:panel:"
            + "accordionPanel:tabs:2:body:content:container:content:searchContainer:resultTable";

    protected static final String DATATABLE_PATH =
            "body:content:tabbedPanel:panel:container:content:searchContainer:resultTable";

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    protected void browsingToRelationshipType() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:typesLI:types");
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypes() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:typesLI:types");
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypeClasses() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:typesLI:types");
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        UTILITY_UI.getTester().assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToPlainSchemas() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:typesLI:types");
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
        UTILITY_UI.getTester().assertComponent(PLAIN_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToVirtualSchemas() {
        UTILITY_UI.getTester().clickLink("body:configurationLI:configurationUL:typesLI:types");
        UTILITY_UI.getTester().assertRenderedPage(Types.class);

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
        UTILITY_UI.getTester().assertComponent(VIRTUAL_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void createPlainSchema(final String key) {
        browsingToPlainSchemas();
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:container:content:add");

        UTILITY_UI.getTester().assertComponent(
                "body:content:tabbedPanel:panel:accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer",
                Modal.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:form:view:details:key:textField", key);
        formTester.setValue("content:form:view:details:type:dropDownChoiceField", "3");
        UTILITY_UI.getTester().executeAjaxEvent("body:content:tabbedPanel:panel:accordionPanel:tabs:0:"
                + "body:content:outerObjectsRepeater:0:outer:form:content:form:buttons:next", Constants.ON_CLICK);

        formTester = UTILITY_UI.getTester().newFormTester("body:content:tabbedPanel:panel:"
                + "accordionPanel:tabs:0:body:content:outerObjectsRepeater:0:outer:form");
        formTester.submit("content:form:buttons:finish");

        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    protected void createAnyTypeClassWithoutSchema(final String name) {
        browsingToAnyTypeClasses();

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");
        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", name);

        UTILITY_UI.getTester().clearFeedbackMessages();
        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().clearFeedbackMessages();
    }

    protected void createAnyType(final String name) {
        browsingToAnyTypes();

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");
        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeDetailsPanel:container:form:key:textField", name);

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().clearFeedbackMessages();
    }

    protected void createRelationshipType(final String name) {
        browsingToRelationshipType();

        UTILITY_UI.getTester().clickLink("body:content:tabbedPanel:panel:container:content:add");

        UTILITY_UI.getTester().assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer",
                Modal.class);

        final FormTester formTester = UTILITY_UI.getTester().newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:relationshipTypeDetails:container:form:key:textField", name);
        formTester.setValue(
                "content:relationshipTypeDetails:container:form:description:textField", "test relationshipType");

        UTILITY_UI.getTester().clickLink(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        UTILITY_UI.getTester().assertInfoMessages("Operation successfully executed");

        UTILITY_UI.getTester().clearFeedbackMessages();
        UTILITY_UI.getTester().assertRenderedPage(Types.class);
    }
}
