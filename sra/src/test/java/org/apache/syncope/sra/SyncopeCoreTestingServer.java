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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SyncopeCoreTestingServer implements ApplicationListener<ContextRefreshedEvent> {

    private static final int PORT = 9999;

    public static final String ADDRESS = "http://localhost:" + PORT + "/syncope/rest";

    public static final Map<String, SRARouteTO> ROUTES = new ConcurrentHashMap<>();

    @Autowired
    private RouteRefresher routeRefresher;

    @Autowired
    private ServiceOps serviceOps;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (AbstractTest.available(PORT)) {
            // 1. start (mocked) Core as embedded CXF
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setAddress(ADDRESS);
            sf.setResourceClasses(SRARouteService.class);
            sf.setResourceProvider(SRARouteService.class,
                    new SingletonResourceProvider(new StubSRARouteService(), true));
            sf.setProviders(List.of(new JacksonJsonProvider(JsonMapper.builder().findAndAddModules().build())));
            sf.create();

            // 2. register Core in Keymaster
            NetworkService core = new NetworkService();
            core.setType(NetworkService.Type.CORE);
            core.setAddress(ADDRESS);
            serviceOps.register(core);
        }
    }

    public class StubSRARouteService implements SRARouteService {

        @Override
        public List<SRARouteTO> list() {
            return ROUTES.values().stream().
                    sorted(Comparator.comparing(SRARouteTO::getKey)).
                    toList();
        }

        @Override
        public Response create(final SRARouteTO routeTO) {
            ROUTES.putIfAbsent(routeTO.getKey(), routeTO);
            return Response.noContent().build();
        }

        @Override
        public SRARouteTO read(final String key) {
            SRARouteTO route = ROUTES.get(key);
            if (route == null) {
                throw new NotFoundException();
            }
            return route;
        }

        @Override
        public void update(final SRARouteTO routeTO) {
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
