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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.OIDCC4UIProviderLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OIDCC4UIProviderServiceImplTest {

    private static final URI BASE = URI.create("http://localhost:8080/oidcc4ui/providers");

    private OIDCC4UIProviderLogic logic;

    private OIDCC4UIProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(OIDCC4UIProviderLogic.class);

        service = new OIDCC4UIProviderServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromUri(BASE));
        service.uriInfo = uriInfo;
    }

    private static OIDCC4UIProviderTO provider() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");
        opTO.setName("op");
        return opTO;
    }

    @Test
    void createReturnsCreatedResponse() {
        OIDCC4UIProviderTO opTO = provider();
        when(logic.create(opTO)).thenReturn("key");

        Response response = service.create(opTO);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(UriBuilder.fromUri(BASE).path("key").build(), response.getLocation());
        assertEquals("key", response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        verify(logic).create(opTO);
    }

    @Test
    void createFromDiscoveryReturnsCreatedResponse() {
        OIDCC4UIProviderTO opTO = provider();
        when(logic.createFromDiscovery(opTO)).thenReturn("key");

        Response response = service.createFromDiscovery(opTO);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(UriBuilder.fromUri(BASE).path("key").build(), response.getLocation());
        assertEquals("key", response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        verify(logic).createFromDiscovery(opTO);
    }

    @Test
    void listDelegatesToLogic() {
        List<OIDCC4UIProviderTO> providers = List.of(provider());
        when(logic.list()).thenReturn(providers);

        assertSame(providers, service.list());
    }

    @Test
    void readDelegatesToLogic() {
        OIDCC4UIProviderTO opTO = provider();
        when(logic.read("key")).thenReturn(opTO);

        assertSame(opTO, service.read("key"));
    }

    @Test
    void updateDelegatesToLogic() {
        OIDCC4UIProviderTO opTO = provider();

        service.update(opTO);

        verify(logic).update(opTO);
    }

    @Test
    void deleteDelegatesToLogic() {
        service.delete("key");

        verify(logic).delete("key");
    }
}
