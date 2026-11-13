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
package org.apache.syncope.client.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.EntityTag;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.junit.jupiter.api.Test;

class SyncopeClientTest {

    private static final String ADDRESS = "http://localhost";

    private static final String DOMAIN = "Master";

    private static final String JWT = "token";

    private SyncopeClient newClient() {
        return newClient(new JWTAuthenticationHandler(JWT));
    }

    private SyncopeClient newClient(final AuthenticationHandler authHandler) {
        JAXRSClientFactoryBean restClientFactory = new JAXRSClientFactoryBean();
        restClientFactory.setAddress(ADDRESS);
        restClientFactory.setHeaders(new HashMap<>());

        return new SyncopeClient(
                restClientFactory,
                new RestClientExceptionMapper(),
                authHandler,
                false,
                new HTTPClientPolicy(),
                null);
    }

    @Test
    void getRealmFiqlSearchConditionBuilder() {
        assertNotNull(SyncopeClient.getRealmFiqlSearchConditionBuilder());
    }

    @Test
    void getUserSearchConditionBuilder() {
        assertNotNull(SyncopeClient.getUserSearchConditionBuilder());
    }

    @Test
    void getGroupSearchConditionBuilder() {
        assertNotNull(SyncopeClient.getGroupSearchConditionBuilder());
    }

    @Test
    void getAnyObjectSearchConditionBuilder() {
        assertNotNull(SyncopeClient.getAnyObjectSearchConditionBuilder("anyType"));
    }

    @Test
    void getConnObjectTOFiqlSearchConditionBuilder() {
        assertNotNull(SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder());
    }

    @Test
    void getOrderByClauseBuilder() {
        assertNotNull(SyncopeClient.getOrderByClauseBuilder());
    }

    @Test
    void getAddress() {
        assertEquals(ADDRESS, newClient().getAddress());
    }

    @Test
    void getDomainDefaultMasterDomain() {
        assertEquals("Master", newClient().getDomain());
    }

    @Test
    void getDomainCustomDomain() {
        SyncopeClient client = newClient();
        client.restClientFactory.getHeaders().put(RESTHeaders.DOMAIN, List.of(DOMAIN));

        assertEquals(DOMAIN, client.getDomain());
    }

    @Test
    void jwtInfoEmpty() {
        JAXRSClientFactoryBean restClientFactory = new JAXRSClientFactoryBean();
        restClientFactory.setAddress(ADDRESS);
        restClientFactory.setHeaders(new HashMap<>());

        SyncopeClient client = new SyncopeClient(
                restClientFactory,
                new RestClientExceptionMapper(),
                new BasicAuthenticationHandler("user", "password"),
                false,
                new HTTPClientPolicy(),
                null);

        assertFalse(client.jwtInfo().isPresent());
    }

    @Test
    void jwtInfo() {
        Optional<SyncopeClient.JwtInfo> jwtInfo = newClient().jwtInfo();

        assertTrue(jwtInfo.isPresent());
        assertEquals(JWT, jwtInfo.get().value());
        assertNull(jwtInfo.get().expiration());
    }

    @Test
    void delegatedBy() {
        SyncopeClient client = newClient();

        assertSame(client, client.delegatedBy("delegating"));
        assertEquals(List.of("delegating"), client.restClientFactory.getHeaders().get(RESTHeaders.DELEGATED_BY));

        assertSame(client, client.delegatedBy(null));
        assertNull(client.restClientFactory.getHeaders().get(RESTHeaders.DELEGATED_BY));
    }

    @Test
    void header() {
        SyncopeService service = newClient().getService(SyncopeService.class);

        assertSame(service, SyncopeClient.header(service, "X-Custom", "value"));
    }

    @Test
    void ifMatch() {
        SyncopeService service = newClient().getService(SyncopeService.class);

        assertSame(service, SyncopeClient.ifMatch(service, new EntityTag("etag")));
    }

    @Test
    void ifNoneMatch() {
        SyncopeService service = newClient().getService(SyncopeService.class);

        assertSame(service, SyncopeClient.ifNoneMatch(service, new EntityTag("etag")));
    }

    @Test
    void getService() {
        assertInstanceOf(SyncopeService.class, newClient().getService(SyncopeService.class));
    }
}
