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
package org.apache.syncope.core.keymaster.rest.cxf.service;

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
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.DomainLogic;
import org.junit.jupiter.api.Test;

class DomainServiceImplTest {

    private final DomainLogic logic = mock(DomainLogic.class);

    private final UriInfo uriInfo = mock(UriInfo.class);

    private final DomainServiceImpl service = new DomainServiceImpl(logic);

    {
        service.uriInfo = uriInfo;
    }

    @Test
    void list() {
        List<Domain> expected = List.of(mock(Domain.class));
        when(logic.list()).thenReturn(expected);

        assertSame(expected, service.list());
        verify(logic).list();
    }

    @Test
    void read() {
        String key = "domain";
        Domain expected = mock(Domain.class);
        when(logic.read(key)).thenReturn(expected);

        assertSame(expected, service.read(key));
        verify(logic).read(key);
    }

    @Test
    void create() {
        Domain domain = mock(Domain.class);
        Domain created = mock(Domain.class);
        when(created.getKey()).thenReturn("domain");
        when(logic.create(domain)).thenReturn(created);

        UriBuilder builder = mock(UriBuilder.class);
        when(builder.path("domain")).thenReturn(builder);
        when(builder.build()).thenReturn(URI.create("/keymaster/domain"));
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(builder);

        Response response = service.create(domain);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("domain", response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        verify(logic).create(domain);
    }

    @Test
    void deployed() {
        String key = "domain";

        service.deployed(key);

        verify(logic).deployed(key);
    }

    @Test
    void changeAdminPassword() {
        String key = "domain";
        String password = "new-password";

        service.changeAdminPassword(key, password, CipherAlgorithm.SHA512);

        verify(logic).changeAdminPassword(key, password, CipherAlgorithm.SHA512);
    }

    @Test
    void setAdminMfaSecret() {
        String key = "domain";
        String secret = "secret";

        service.setAdminMfaSecret(key, secret);

        verify(logic).setAdminMfaSecret(key, secret);
    }

    @Test
    void adjustPoolSize() {
        String key = "domain";

        service.adjustPoolSize(key, 20, 5);

        verify(logic).adjustPoolSize(key, 20, 5);
    }

    @Test
    void delete() {
        String key = "domain";

        service.delete(key);

        verify(logic).delete(key);
    }
}
