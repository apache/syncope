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
package org.apache.syncope.sra;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.rest.api.service.GatewayRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class SyncopeSRATestCoreStartup extends SyncopeSRAStartStop
        implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    public static final String ADDRESS = "http://localhost:9080/syncope/rest";

    public static final Map<String, GatewayRouteTO> ROUTES = new ConcurrentHashMap<>();

    @Autowired
    private RouteRefresher routeRefresher;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        // 1. start (mocked) Core as embedded CXF
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(ADDRESS);
        sf.setResourceClasses(GatewayRouteService.class);
        sf.setResourceProvider(
                GatewayRouteService.class,
                new SingletonResourceProvider(new StubGatewayRouteService(), true));
        sf.setProviders(Collections.singletonList(new JacksonJsonProvider()));
        sf.create();

        // 2. register Core in Keymaster
        NetworkService core = new NetworkService();
        core.setType(NetworkService.Type.CORE);
        core.setAddress(SyncopeSRATestCoreStartup.ADDRESS);
        serviceOps.register(core);
    }

    public class StubGatewayRouteService implements GatewayRouteService {

        @Override
        public List<GatewayRouteTO> list() {
            return ROUTES.values().stream().
                    sorted(Comparator.comparing(GatewayRouteTO::getKey)).
                    collect(Collectors.toList());
        }

        @Override
        public Response create(final GatewayRouteTO routeTO) {
            ROUTES.putIfAbsent(routeTO.getKey(), routeTO);
            return Response.noContent().build();
        }

        @Override
        public GatewayRouteTO read(final String key) {
            GatewayRouteTO route = ROUTES.get(key);
            if (route == null) {
                throw new NotFoundException();
            }
            return route;
        }

        @Override
        public void update(final GatewayRouteTO routeTO) {
            read(routeTO.getKey());
            ROUTES.put(routeTO.getKey(), routeTO);
        }

        @Override
        public void delete(final String key) {
            ROUTES.remove(key);
        }

        @Override
        public void pushToSRA() {
            routeRefresher.refresh();
        }
    }
}
