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
import org.apache.syncope.client.console.pages.Realms;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class DisplayAttributesITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        wicketTester.clickLink("body:realmsLI:realms");
        wicketTester.assertRenderedPage(Realms.class);
    }

    @Test
    public void read() {
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");
        wicketTester.clickLink("body:content:body:tabbedPanel:panel:"
                + "searchResult:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:4:header:label:panelChangeView:changeViewLink");

        wicketTester.assertComponent(
                "body:content:body:tabbedPanel:panel:searchResult:outerObjectsRepeater:2:outer", Modal.class);
    }

    @Test
    public void set() {
        wicketTester.clickLink("body:content:body:tabbedPanel:tabs-container:tabs:3:link");
        wicketTester.clickLink("body:content:body:tabbedPanel:panel:"
                + "searchResult:container:content:searchContainer:resultTable:"
                + "tablePanel:groupForm:checkgroup:dataTable:topToolbars:"
                + "toolbars:1:headers:4:header:label:panelChangeView:changeViewLink");

        wicketTester.assertComponent(
                "body:content:body:tabbedPanel:panel:searchResult:outerObjectsRepeater:2:outer", Modal.class);

        final FormTester formTester = wicketTester.newFormTester(
                "body:content:body:tabbedPanel:panel:searchResult:outerObjectsRepeater:2:outer:form");

        formTester.setValue("content:container:details:paletteField:recorder", "status");

        wicketTester.clickLink(
                "body:content:body:tabbedPanel:panel:searchResult:outerObjectsRepeater:2:outer:dialog:footer:"
                + "inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
    }
}
