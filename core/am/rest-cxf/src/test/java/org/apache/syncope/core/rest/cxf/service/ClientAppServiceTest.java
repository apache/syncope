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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.core.logic.ClientAppLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ClientAppServiceTest {

    private ClientAppLogic logic;

    private ClientAppServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(ClientAppLogic.class);
        service = new ClientAppServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost/"));
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @Test
    void create() {
        OIDCRPClientAppTO to = new OIDCRPClientAppTO();
        to.setKey("key");
        when(logic.create(ClientAppType.OIDCRP, to)).thenReturn(to);
        Response response = service.create(ClientAppType.OIDCRP, to);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void list() {
        when(logic.list(ClientAppType.OIDCRP)).thenReturn(List.of());
        assertEquals(List.of(), service.list(ClientAppType.OIDCRP));
    }

    @Test
    void read() {
        OIDCRPClientAppTO to = new OIDCRPClientAppTO();
        to.setKey("key");
        when(logic.read(ClientAppType.OIDCRP, "key")).thenReturn(to);
        assertSame(to, service.read(ClientAppType.OIDCRP, "key"));
    }

    @Test
    void update() {
        OIDCRPClientAppTO to = new OIDCRPClientAppTO();
        to.setKey("key");
        service.update(ClientAppType.OIDCRP, to);
        verify(logic).update(ClientAppType.OIDCRP, to);
    }

    @Test
    void delete() {
        service.delete(ClientAppType.OIDCRP, "key");
        verify(logic).delete(ClientAppType.OIDCRP, "key");
    }
}
