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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

public class SyncopeConsoleSession extends AuthenticatedWebSession {

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

    protected final SyncopeClientFactoryBean clientFactory;

    protected final SyncopeClient anonymousClient;

    protected final PlatformInfo platformInfo;

    protected final SystemInfo systemInfo;

    protected final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    protected final ThreadPoolTaskExecutor executor;

    protected String domain;

    protected SyncopeClient client;

    protected UserTO selfTO;

    protected Map<String, Set<String>> auth;

    protected List<String> delegations;

    protected String delegatedBy;

    protected Roles roles;

    public static SyncopeConsoleSession get() {
        return (SyncopeConsoleSession) Session.get();
    }

    public SyncopeConsoleSession(final Request request) {
        super(request);

        clientFactory = SyncopeConsoleApplication.get().newClientFactory();
        anonymousClient = clientFactory.create(new AnonymousAuthenticationHandler(
                SyncopeConsoleApplication.get().getAnonymousUser(),
                SyncopeConsoleApplication.get().getAnonymousKey()));

        platformInfo = anonymousClient.getService(SyncopeService.class).platform();
        systemInfo = anonymousClient.getService(SyncopeService.class).system();

        executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setCorePoolSize(SyncopeConsoleApplication.get().getCorePoolSize());
        executor.setMaxPoolSize(SyncopeConsoleApplication.get().getMaxPoolSize());
        executor.setQueueCapacity(SyncopeConsoleApplication.get().getQueueCapacity());
        executor.initialize();
    }

    protected String message(final SyncopeClientException sce) {
        return sce.getType().name() + ": " + sce.getElements().stream().collect(Collectors.joining(", "));
    }

    /**
     * Extract and localize (if translation available) the actual message from the given exception; then, report it
     * via {@link Session#error(java.io.Serializable)}.
     *
     * @see org.apache.syncope.client.lib.RestClientExceptionMapper
     *
     * @param e raised exception
     */
    public void onException(final Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        String message = root.getMessage();

        if (root instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) root;
            message = sce.isComposite()
                    ? sce.asComposite().getExceptions().stream().map(this::message).collect(Collectors.joining("; "))
                    : message(sce);
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

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return StringUtils.isBlank(domain) ? SyncopeConstants.MASTER_DOMAIN : domain;
    }

    public String getJWT() {
        return client == null ? null : client.getJWT();
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
        delegations = null;
        delegatedBy = null;
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

    public List<String> getSearchableRealms() {
        Set<String> roots = auth.get(StandardEntitlement.REALM_LIST);
        return CollectionUtils.isEmpty(roots)
                ? Collections.emptyList()
                : roots.stream().sorted().collect(Collectors.toList());
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

        if (auth == null) {
            return false;
        }

        Set<String> requested = ArrayUtils.isEmpty(realms)
                ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(realms));

        for (String entitlement : entitlements.split(",")) {
            if (auth.containsKey(entitlement)) {
                boolean owns = false;

                Set<String> owned = auth.get(entitlement).stream().
                        map(RealmsUtils::getFullPath).collect(Collectors.toSet());
                if (requested.isEmpty()) {
                    return !owned.isEmpty();
                } else {
                    for (String realm : requested) {
                        if (realm.startsWith(SyncopeConstants.ROOT_REALM)) {
                            owns |= owned.stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm));
                        } else {
                            owns |= owned.contains(realm);
                        }
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

    public List<String> getDelegations() {
        return delegations;
    }

    public String getDelegatedBy() {
        return delegatedBy;
    }

    public void setDelegatedBy(final String delegatedBy) {
        this.delegatedBy = delegatedBy;

        this.client.delegatedBy(delegatedBy);

        refreshAuth(null);
    }

    public void refreshAuth(final String username) {
        try {
            Triple<Map<String, Set<String>>, List<String>, UserTO> self = client.self();
            auth = self.getLeft();
            delegations = self.getMiddle();
            selfTO = self.getRight();
            roles = null;
        } catch (ForbiddenException e) {
            LOG.warn("Could not read self(), probably in a {} scenario", StandardEntitlement.MUST_CHANGE_PASSWORD, e);

            selfTO = new UserTO();
            selfTO.setUsername(username);
            selfTO.setMustChangePassword(true);
        }
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

    public <T> T getService(final Class<T> serviceClass) {
        return getCachedService(serviceClass);
    }

    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T serviceInstance = getCachedService(serviceClass);
        WebClient.client(serviceInstance).match(new EntityTag(etag), false);

        return serviceInstance;
    }

    public BatchRequest batch() {
        return client.batch();
    }

    public <T> void resetClient(final Class<T> service) {
        T serviceInstance = getCachedService(service);
        WebClient.client(serviceInstance).reset();
    }

    public FastDateFormat getDateFormat() {
        return FastDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale());
    }
}
