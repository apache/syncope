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
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class SyncopeCoreTestingServer implements ApplicationListener<ContextRefreshedEvent> {

    private static final String ADDRESS = "http://localhost:9081/syncope/rest";

    public static final List<AuthModuleTO> AUTH_MODULES = new ArrayList<>();

    public static final List<AttrRepoTO> ATTR_REPOS = new ArrayList<>();

    public static final List<WAClientApp> CLIENT_APPS = new ArrayList<>();

    public static final List<Attr> CONFIG = new ArrayList<>();

    protected static class StubAuthModuleService implements AuthModuleService {

        @Override
        public AuthModuleTO read(final String key) {
            return AUTH_MODULES.stream().filter(m -> Objects.equals(key, m.getKey())).
                    findFirst().orElseThrow(() -> new NotFoundException("Auth Module with key " + key));
        }

        @Override
        public List<AuthModuleTO> list() {
            return AUTH_MODULES;
        }

        @Override
        public Response create(final AuthModuleTO authModuleTO) {
            AUTH_MODULES.add(authModuleTO);
            return Response.created(null).build();
        }

        @Override
        public void update(final AuthModuleTO authModuleTO) {
            delete(authModuleTO.getKey());
            create(authModuleTO);
        }

        @Override
        public void delete(final String key) {
            AUTH_MODULES.removeIf(m -> Objects.equals(key, m.getKey()));
        }
    }

    protected static class StubAttrRepoService implements AttrRepoService {

        @Override
        public AttrRepoTO read(final String key) {
            return ATTR_REPOS.stream().filter(m -> Objects.equals(key, m.getKey())).
                    findFirst().orElseThrow(() -> new NotFoundException("Attr Repo with key " + key));
        }

        @Override
        public List<AttrRepoTO> list() {
            return ATTR_REPOS;
        }

        @Override
        public Response create(final AttrRepoTO attrRepoTO) {
            ATTR_REPOS.add(attrRepoTO);
            return Response.created(null).build();
        }

        @Override
        public void update(final AttrRepoTO attrRepoTO) {
            delete(attrRepoTO.getKey());
            create(attrRepoTO);
        }

        @Override
        public void delete(final String key) {
            ATTR_REPOS.removeIf(m -> Objects.equals(key, m.getKey()));
        }
    }

    protected static class StubWAClientAppService implements WAClientAppService {

        @Override
        public List<WAClientApp> list() {
            return CLIENT_APPS;
        }

        @Override
        public WAClientApp read(final Long clientAppId, final ClientAppType type) {
            return CLIENT_APPS.stream().
                    filter(app -> Objects.equals(clientAppId, app.getClientAppTO().getClientAppId())).
                    findFirst().orElseThrow(() -> new NotFoundException("ClientApp with clientId " + clientAppId));
        }

        @Override
        public WAClientApp read(final String name, final ClientAppType type) {
            return CLIENT_APPS.stream().filter(app -> Objects.equals(name, app.getClientAppTO().getName())).
                    findFirst().orElseThrow(() -> new NotFoundException("ClientApp with name " + name));
        }
    }

    protected static class StubWAConfigService implements WAConfigService {

        @Override
        public List<Attr> list() {
            return CONFIG;
        }

        @Override
        public Attr get(final String schema) {
            return CONFIG.stream().filter(c -> Objects.equals(schema, c.getSchema())).
                    findFirst().orElseThrow(() -> new NotFoundException("Config with schema " + schema));
        }

        @Override
        public void set(final Attr value) {
            delete(value.getSchema());
            CONFIG.add(value);
        }

        @Override
        public void delete(final String schema) {
            CONFIG.removeIf(c -> Objects.equals(schema, c.getSchema()));
        }

        @Override
        public void pushToWA(final PushSubject subject, final List<String> services) {
            // nothing to do
        }
    }

    protected static class StubImpersonationService implements ImpersonationService {

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

    protected static class StubGoogleMfaAuthTokenService implements GoogleMfaAuthTokenService {

        private final Map<String, GoogleMfaAuthToken> tokens = new HashMap<>();

        @Override
        public void delete(final LocalDateTime expirationDate) {
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
            return tokens.entrySet().stream().
                    filter(to -> to.getValue().getOtp() == otp && to.getKey().equalsIgnoreCase(owner)).
                    findFirst().get().getValue();
        }

        @Override
        public PagedResult<GoogleMfaAuthToken> read(final String user) {
            PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
            result.getResult().addAll(tokens.entrySet().stream().
                    filter(to -> to.getKey().equalsIgnoreCase(user)).
                    map(Map.Entry::getValue).
                    toList());
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

    protected static class StubAuditService implements AuditService {

        @Override
        public List<AuditConfTO> confs() {
            return List.of();
        }

        @Override
        public AuditConfTO getConf(final String key) {
            throw new NotFoundException();
        }

        @Override
        public void setConf(final AuditConfTO auditTO) {
            // nothing to do
        }

        @Override
        public void deleteConf(final String key) {
            // nothing to do
        }

        @Override
        public List<OpEvent> events() {
            return List.of();
        }

        @Override
        public PagedResult<AuditEventTO> search(final AuditQuery auditQuery) {
            return new PagedResult<>();
        }

        @Override
        public void create(final AuditEventTO auditEvent) {
            // nothing to do
        }
    }

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
                        AuditService.class,
                        AuthModuleService.class,
                        AttrRepoService.class,
                        WAClientAppService.class,
                        WAConfigService.class,
                        GoogleMfaAuthTokenService.class,
                        ImpersonationService.class);
                sf.setResourceProvider(
                        AuditService.class,
                        new SingletonResourceProvider(new StubAuditService(), true));
                sf.setResourceProvider(
                        AuthModuleService.class,
                        new SingletonResourceProvider(new StubAuthModuleService(), true));
                sf.setResourceProvider(
                        AttrRepoService.class,
                        new SingletonResourceProvider(new StubAttrRepoService(), true));
                sf.setResourceProvider(
                        WAClientAppService.class,
                        new SingletonResourceProvider(new StubWAClientAppService(), true));
                sf.setResourceProvider(
                        WAConfigService.class,
                        new SingletonResourceProvider(new StubWAConfigService(), true));
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
}
