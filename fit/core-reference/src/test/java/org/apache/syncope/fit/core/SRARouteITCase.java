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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class SRARouteITCase extends AbstractITCase {

    @Test
    public void read() {
        SRARouteTO route = SRA_ROUTE_SERVICE.read("ec7bada2-3dd6-460c-8441-65521d005ffa");
        assertNotNull(route);
        assertEquals(1, route.getPredicates().size());

        try {
            SRA_ROUTE_SERVICE.read(UUID.randomUUID().toString());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void findAll() {
        List<SRARouteTO> routes = SRA_ROUTE_SERVICE.list();
        assertNotNull(routes);
        assertFalse(routes.isEmpty());
    }

    @Test
    public void createUpdateDelete() {
        SRARouteTO route = new SRARouteTO();
        route.setName("just for test");
        route.setTarget(URI.create("http://localhost:80"));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.METHOD).args(HttpMethod.GET).build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_HEADER).args("X-Request-Foo, Bar").build());

        int beforeCount = SRA_ROUTE_SERVICE.list().size();

        Response response = SRA_ROUTE_SERVICE.create(route);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        route = getObject(response.getLocation(), SRARouteService.class, SRARouteTO.class);
        assertNotNull(route);
        assertNotNull(route.getKey());

        int afterCount = SRA_ROUTE_SERVICE.list().size();
        assertEquals(afterCount, beforeCount + 1);

        SRA_ROUTE_SERVICE.delete(route.getKey());

        try {
            SRA_ROUTE_SERVICE.read(route.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        int endCount = SRA_ROUTE_SERVICE.list().size();
        assertEquals(endCount, beforeCount);
    }

    @Test
    public void exceptions() {
        SRARouteTO route = new SRARouteTO();
        try {
            SRA_ROUTE_SERVICE.create(route);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        route.setName("createException");
        try {
            SRA_ROUTE_SERVICE.create(route);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        route.setTarget(URI.create("http://localhost:80"));
        Response response = SRA_ROUTE_SERVICE.create(route);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        try {
            SRA_ROUTE_SERVICE.create(route);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        route.setKey(UUID.randomUUID().toString());
        try {
            SRA_ROUTE_SERVICE.update(route);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        try {
            SRA_ROUTE_SERVICE.delete(route.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }
}
