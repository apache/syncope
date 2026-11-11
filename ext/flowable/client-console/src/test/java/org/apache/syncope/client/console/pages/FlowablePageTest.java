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
package org.apache.syncope.client.console.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.AbstractFlowableConsoleTest;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.junit.jupiter.api.Test;

class FlowablePageTest extends AbstractFlowableConsoleTest {

    @Test
    void renderFlowablePage() {
        BpmnProcess process = new BpmnProcess();
        process.setKey("userRequest");
        process.setName("User request");
        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>(List.of(process)));

        TESTER.startPage(Flowable.class);

        TESTER.assertRenderedPage(Flowable.class);
        assertNotNull(TESTER.getComponentFromLastRenderedPage("body:content:bpmnProcessesPanel"));
        TESTER.assertContains("userRequest");
        verify(bpmnProcessRestClient, atLeastOnce()).getDefinitions();
    }

    @Test
    void modelerPopupPageRedirectsToModeler() {
        TESTER.startPage(FlowableModelerPopupPage.class);

        TESTER.assertRenderedPage(FlowableModelerPopupPage.class);
        assertTrue(TESTER.getLastResponseAsString().contains("flowable-modeler/modeler.html"));
    }

    @Test
    void renderUserRequestsPage() {
        TESTER.startPage(UserRequests.class);

        TESTER.assertRenderedPage(UserRequests.class);
        WebMarkupContainer content = (WebMarkupContainer) TESTER.getComponentFromLastRenderedPage("body:content");
        assertEquals("userRequests", content.getMarkupId());

        verify(userRequestRestClient, atLeastOnce()).countForms(null);
    }
}
