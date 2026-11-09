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
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.logic.SecurityQuestionLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
class SecurityQuestionServiceTest {

    private SecurityQuestionLogic logic;
    private SecurityQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(SecurityQuestionLogic.class);
        service = new SecurityQuestionServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost/"));
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @Test
    void list() {
        when(logic.list()).thenReturn(List.of());
        assertEquals(List.of(), service.list());
    }

    @Test
    void read() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");
        when(logic.read("key")).thenReturn(to);
        assertSame(to, service.read("key"));
    }

    @Test
    void create() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");
        when(logic.create(to)).thenReturn(to);
        Response response = service.create(to);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void update() {
        SecurityQuestionTO to = new SecurityQuestionTO();
        to.setKey("key");
        when(logic.update(to)).thenReturn(to);
        service.update(to);
        verify(logic).update(to);
    }

    @Test
    void delete() {
        when(logic.delete("key")).thenReturn(null);
        service.delete("key");
        verify(logic).delete("key");
    }
}
