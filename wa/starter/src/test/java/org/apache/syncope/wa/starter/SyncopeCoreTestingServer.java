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
package org.apache.syncope.wa.starter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SyncopeCoreTestingServer implements ApplicationListener<ContextRefreshedEvent> {

    public static final String ADDRESS = "http://localhost:9080/syncope/rest";

    public static final List<WAClientApp> APPS = new ArrayList<>();

    @Autowired
    private ServiceOps serviceOps;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        synchronized (serviceOps) {
            if (serviceOps.list(NetworkService.Type.CORE).isEmpty()) {
                // 1. start (mocked) Core as embedded CXF
                JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
                sf.setAddress(ADDRESS);
                sf.setResourceClasses(WAClientAppService.class, GoogleMfaAuthTokenService.class);
                sf.setResourceProvider(
                        WAClientAppService.class,
                        new SingletonResourceProvider(new StubWAClientAppService(), true));
                sf.setResourceProvider(
                    GoogleMfaAuthTokenService.class,
                    new SingletonResourceProvider(new StubGoogleMfaAuthTokenService(), true));
                sf.setProviders(List.of(new JacksonJsonProvider()));
                sf.create();

                // 2. register Core in Keymaster
                NetworkService core = new NetworkService();
                core.setType(NetworkService.Type.CORE);
                core.setAddress(ADDRESS);
                serviceOps.register(core);
            }
        }
    }

    public static class StubGoogleMfaAuthTokenService implements GoogleMfaAuthTokenService {
        private final List<GoogleMfaAuthTokenTO> tokens = new ArrayList<>();

        @Override
        public Response deleteTokensByDate(@NotNull final Date expirationDate) {
            tokens.removeIf(token -> token.getIssuedDate().compareTo(expirationDate) >= 0);
            return Response.ok().build();
        }

        @Override
        public Response deleteToken(@NotNull final String user, @NotNull final Integer token) {
            tokens.removeIf(to -> to.getToken().equals(token) && to.getUser().equalsIgnoreCase(user));
            return Response.ok().build();
        }

        @Override
        public Response deleteTokensFor(@NotNull final String user) {
            tokens.removeIf(to -> to.getUser().equalsIgnoreCase(user));
            return Response.ok().build();
        }

        @Override
        public Response deleteToken(@NotNull final Integer token) {
            tokens.removeIf(to -> to.getToken().equals(token));
            return Response.ok().build();
        }

        @Override
        public Response deleteTokens() {
            tokens.clear();
            return Response.ok().build();
        }

        @Override
        public Response save(@NotNull final GoogleMfaAuthTokenTO tokenTO) {
            tokenTO.setKey(UUID.randomUUID().toString());
            tokens.add(tokenTO);
            return Response.ok().build();
        }

        @Override
        public GoogleMfaAuthTokenTO findTokenFor(@NotNull final String user, @NotNull final Integer token) {
            return tokens.stream()
                .filter(to -> to.getToken().equals(token) && to.getUser().equalsIgnoreCase(user))
                .findFirst().get();
        }

        @Override
        public List<GoogleMfaAuthTokenTO> findTokensFor(@NotNull final String user) {
            return tokens.stream()
                .filter(to -> to.getUser().equalsIgnoreCase(user))
                .collect(Collectors.toList());
        }

        @Override
        public GoogleMfaAuthTokenTO findTokenFor(@NotNull final String key) {
            return tokens.stream()
                .filter(to -> to.getKey().equalsIgnoreCase(key))
                .findFirst().get();
        }

        @Override
        public long countTokensForUser(@NotNull final String user) {
            return tokens.stream()
                .filter(to -> to.getUser().equalsIgnoreCase(user))
                .count();
        }

        @Override
        public long countTokens() {
            return tokens.size();
        }
    }

    public static class StubWAClientAppService implements WAClientAppService {

        @Override
        public List<WAClientApp> list() {
            return APPS;
        }

        @Override
        public WAClientApp read(final Long clientAppId, final ClientAppType type) {
            return APPS.stream().filter(app -> Objects.equals(clientAppId, app.getClientAppTO().getClientAppId())).
                findFirst().orElseThrow(() -> new NotFoundException("ClientApp with clientId " + clientAppId));
        }

        @Override
        public WAClientApp read(final String name, final ClientAppType type) {
            return APPS.stream().filter(app -> Objects.equals(name, app.getClientAppTO().getName())).
                findFirst().orElseThrow(() -> new NotFoundException("ClientApp with name " + name));
        }
    }
}
