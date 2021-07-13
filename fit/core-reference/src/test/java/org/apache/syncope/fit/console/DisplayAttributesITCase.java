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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DisplayAttributesITCase extends AbstractConsoleITCase {

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.assertRenderedPage(Realms.class);
    }

    @Test
    public void readAndSet() {
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:3:link");
        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:container:content:"
                + "searchContainer:tablehandling:actionRepeater:1:action:action");

        TESTER.assertComponent(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:3:outer",
                Modal.class);

        FormTester formTester = TESTER.newFormTester(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:3:outer:form");

        formTester.setValue("content:container:details:paletteField:recorder", "status");

        TESTER.clickLink(
                "body:content:body:container:content:tabbedPanel:panel:searchResult:outerObjectsRepeater:"
                + "3:outer:dialog:footer:inputs:0:submit");
        assertSuccessMessage();
        TESTER.clearFeedbackMessages();
    }
}
