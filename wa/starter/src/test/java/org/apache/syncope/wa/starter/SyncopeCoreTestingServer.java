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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SyncopeCoreTestingServer implements ApplicationListener<ContextRefreshedEvent> {

    private static final String ADDRESS = "http://localhost:9080/syncope/rest";

    public static final List<WAClientApp> APPS = new ArrayList<>();

    @Autowired
    private ServiceOps serviceOps;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        synchronized (ADDRESS) {
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

        private final List<GoogleMfaAuthToken> tokens = new ArrayList<>();

        @Override
        public Response deleteTokensByDate(@NotNull final Date expirationDate) {
            tokens.removeIf(token -> token.getIssueDate().compareTo(expirationDate) >= 0);
            return Response.noContent().build();
        }

        @Override
        public Response deleteToken(@NotNull final String owner, @NotNull final Integer token) {
            tokens.removeIf(to -> to.getToken().equals(token) && to.getOwner().equalsIgnoreCase(owner));
            return Response.noContent().build();
        }

        @Override
        public Response deleteTokensFor(@NotNull final String owner) {
            tokens.removeIf(to -> to.getOwner().equalsIgnoreCase(owner));
            return Response.noContent().build();
        }

        @Override
        public Response deleteToken(@NotNull final Integer token) {
            tokens.removeIf(to -> to.getToken().equals(token));
            return Response.noContent().build();
        }

        @Override
        public Response deleteTokens() {
            tokens.clear();
            return Response.noContent().build();
        }

        @Override
        public Response save(@NotNull final GoogleMfaAuthToken tokenTO) {
            tokenTO.setKey(UUID.randomUUID().toString());
            tokens.add(tokenTO);
            return Response.ok().build();
        }

        @Override
        public GoogleMfaAuthToken findTokenFor(@NotNull final String owner, @NotNull final Integer token) {
            return tokens.stream()
                    .filter(to -> to.getToken().equals(token) && to.getOwner().equalsIgnoreCase(owner))
                    .findFirst().get();
        }

        @Override
        public PagedResult<GoogleMfaAuthToken> findTokensFor(@NotNull final String user) {
            PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
            result.getResult().addAll(tokens.stream().
                    filter(to -> to.getOwner().equalsIgnoreCase(user)).
                    collect(Collectors.toList()));
            result.setSize(result.getResult().size());
            result.setTotalCount(result.getSize());
            return result;
        }

        @Override
        public GoogleMfaAuthToken findTokenFor(@NotNull final String key) {
            return tokens.stream()
                    .filter(to -> to.getKey().equalsIgnoreCase(key))
                    .findFirst().get();
        }

        @Override
        public PagedResult<GoogleMfaAuthToken> countTokens() {
            PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
            result.setSize(tokens.size());
            result.setTotalCount(tokens.size());
            return result;
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
