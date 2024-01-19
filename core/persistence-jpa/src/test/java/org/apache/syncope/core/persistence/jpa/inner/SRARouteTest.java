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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.HttpMethod;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.syncope.core.persistence.api.dao.SRARouteDAO;
import org.apache.syncope.core.persistence.api.entity.SRARoute;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SRARouteTest extends AbstractTest {

    @Autowired
    private SRARouteDAO routeDAO;

    @Test
    public void find() {
        SRARoute route = routeDAO.findById("ec7bada2-3dd6-460c-8441-65521d005ffa").orElseThrow();
        assertEquals(1, route.getPredicates().size());

        assertTrue(routeDAO.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    public void findAll() {
        List<? extends SRARoute> routes = routeDAO.findAll();
        assertNotNull(routes);
        assertEquals(1, routes.size());
    }

    @Test
    public void save() {
        SRARoute route = entityFactory.newEntity(SRARoute.class);
        route.setName("just for test");
        route.setTarget(URI.create("http://localhost:80"));
        route.setType(SRARouteType.PUBLIC);
        route.setPredicates(List.of(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.METHOD).args(HttpMethod.GET).build()));
        route.setFilters(List.of(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_HEADER).args("X-Request-Foo, Bar").build()));

        int beforeCount = routeDAO.findAll().size();

        route = routeDAO.save(route);
        assertNotNull(route);
        assertNotNull(route.getKey());

        int afterCount = routeDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void delete() {
        assertTrue(routeDAO.findById("ec7bada2-3dd6-460c-8441-65521d005ffa").isPresent());

        routeDAO.deleteById("ec7bada2-3dd6-460c-8441-65521d005ffa");

        assertTrue(routeDAO.findById("ec7bada2-3dd6-460c-8441-65521d005ffa").isEmpty());
    }
}
