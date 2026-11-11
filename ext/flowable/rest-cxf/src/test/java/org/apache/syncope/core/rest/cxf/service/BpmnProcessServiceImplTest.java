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
package org.apache.syncope.core.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.BpmnProcessLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpmnProcessServiceImplTest {

    private BpmnProcessLogic logic;

    private MessageContext messageContext;

    private HttpHeaders httpHeaders;

    private BpmnProcessServiceImpl service;

    @BeforeEach
    void setup() {
        logic = mock(BpmnProcessLogic.class);
        messageContext = mock(MessageContext.class);
        httpHeaders = mock(HttpHeaders.class);
        when(messageContext.getHttpHeaders()).thenReturn(httpHeaders);

        service = new BpmnProcessServiceImpl(logic);
        service.messageContext = messageContext;
    }

    @Test
    void list() {
        List<BpmnProcess> processes = List.of(new BpmnProcess());
        when(logic.list()).thenReturn(processes);

        assertSame(processes, service.list());
    }

    @Test
    void getAsJson() throws IOException {
        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_JSON_TYPE));

        Response response = service.get("key");

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(os);

        verify(logic).exportDefinition("key", BpmnProcessFormat.JSON, os);
    }

    @Test
    void getAsXml() throws IOException {
        when(httpHeaders.getAcceptableMediaTypes()).thenReturn(List.of(MediaType.APPLICATION_XML_TYPE));

        Response response = service.get("key");

        assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(os);

        verify(logic).exportDefinition("key", BpmnProcessFormat.XML, os);
    }

    @Test
    void setAsJson() {
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);

        service.set("key", "definition");

        verify(logic).importDefinition("key", BpmnProcessFormat.JSON, "definition");
    }

    @Test
    void setAsXml() {
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);

        service.set("key", "definition");

        verify(logic).importDefinition("key", BpmnProcessFormat.XML, "definition");
    }

    @Test
    void exportDiagram() throws IOException {
        Response response = service.exportDiagram("key");

        assertEquals(MediaType.valueOf(RESTHeaders.MEDIATYPE_IMAGE_PNG), response.getMediaType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(os);

        verify(logic).exportDiagram("key", os);
    }

    @Test
    void delete() {
        service.delete("key");

        verify(logic).delete("key");
    }
}
