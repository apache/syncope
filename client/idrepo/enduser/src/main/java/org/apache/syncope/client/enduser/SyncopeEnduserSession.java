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
package org.apache.syncope.client.enduser;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.ws.WebServiceException;
import java.text.DateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeEnduserSession extends AuthenticatedWebSession implements BaseSession {

    private static final long serialVersionUID = 747562246415852166L;

    public enum Error {
        INVALID_SECURITY_ANSWER("invalid.security.answer", "Invalid Security Answer"),
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

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeEnduserSession.class);

    public static SyncopeEnduserSession get() {
        return (SyncopeEnduserSession) Session.get();
    }

    protected final SyncopeClientFactoryBean clientFactory;

    protected final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    protected String domain;

    protected SyncopeClient client;

    protected Instant jwtExpiration;

    protected SyncopeAnonymousClient anonymousClient;

    protected UserTO selfTO;

    protected OffsetDateTime lastReauth;

    public SyncopeEnduserSession(final Request request) {
        super(request);

        clientFactory = SyncopeWebApplication.get().newClientFactory();
    }

    public boolean isReauthExpired() {
        return SyncopeWebApplication.get().getReauthExpirationMinutesTimeout().
                map(timeout -> Optional.ofNullable(lastReauth).
                map(r -> ChronoUnit.MINUTES.between(r, OffsetDateTime.now()) > timeout).
                orElse(true)).
                orElse(false);
    }

    public void setLastReauth() {
        this.lastReauth = OffsetDateTime.now();
    }

    public void clearLastReauth() {
        this.lastReauth = null;
    }

    protected String message(final SyncopeClientException sce) {
        Error error = null;
        if (sce.getType() == ClientExceptionType.InvalidSecurityAnswer) {
            error = Error.INVALID_SECURITY_ANSWER;
        }
        if (error == null) {
            return sce.getType().name() + ": " + String.join(", ", sce.getElements());
        }
        return getApplication().getResourceSettings().getLocalizer().
                getString(error.key(), null, null, null, null, error.fallback());
    }

    /**
     * Extract and localize (if translation available) the actual message from the given exception; then, report it
     * via {@link Session#error(java.io.Serializable)}.
     *
     * @see org.apache.syncope.client.lib.RestClientExceptionMapper
     *
     * @param e raised exception
     */
    @Override
    public void onException(final Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        String message = root.getMessage();

        if (root instanceof final SyncopeClientException sce) {
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
        error(message);
    }

    public MediaType getMediaType() {
        return clientFactory.getContentType().getMediaType();
    }

    @Override
    public <T> Future<T> execute(final Callable<T> command) {
        try {
            return CompletableFuture.completedFuture(command.call());
        } catch (Exception e) {
            LOG.error("Could not execute {}", command, e);
        }
        return new CompletableFuture<>();
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

    @Override
    public Roles getRoles() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PlatformInfo getPlatformInfo() {
        return getAnonymousClient().platform();
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        boolean authenticated = false;
        if (SyncopeWebApplication.get().getAdminUser().equalsIgnoreCase(username)) {
            return authenticated;
        }

        try {
            client = clientFactory.setDomain(getDomain()).create(username, password);
            jwtExpiration = client.jwtInfo().map(jwtInfo -> jwtInfo.expiration().toInstant()).orElse(null);

            refreshAuth(username);

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

            refreshAuth(null);

            authenticated = true;
        } catch (Exception e) {
            LOG.error("Authentication failed", e);
        }

        return authenticated;
    }

    protected void refreshAuth(final String username) {
        try {
            anonymousClient = SyncopeWebApplication.get().newAnonymousClient(getDomain());

            selfTO = client.self().user();
        } catch (ForbiddenException e) {
            LOG.warn("Could not read self(), probably in a {} scenario", IdRepoEntitlement.MUST_CHANGE_PASSWORD, e);

            selfTO = new UserTO();
            selfTO.setUsername(username);
            selfTO.setMustChangePassword(true);
        }

        // bind explicitly this session to have a stateful behavior during http requests, unless session will
        // expire at each request
        this.bind();
    }

    protected boolean isAuthenticated() {
        return getJWT() != null;
    }

    protected boolean isMustChangePassword() {
        return Optional.ofNullable(selfTO).map(UserTO::isMustChangePassword).orElse(false);
    }

    public void cleanup() {
        anonymousClient = null;

        client = null;
        jwtExpiration = null;
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

        super.invalidate();
    }

    public UserTO getSelfTO() {
        return getSelfTO(false);
    }

    public UserTO getSelfTO(final boolean reload) {
        if (reload) {
            refreshAuth(selfTO.getUsername());
        }
        return selfTO;
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
    public <T> T getService(final Class<T> serviceClass) {
        T service = (client == null || !isAuthenticated())
                ? getAnonymousClient().getService(serviceClass)
                : client.getService(serviceClass);
        WebClient.client(service).header(RESTHeaders.DOMAIN, getDomain());
        return service;
    }

    @Override
    public <T> T getService(final String etag, final Class<T> serviceClass) {
        T serviceInstance = getService(serviceClass);
        WebClient.client(serviceInstance).match(new EntityTag(etag), false).
                type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        return serviceInstance;
    }

    @Override
    public <T> void resetClient(final Class<T> service) {
        T serviceInstance = getCachedService(service);
        WebClient.client(serviceInstance).reset();
    }

    @Override
    public DateOps.Format getDateFormat() {
        return new DateOps.Format(FastDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getLocale()));
    }
}
