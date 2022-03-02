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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class SyncopeCoreTestingServer implements ApplicationListener<ContextRefreshedEvent> {

    public static final List<WAClientApp> APPS = new ArrayList<>();

    private static final String ADDRESS = "http://localhost:9081/syncope/rest";

    @Autowired
    private ServiceOps serviceOps;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        synchronized (ADDRESS) {
            if (serviceOps.list(NetworkService.Type.CORE).isEmpty()) {
                // 1. start (mocked) Core as embedded CXF
                JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
                sf.setAddress(ADDRESS);
                sf.setResourceClasses(
                        WAClientAppService.class,
                        GoogleMfaAuthTokenService.class,
                        ImpersonationService.class);
                sf.setResourceProvider(
                        WAClientAppService.class,
                        new SingletonResourceProvider(new StubWAClientAppService(), true));
                sf.setResourceProvider(
                        GoogleMfaAuthTokenService.class,
                        new SingletonResourceProvider(new StubGoogleMfaAuthTokenService(), true));
                sf.setResourceProvider(
                        ImpersonationService.class,
                        new SingletonResourceProvider(new StubImpersonationService(), true));
                sf.setProviders(List.of(new JacksonJsonProvider(JsonMapper.builder().findAndAddModules().build())));
                sf.create();

                // 2. register Core in Keymaster
                NetworkService core = new NetworkService();
                core.setType(NetworkService.Type.CORE);
                core.setAddress(ADDRESS);
                serviceOps.register(core);
            }
        }
    }

    public static class StubImpersonationService implements ImpersonationService {

        private final Map<String, List<ImpersonationAccount>> accounts = new HashMap<>();

        @Override
        public List<ImpersonationAccount> read(final String owner) {
            return accounts.containsKey(owner) ? accounts.get(owner) : List.of();
        }

        @Override
        public void create(final String owner, final ImpersonationAccount account) {
            try {
                if (accounts.containsKey(owner) && accounts.get(owner).stream().
                        noneMatch(acct -> acct.getImpersonated().equalsIgnoreCase(account.getImpersonated()))) {

                    accounts.get(owner).add(account);
                } else {
                    List<ImpersonationAccount> list = new ArrayList<>();
                    list.add(account);
                    accounts.put(owner, list);
                }
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void delete(final String owner, final String impersonated) {
            if (accounts.containsKey(owner)) {
                accounts.get(owner).removeIf(acct -> acct.getImpersonated().equalsIgnoreCase(impersonated));
            }
        }
    }

    public static class StubGoogleMfaAuthTokenService implements GoogleMfaAuthTokenService {

        private final Map<String, GoogleMfaAuthToken> tokens = new HashMap<>();

        @Override
        public void delete(final OffsetDateTime expirationDate) {
            if (expirationDate == null) {
                tokens.clear();
            } else {
                tokens.entrySet().removeIf(token -> token.getValue().getIssueDate().compareTo(expirationDate) >= 0);
            }
        }

        @Override
        public void delete(final String owner, final int otp) {
            tokens.entrySet().
                    removeIf(e -> e.getValue().getOtp() == otp && e.getKey().equalsIgnoreCase(owner));
        }

        @Override
        public void delete(final String owner) {
            tokens.entrySet().removeIf(e -> e.getKey().equalsIgnoreCase(owner));
        }

        @Override
        public void delete(final int otp) {
            tokens.entrySet().removeIf(to -> to.getValue().getOtp() == otp);
        }

        @Override
        public void store(final String owner, final GoogleMfaAuthToken tokenTO) {
            tokens.put(owner, tokenTO);
        }

        @Override
        public GoogleMfaAuthToken read(final String owner, final int otp) {
            return tokens.entrySet().stream()
                    .filter(to -> to.getValue().getOtp() == otp && to.getKey().equalsIgnoreCase(owner))
                    .findFirst().get().getValue();
        }

        @Override
        public PagedResult<GoogleMfaAuthToken> read(final String user) {
            PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
            result.getResult().addAll(tokens.entrySet().stream().
                    filter(to -> to.getKey().equalsIgnoreCase(user)).
                    map(Map.Entry::getValue).
                    collect(Collectors.toList()));
            result.setSize(result.getResult().size());
            result.setTotalCount(result.getSize());
            return result;
        }

        @Override
        public PagedResult<GoogleMfaAuthToken> list() {
            PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
            result.setSize(tokens.size());
            result.setTotalCount(tokens.size());
            result.getResult().addAll(tokens.values());
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
