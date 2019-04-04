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

import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FilterFactory;
import org.apache.syncope.common.lib.types.GatewayFilter;
import org.apache.syncope.common.lib.types.GatewayPredicate;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;
import org.apache.syncope.common.lib.types.PredicateFactory;
import org.apache.syncope.common.rest.api.service.GatewayRouteService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class GatewayRouteITCase extends AbstractITCase {

    @Test
    public void read() {
        GatewayRouteTO route = gatewayRouteService.read("ec7bada2-3dd6-460c-8441-65521d005ffa");
        assertNotNull(route);
        assertEquals(GatewayRouteStatus.PUBLISHED, route.getStatus());
        assertEquals(1, route.getPredicates().size());

        try {
            gatewayRouteService.read(UUID.randomUUID().toString());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void findAll() {
        List<GatewayRouteTO> routes = gatewayRouteService.list();
        assertNotNull(routes);
        assertFalse(routes.isEmpty());
    }

    @Test
    public void createUpdateDelete() {
        GatewayRouteTO route = new GatewayRouteTO();
        route.setName("just for test");
        route.setTarget(URI.create("http://httpbin.org:80"));
        route.getPredicates().add(new GatewayPredicate.Builder().
                factory(PredicateFactory.METHOD).args(HttpMethod.GET).build());
        route.getFilters().add(new GatewayFilter.Builder().
                factory(FilterFactory.ADD_REQUEST_HEADER).args("X-Request-Foo, Bar").build());
        route.setStatus(GatewayRouteStatus.DRAFT);

        int beforeCount = gatewayRouteService.list().size();

        Response response = gatewayRouteService.create(route);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        route = getObject(response.getLocation(), GatewayRouteService.class, GatewayRouteTO.class);
        assertNotNull(route);
        assertNotNull(route.getKey());

        int afterCount = gatewayRouteService.list().size();
        assertEquals(afterCount, beforeCount + 1);

        route.setStatus(GatewayRouteStatus.STAGING);
        gatewayRouteService.update(route);
        route = gatewayRouteService.read(route.getKey());
        assertEquals(GatewayRouteStatus.STAGING, route.getStatus());

        gatewayRouteService.delete(route.getKey());

        try {
            gatewayRouteService.read(route.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        int endCount = gatewayRouteService.list().size();
        assertEquals(endCount, beforeCount);
    }
}
