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

import java.security.AccessControlException;
import java.text.DateFormat;
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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class SyncopeConsoleSession extends AuthenticatedWebSession implements BaseSession {

    private static final long serialVersionUID = 747562246415852166L;

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleSession.class);

    private final SyncopeClientFactoryBean clientFactory;

    private final SyncopeClient anonymousClient;

    private final PlatformInfo platformInfo;

    private final SystemInfo systemInfo;

    private final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    private final ThreadPoolTaskExecutor executor;

    private String domain;

    private SyncopeClient client;

    private UserTO selfTO;

    private Map<String, Set<String>> auth;

    private Roles roles;

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    public SyncopeConsoleSession(final Request request) {
        super(request);

        clientFactory = SyncopeWebApplication.get().newClientFactory();
        anonymousClient = clientFactory.create(new AnonymousAuthenticationHandler(
                SyncopeWebApplication.get().getAnonymousUser(),
                SyncopeWebApplication.get().getAnonymousKey()));

        platformInfo = anonymousClient.getService(SyncopeService.class).platform();
        systemInfo = anonymousClient.getService(SyncopeService.class).system();

        executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setCorePoolSize(SyncopeWebApplication.get().getCorePoolSize());
        executor.setMaxPoolSize(SyncopeWebApplication.get().getMaxPoolSize());
        executor.setQueueCapacity(SyncopeWebApplication.get().getQueueCapacity());
        executor.initialize();
    }

    @Override
    public void onException(final Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        String message = root.getMessage();

        if (root instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) root;
            if (!sce.isComposite()) {
                message = sce.getElements().stream().collect(Collectors.joining(", "));
            }
        } else if (root instanceof AccessControlException || root instanceof ForbiddenException) {
            Error error = StringUtils.containsIgnoreCase(message, "expired")
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
        error(message);
    }

    public MediaType getMediaType() {
        return clientFactory.getContentType().getMediaType();
    }

    public SyncopeClient getAnonymousClient() {
        return anonymousClient;
    }

    public void execute(final Runnable command) {
        try {
            executor.execute(command);
        } catch (TaskRejectedException e) {
            LOG.error("Could not execute {}", command, e);
        }
    }

    public <T> Future<T> execute(final Callable<T> command) {
        try {
            return executor.submit(command);
        } catch (TaskRejectedException e) {
            LOG.error("Could not execute {}", command, e);

            return new CompletableFuture<>();
        }
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

    public String getJWT() {
        return Optional.ofNullable(client).map(SyncopeClient::getJWT).orElse(null);
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(username, password);

            refreshAuth(username);

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    public boolean authenticate(final String jwt) {
        boolean authenticated = false;

        try {
            client = clientFactory.setDomain(getDomain()).create(jwt);

            refreshAuth(null);

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

    public void cleanup() {
        client = null;
        auth = null;
        selfTO = null;
        services.clear();
    }

    @Override
    public void invalidate() {
        if (getJWT() != null) {
            if (client != null) {
                client.logout();
            }
            cleanup();
        }
        executor.shutdown();
        super.invalidate();
    }

    public UserTO getSelfTO() {
        return selfTO;
    }

    public List<String> getAuthRealms() {
        return auth.values().stream().flatMap(Set::stream).distinct().sorted().collect(Collectors.toList());
    }

    public boolean owns(final String entitlements, final String... realms) {
        if (StringUtils.isEmpty(entitlements)) {
            return true;
        }

        if (auth == null) {
            return false;
        }

        Set<String> requested = ArrayUtils.isEmpty(realms)
                ? Set.of(SyncopeConstants.ROOT_REALM)
                : Set.of(realms);

        for (String entitlement : entitlements.split(",")) {
            if (auth.containsKey(entitlement)) {
                boolean owns = false;

                Set<String> owned = auth.get(entitlement);
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
        if (isSignedIn() && roles == null && auth != null) {
            roles = new Roles(auth.keySet().toArray(new String[] {}));
            roles.add(Constants.ROLE_AUTHENTICATED);
        }

        return roles;
    }

    public void refreshAuth(final String username) {
        try {
            Pair<Map<String, Set<String>>, UserTO> self = client.self();
            auth = self.getLeft();
            selfTO = self.getRight();
            roles = null;
        } catch (ForbiddenException e) {
            LOG.warn("Could not read self(), probably in a {} scenario", IdRepoEntitlement.MUST_CHANGE_PASSWORD, e);

            selfTO = new UserTO();
            selfTO.setUsername(username);
            selfTO.setMustChangePassword(true);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedService(final Class<T> serviceClass) {
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
    public <T> T getAnonymousService(final Class<T> serviceClass) {
        return getAnonymousClient().getService(serviceClass);
    }

    @Override
    public <T> T getService(final Class<T> serviceClass) {
        return getCachedService(serviceClass);
    }

    @Override
    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T serviceInstance = getCachedService(serviceClass);
        WebClient.client(serviceInstance).match(new EntityTag(etag), false);

        return serviceInstance;
    }

    public BatchRequest batch() {
        return client.batch();
    }

    @Override
    public <T> void resetClient(final Class<T> service) {
        T serviceInstance = getCachedService(service);
        WebClient.client(serviceInstance).reset();
    }

    @Override
    public FastDateFormat getDateFormat() {
        return FastDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale());
    }
}
