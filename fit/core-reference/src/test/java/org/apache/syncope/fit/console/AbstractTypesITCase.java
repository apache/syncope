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
        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:0:link");
        TESTER.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypes() {
        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        TESTER.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToAnyTypeClasses() {
        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:2:link");
        TESTER.assertComponent(DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToPlainSchemas() {
        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
        TESTER.assertComponent(PLAIN_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void browsingToVirtualSchemas() {
        TESTER.clickLink("body:configurationLI:configurationUL:typesLI:types", false);
        TESTER.assertRenderedPage(Types.class);

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:3:link");
        TESTER.assertComponent(VIRTUAL_DATATABLE_PATH + ":tablePanel:groupForm:checkgroup:dataTable",
                AjaxFallbackDataTable.class);
    }

    protected void createAnyTypeClassWithoutSchema(final String name) {
        browsingToAnyTypeClasses();

        TESTER.clickLink("body:content:tabbedPanel:panel:container:content:add");
        TESTER.assertComponent("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer", Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:form");
        formTester.setValue("content:anyTypeClassDetailsPanel:form:key:textField", name);

        TESTER.clearFeedbackMessages();
        TESTER.clickLink("body:content:tabbedPanel:panel:outerObjectsRepeater:0:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
        TESTER.clearFeedbackMessages();
    }
}
