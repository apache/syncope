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
package org.apache.syncope.client.console;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.ws.WebServiceException;
import java.text.DateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.CollectionUtils;

public class SyncopeConsoleSession extends AuthenticatedWebSession implements BaseSession {

    private static final long serialVersionUID = 747562246415852166L;

    public enum Error {
        SESSION_EXPIRED("error.session.expired", "Session expired: please login again"),
        AUTHORIZATION("error.authorization", "Insufficient access rights when performing the requested operation"),
        REST("error.rest", "There was an error while contacting the Core server");

        private final String key;

        private final String fallback;

        Error(final String key, final String fallback) {
            this.key = key;
            this.fallback = fallback;
        }

        public String key() {
            return key;
        }

        public String fallback() {
            return fallback;
        }
    }

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleSession.class);

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    protected final SyncopeClientFactoryBean clientFactory;

    protected final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    protected final SimpleAsyncTaskExecutor executor;

    protected String domain;

    protected SyncopeClient client;

    protected Instant jwtExpiration;

    protected SyncopeAnonymousClient anonymousClient;

    protected Pair<String, String> gitAndBuildInfo;

    protected PlatformInfo platformInfo;

    protected SystemInfo systemInfo;

    protected SyncopeClient.Self self;

    protected String delegatedBy;

    protected Roles roles;

    public SyncopeConsoleSession(final Request request) {
        super(request);

        clientFactory = SyncopeWebApplication.get().newClientFactory();

        executor = new SimpleAsyncTaskScheduler();
        executor.setVirtualThreads(true);
    }

    protected String message(final SyncopeClientException sce) {
        return sce.getType().name() + ": " + String.join(", ", sce.getElements());
    }

    @Override
    public void onException(final Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        String message = root.getMessage();

        if (root instanceof SyncopeClientException sce) {
            message = sce.isComposite()
                    ? sce.asComposite().getExceptions().stream().map(this::message).collect(Collectors.joining("; "))
                    : message(sce);
        } else if (root instanceof NotAuthorizedException || root instanceof ForbiddenException) {
            Error error = Strings.CI.contains(message, "expired")
                    ? Error.SESSION_EXPIRED
                    : Error.AUTHORIZATION;
            message = getApplication().getResourceSettings().getLocalizer().
                    getString(error.key(), null, null, null, null, error.fallback());
        } else if (root instanceof BadRequestException || root instanceof WebServiceException) {
            message = getApplication().getResourceSettings().getLocalizer().
                    getString(Error.REST.key(), null, null, null, null, Error.REST.fallback());
        }

        message = getApplication().getResourceSettings().getLocalizer().
                getString(message, null, null, null, null, message);
        error(message.replace("\n", "<br/>"));
    }

    public MediaType getMediaType() {
        return clientFactory.getContentType().getMediaType();
    }

    public void execute(final Runnable command) {
        try {
            executor.execute(command);
        } catch (TaskRejectedException e) {
            LOG.error("Could not execute {}", command, e);
        }
    }

    @Override
    public <T> Future<T> execute(final Callable<T> command) {
        try {
            return executor.submit(command);
        } catch (TaskRejectedException e) {
            LOG.error("Could not execute {}", command, e);

            return new CompletableFuture<>();
        }
    }

    public Pair<String, String> gitAndBuildInfo() {
        return gitAndBuildInfo;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    @Override
    public void setDomain(final String domain) {
        this.domain = domain;
    }

    @Override
    public String getDomain() {
        return StringUtils.isBlank(domain) ? SyncopeConstants.MASTER_DOMAIN : domain;
    }

    @Override
    public String getJWT() {
        return Optional.ofNullable(client).
                flatMap(SyncopeClient::jwtInfo).map(SyncopeClient.JwtInfo::value).
                orElse(null);
    }

    public boolean isJWTExpiring() {
        return Optional.ofNullable(jwtExpiration).
                map(je -> je.isBefore(Instant.now().
                plus(SyncopeWebApplication.get().getJwtExpirationMinutesThreshold(), ChronoUnit.MINUTES))).
                orElse(false);
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(username, password);
            jwtExpiration = client.jwtInfo().map(jwtInfo -> jwtInfo.expiration().toInstant()).orElse(null);

            reloadSettings(username);

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    @Override
    public boolean authenticate(final String jwt, final Instant jwtExpiration) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(jwt);
            this.jwtExpiration = jwtExpiration;

            reloadSettings(null);

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        if (authenticated) {
            bind();
        }
        signIn(authenticated);

        return authenticated;
    }

    public boolean refresh() {
        boolean refreshed = false;

        try {
            client.refresh();
            jwtExpiration = client.jwtInfo().map(jwtInfo -> jwtInfo.expiration().toInstant()).orElse(null);

            reloadSettings(null);

            refreshed = true;
        } catch (Exception e) {
            LOG.error("Refresh failed", e);
        }

        if (refreshed) {
            changeSessionId();
            bind();
        }

        return refreshed;
    }

    public void cleanup() {
        anonymousClient = null;
        gitAndBuildInfo = null;
        platformInfo = null;
        systemInfo = null;

        client = null;
        jwtExpiration = null;
        self = null;
        delegatedBy = null;
        services.clear();
    }

    @Override
    public void invalidate() {
        SyncopeWebApplication.get().storeLoggedOutSessionId(getId());

        if (getJWT() != null) {
            if (client != null) {
                client.logout();
            }
            cleanup();
        }

        super.invalidate();
    }

    public UserTO getSelfTO() {
        return self.user();
    }

    public List<String> getAuthRealms() {
        return self.entitlements().values().stream().flatMap(Set::stream).distinct().sorted().toList();
    }

    public List<String> getSearchableRealms() {
        Set<String> roots = self.entitlements().get(IdRepoEntitlement.REALM_SEARCH);
        return CollectionUtils.isEmpty(roots)
                ? List.of()
                : roots.stream().sorted().toList();
    }

    public Optional<String> getRootRealm(final String initial) {
        List<String> searchable = getSearchableRealms();
        return searchable.isEmpty()
                ? Optional.empty()
                : initial != null && searchable.stream().anyMatch(initial::startsWith)
                ? Optional.of(initial)
                : searchable.stream().findFirst();
    }

    public boolean owns(final String entitlements, final String... realms) {
        if (StringUtils.isEmpty(entitlements)) {
            return true;
        }

        if (self.entitlements() == null) {
            return false;
        }

        Set<String> requested = ArrayUtils.isEmpty(realms)
                ? Set.of()
                : Set.of(realms);

        for (String entitlement : entitlements.split(",")) {
            if (self.entitlements().containsKey(entitlement)) {
                boolean owns = false;

                Set<String> owned = self.entitlements().get(entitlement).stream().
                        map(RealmsUtils::getFullPath).collect(Collectors.toSet());
                if (requested.isEmpty()) {
                    return !owned.isEmpty();
                }

                for (String realm : requested) {
                    if (realm.startsWith(SyncopeConstants.ROOT_REALM)) {
                        owns |= owned.stream().anyMatch(realm::startsWith);
                    } else {
                        owns |= owned.contains(realm);
                    }
                }
                return owns;
            }
        }

        return false;
    }

    @Override
    public Roles getRoles() {
        if (isSignedIn() && roles == null && self.entitlements() != null) {
            roles = new Roles(self.entitlements().keySet().toArray(String[]::new));
            roles.add(Constants.ROLE_AUTHENTICATED);
        }

        return roles;
    }

    public List<String> getDelegations() {
        return self.delegations();
    }

    public String getDelegatedBy() {
        return delegatedBy;
    }

    public void setDelegatedBy(final String delegatedBy) {
        this.delegatedBy = delegatedBy;

        this.client.delegatedBy(delegatedBy);

        reloadSettings(null);
    }

    public void reloadSettings(final String username) {
        try {
            anonymousClient = SyncopeWebApplication.get().newAnonymousClient(getDomain());
            gitAndBuildInfo = anonymousClient.gitAndBuildInfo();
            platformInfo = anonymousClient.platform();
            systemInfo = anonymousClient.system();

            self = client.self();
            roles = null;
        } catch (ForbiddenException e) {
            LOG.warn("Could not read self(), probably in a {} scenario", IdRepoEntitlement.MUST_CHANGE_PASSWORD, e);

            UserTO user = new UserTO();
            user.setUsername(username);
            user.setMustChangePassword(true);
            self = new SyncopeClient.Self(user, Map.of(), List.of());
        }
    }

    @Override
    public SyncopeAnonymousClient getAnonymousClient() {
        if (anonymousClient == null) {
            anonymousClient = SyncopeWebApplication.get().newAnonymousClient(getDomain());
        }
        return anonymousClient;
    }

    @Override
    public <T> T getAnonymousService(final Class<T> serviceClass) {
        return getAnonymousClient().getService(serviceClass);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getCachedService(final Class<T> serviceClass) {
        T service;
        if (services.containsKey(serviceClass)) {
            service = (T) services.get(serviceClass);
        } else {
            service = client.getService(serviceClass);
            services.put(serviceClass, service);
        }

        WebClient.client(service).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        return service;
    }

    @Override
    public <T> T getService(final Class<T> serviceClass) {
        return getCachedService(serviceClass);
    }

    @Override
    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T service = getCachedService(serviceClass);
        WebClient.client(service).match(new EntityTag(etag), false);

        return service;
    }

    public BatchRequest batch() {
        return client.batch();
    }

    @Override
    public <T> void resetClient(final Class<T> serviceClass) {
        T service = getCachedService(serviceClass);
        WebClient.client(service).reset();
    }

    @Override
    public DateOps.Format getDateFormat() {
        return new DateOps.Format(FastDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale()));
    }
}
