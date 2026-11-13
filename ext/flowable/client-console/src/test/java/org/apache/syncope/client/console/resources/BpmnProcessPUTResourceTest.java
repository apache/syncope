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
package org.apache.syncope.client.console.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.AbstractFlowableConsoleTest;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpmnProcessPUTResourceTest extends AbstractFlowableConsoleTest {

    private static final String DEFINITION = "{\"process\":\"updated\"}";

    private BpmnProcess process;

    private ResourceReference reference;

    @BeforeEach
    void setup() {
        process = new BpmnProcess();
        process.setKey("process");
        process.setModelId("model");
        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>(List.of(process)));

        reference = new ResourceReference("bpmnProcessPUT") {

            private static final long serialVersionUID = 1L;

            @Override
            public IResource getResource() {
                return new BpmnProcessPUTResource(bpmnProcessRestClient);
            }
        };
    }

    @Test
    void definitionUpdated() {
        TESTER.getRequest().setParameter(Constants.MODEL_ID_PARAM, "model");
        TESTER.getRequest().getPostParameters().setParameterValue("json_xml", DEFINITION);

        TESTER.startResourceReference(reference, new PageParameters());

        assertEquals(204, TESTER.getLastResponse().getStatus());
        verify(bpmnProcessRestClient).setDefinition(MediaType.APPLICATION_JSON_TYPE, "process", DEFINITION);
    }

    @Test
    void missingDefinition() {
        TESTER.getRequest().setParameter(Constants.MODEL_ID_PARAM, "model");

        TESTER.startResourceReference(reference, new PageParameters());

        assertEquals(400, TESTER.getLastResponse().getStatus());
    }

    @Test
    void failureWhileUpdating() {
        doThrow(SyncopeClientException.build(ClientExceptionType.NotFound)).
                when(bpmnProcessRestClient).setDefinition(
                        eq(MediaType.APPLICATION_JSON_TYPE), anyString(), anyString());

        TESTER.getRequest().setParameter(Constants.MODEL_ID_PARAM, "model");
        TESTER.getRequest().getPostParameters().setParameterValue("json_xml", DEFINITION);

        TESTER.startResourceReference(reference, new PageParameters());

        assertEquals(400, TESTER.getLastResponse().getStatus());
        verify(bpmnProcessRestClient).setDefinition(any(), anyString(), anyString());
    }
}
