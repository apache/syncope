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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.AbstractFlowableConsoleTest;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.wicket.mock.MockRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.junit.jupiter.api.Test;

class BpmnProcessGETResourceTest extends AbstractFlowableConsoleTest {

    private static final String DEFINITION = "{\"process\":\"definition\"}";

    private ResourceReference reference(final IResource resource) {
        return new ResourceReference("bpmnProcessGET") {

            private static final long serialVersionUID = 1L;

            @Override
            public IResource getResource() {
                return resource;
            }
        };
    }

    private BpmnProcess process() {
        BpmnProcess process = new BpmnProcess();
        process.setKey("process");
        process.setModelId("model");
        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>(List.of(process)));
        when(bpmnProcessRestClient.getDefinition(MediaType.APPLICATION_JSON_TYPE, "process")).thenReturn(
                new ByteArrayInputStream(DEFINITION.getBytes(StandardCharsets.UTF_8)));
        return process;
    }

    @Test
    void definitionWrittenToResponse() {
        process();

        TESTER.getRequest().setParameter(Constants.MODEL_ID_PARAM, "model");
        TESTER.startResourceReference(
                reference(new BpmnProcessGETResource(bpmnProcessRestClient)),
                new PageParameters());

        assertEquals(200, TESTER.getLastResponse().getStatus());
        assertEquals(DEFINITION, TESTER.getLastResponseAsString());
        verify(bpmnProcessRestClient).getDefinition(MediaType.APPLICATION_JSON_TYPE, "process");
    }

    @Test
    void unknownModelId() {
        when(bpmnProcessRestClient.getDefinitions()).thenReturn(new ArrayList<>());

        MockRequestParameters parameters = new MockRequestParameters();
        parameters.setParameterValue(Constants.MODEL_ID_PARAM, "unknown");

        Request request = mock(Request.class);
        when(request.getQueryParameters()).thenReturn(parameters);

        BpmnProcessGETResource resource = new BpmnProcessGETResource(bpmnProcessRestClient);
        assertThrows(NotFoundException.class,
                () -> resource.respond(new IResource.Attributes(request, mock(Response.class))));
    }
}
