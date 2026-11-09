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
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.core.logic.OIDCOpEntityLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OIDCOpEntityServiceTest {

    private OIDCOpEntityLogic logic;

    private OIDCOpEntityServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(OIDCOpEntityLogic.class);
        service = new OIDCOpEntityServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost/"));
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @Test
    void get() {
        OIDCOpEntityTO to = new OIDCOpEntityTO();
        when(logic.get()).thenReturn(to);
        assertSame(to, service.get());
    }

    @Test
    void set() {
        OIDCOpEntityTO to = new OIDCOpEntityTO();
        service.set(to);
        verify(logic).set(to);
    }

    @Test
    void generate() {
        OIDCOpEntityTO to = new OIDCOpEntityTO();
        when(logic.generate("keyId", "oct", 256)).thenReturn(to);
        Response response = service.generate("keyId", "oct", 256);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertSame(to, response.getEntity());
    }

    @Test
    void delete() {
        service.delete();
        verify(logic).delete();
    }
}
