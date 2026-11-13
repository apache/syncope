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
package org.apache.syncope.client.enduser.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.enduser.AbstractFlowableEnduserTest;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.to.UserRequest;
import org.junit.jupiter.api.Test;

class FlowablePageTest extends AbstractFlowableEnduserTest {

    private static final String CONTENT = "body:contentWrapper:content";

    private BpmnProcess process(final String key, final boolean userWorkflow) {
        BpmnProcess process = new BpmnProcess();
        process.setKey(key);
        process.setName(key);
        process.setUserWorkflow(userWorkflow);
        return process;
    }

    @Test
    void renderPage() {
        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>(List.of(
                process("userWorkflow", true),
                process("regular", false))));

        TESTER.startPage(Flowable.class);

        TESTER.assertRenderedPage(Flowable.class);
        assertNotNull(TESTER.getComponentFromLastRenderedPage(CONTENT + ":bpmnProcesses"));
        TESTER.assertComponent(CONTENT + ":bpmnProcesses", AjaxTextFieldPanel.class);
        TESTER.assertDisabled(CONTENT + ":start");

        verify(bpmnProcessRestClient).getDefinitions();
        verify(userRequestRestClient).countRequests();
    }

    @Test
    void activeRequestsListed() {
        UserRequest request = new UserRequest();
        request.setExecutionId("executionId");
        request.setActivityId("activityId");
        request.setStartTime(Date.from(OffsetDateTime.parse("2024-01-01T10:15:30Z").toInstant()));
        request.setTaskId("task");
        request.setHasForm(false);

        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>());
        when(userRequestRestClient.countRequests()).thenReturn(1L);
        when(userRequestRestClient.listRequests(anyInt(), anyInt(), any())).
                thenReturn(List.of(request));

        TESTER.startPage(Flowable.class);

        TESTER.assertRenderedPage(Flowable.class);
        TESTER.assertContains("executionId");
        TESTER.assertContains("activityId");

        verify(userRequestRestClient, atLeastOnce()).
                listRequests(anyInt(), anyInt(), any());
    }
}
