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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpmnProcessRestClientTest {

    private BpmnProcessService service;

    private BpmnProcessRestClient client;

    @BeforeEach
    void setup() {
        service = mock(BpmnProcessService.class);
        client = new TestBpmnProcessRestClient(service);
    }

    @Test
    void getDefinitions() {
        List<BpmnProcess> definitions = List.of(new BpmnProcess());
        when(service.list()).thenReturn(definitions);

        assertSame(definitions, client.getDefinitions());
    }

    @Test
    void deleteDefinition() {
        client.deleteDefinition("key");

        verify(service).delete("key");
    }

    private static final class TestBpmnProcessRestClient extends BpmnProcessRestClient {

        private static final long serialVersionUID = 1L;

        private final BpmnProcessService service;

        private TestBpmnProcessRestClient(final BpmnProcessService service) {
            this.service = service;
        }

        @Override
        protected <T> T getService(final Class<T> serviceClass) {
            return serviceClass.cast(service);
        }
    }
}
