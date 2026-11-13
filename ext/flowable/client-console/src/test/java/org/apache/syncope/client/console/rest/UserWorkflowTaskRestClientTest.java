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
package org.apache.syncope.client.console.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.service.UserWorkflowTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserWorkflowTaskRestClientTest {

    private UserWorkflowTaskService service;

    private UserWorkflowTaskRestClient client;

    @BeforeEach
    void setup() {
        service = mock(UserWorkflowTaskService.class);
        client = new TestUserWorkflowTaskRestClient(service);
    }

    @Test
    void executeNextTask() {
        UserTO userTO = new UserTO();
        userTO.setKey("userKey");

        UserTO updated = new UserTO();
        updated.setKey("userKey");
        when(service.executeNextTask(any())).thenReturn(updated);

        assertSame(updated, client.executeNextTask(userTO));

        ArgumentCaptor<WorkflowTaskExecInput> input = ArgumentCaptor.forClass(WorkflowTaskExecInput.class);
        verify(service).executeNextTask(input.capture());
        assertEquals("userKey", input.getValue().getUserKey());
    }

    private static final class TestUserWorkflowTaskRestClient extends UserWorkflowTaskRestClient {

        private static final long serialVersionUID = 1L;

        private final UserWorkflowTaskService service;

        private TestUserWorkflowTaskRestClient(final UserWorkflowTaskService service) {
            this.service = service;
        }

        @Override
        protected <T> T getService(final Class<T> serviceClass) {
            return serviceClass.cast(service);
        }
    }
}
