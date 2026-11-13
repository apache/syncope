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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;
import org.apache.syncope.core.logic.AccessTokenLogic;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;
class AccessTokenServiceTest {

    private AccessTokenLogic logic;
    private AccessTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(AccessTokenLogic.class);
        service = new AccessTokenServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(URI.create("http://localhost/"));
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @Test
    void login() {
        AccessTokenDataBinder.AccessTokenInfo info = mock(AccessTokenDataBinder.AccessTokenInfo.class);
        when(info.jwt()).thenReturn("token");
        when(info.expiration()).thenReturn(OffsetDateTime.now());
        when(logic.login()).thenReturn(info);

        Response response = service.login();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void refresh() {
        AccessTokenDataBinder.AccessTokenInfo info = mock(AccessTokenDataBinder.AccessTokenInfo.class);
        when(info.jwt()).thenReturn("token");
        when(info.expiration()).thenReturn(OffsetDateTime.now());
        when(logic.refresh()).thenReturn(info);

        Response response = service.refresh();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void logout() {
        doNothing().when(logic).logout();

        service.logout();

        verify(logic).logout();
    }

    @SuppressWarnings("unchecked")
    @Test
    void list() {
        AccessTokenQuery query = new AccessTokenQuery();
        query.setPage(1);
        query.setSize(10);

        Page<AccessTokenTO> page = mock(Page.class);
        when(logic.list(any())).thenReturn(page);

        PagedResult<AccessTokenTO> result = service.list(query);

        assertNotNull(result);
        verify(logic).list(any());
    }

    @Test
    void delete() {
        doNothing().when(logic).delete("key");

        service.delete("key");

        verify(logic).delete("key");
    }
}
