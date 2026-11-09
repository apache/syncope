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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AuthProfileQuery;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

class AuthProfileServiceTest {

    private AuthProfileLogic logic;

    private AuthProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(AuthProfileLogic.class);
        service = new AuthProfileServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost/"));
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @SuppressWarnings("unchecked")
    @Test
    void search() {
        AuthProfileQuery query = new AuthProfileQuery();
        query.setPage(1);
        query.setSize(10);
        query.setKeyword("foo*");

        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");

        Page<AuthProfileTO> page = mock(Page.class);
        when(page.get()).thenReturn(Stream.of(to));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(1L);
        when(logic.search(eq("foo%"), any())).thenReturn(page);

        PagedResult<AuthProfileTO> result = service.search(query);

        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(to), result.getResult());
        verify(logic).search(eq("foo%"), any());
    }

    @Test
    void read() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");
        when(logic.read("key")).thenReturn(to);
        assertSame(to, service.read("key"));
    }

    @Test
    void create() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");
        when(logic.create(to)).thenReturn(to);
        Response response = service.create(to);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    void update() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");
        service.update(to);
        verify(logic).update(to);
    }

    @Test
    void delete() {
        service.delete("key");
        verify(logic).delete("key");
    }
}
