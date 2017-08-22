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
package org.apache.syncope.client.lib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.OrderByClauseBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.UserSelfService;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * {@link SyncopeClientFactoryBean}.
 */
public class SyncopeClient {

    private static final String HEADER_SPLIT_PROPERTY = "org.apache.cxf.http.header.split";

    private final MediaType mediaType;

    private final JAXRSClientFactoryBean restClientFactory;

    private final RestClientExceptionMapper exceptionMapper;

    private final boolean useCompression;

    public SyncopeClient(
            final MediaType mediaType,
            final JAXRSClientFactoryBean restClientFactory,
            final RestClientExceptionMapper exceptionMapper,
            final AuthenticationHandler handler,
            final boolean useCompression) {

        this.mediaType = mediaType;
        this.restClientFactory = restClientFactory;
        if (this.restClientFactory.getHeaders() == null) {
            this.restClientFactory.setHeaders(new HashMap<>());
        }
        this.exceptionMapper = exceptionMapper;
        init(handler);
        this.useCompression = useCompression;
    }

    /**
     * Initializes the provided {@code restClientFactory} with the authentication capabilities of the provided
     * {@code handler}.
     *
     * Currently supports:
     * <ul>
     * <li>{@link JWTAuthenticationHandler}</li>
     * <li>{@link AnonymousAuthenticationHandler}</li>
     * <li>{@link BasicAuthenticationHandler}</li>
     * </ul>
     * More can be supported by subclasses.
     *
     * @param handler authentication handler
     */
    protected void init(final AuthenticationHandler handler) {
        cleanup();

        if (handler instanceof AnonymousAuthenticationHandler) {
            restClientFactory.setUsername(((AnonymousAuthenticationHandler) handler).getUsername());
            restClientFactory.setPassword(((AnonymousAuthenticationHandler) handler).getPassword());
        } else if (handler instanceof BasicAuthenticationHandler) {
            restClientFactory.setUsername(((BasicAuthenticationHandler) handler).getUsername());
            restClientFactory.setPassword(((BasicAuthenticationHandler) handler).getPassword());

            String jwt = getService(AccessTokenService.class).login().getHeaderString(RESTHeaders.TOKEN);
            restClientFactory.getHeaders().put(HttpHeaders.AUTHORIZATION, Collections.singletonList("Bearer " + jwt));

            restClientFactory.setUsername(null);
            restClientFactory.setPassword(null);
        } else if (handler instanceof JWTAuthenticationHandler) {
            restClientFactory.getHeaders().put(
                    HttpHeaders.AUTHORIZATION,
                    Collections.singletonList("Bearer " + ((JWTAuthenticationHandler) handler).getJwt()));
        }
    }

    protected void cleanup() {
        restClientFactory.getHeaders().remove(HttpHeaders.AUTHORIZATION);
        restClientFactory.setUsername(null);
        restClientFactory.setPassword(null);
    }

    /**
     * Attempts to extend the lifespan of the JWT currently in use.
     */
    public void refresh() {
        String jwt = getService(AccessTokenService.class).refresh().getHeaderString(RESTHeaders.TOKEN);
        restClientFactory.getHeaders().put(HttpHeaders.AUTHORIZATION, Collections.singletonList("Bearer " + jwt));
    }

    /**
     * Invalidates the JWT currently in use.
     */
    public void logout() {
        getService(AccessTokenService.class).logout();
        cleanup();
    }

    /**
     * (Re)initializes the current instance with the authentication capabilities of the provided {@code handler}.
     *
     * @param handler authentication handler
     */
    public void login(final AuthenticationHandler handler) {
        init(handler);
    }

    /**
     * Returns a new instance of {@link UserFiqlSearchConditionBuilder}, for assisted building of FIQL queries.
     *
     * @return default instance of {@link UserFiqlSearchConditionBuilder}
     */
    public static UserFiqlSearchConditionBuilder getUserSearchConditionBuilder() {
        return new UserFiqlSearchConditionBuilder();
    }

    /**
     * Returns a new instance of {@link GroupFiqlSearchConditionBuilder}, for assisted building of FIQL queries.
     *
     * @return default instance of {@link GroupFiqlSearchConditionBuilder}
     */
    public static GroupFiqlSearchConditionBuilder getGroupSearchConditionBuilder() {
        return new GroupFiqlSearchConditionBuilder();
    }

    /**
     * Returns a new instance of {@link AnyObjectFiqlSearchConditionBuilder}, for assisted building of FIQL queries.
     *
     * @param type any type
     * @return default instance of {@link AnyObjectFiqlSearchConditionBuilder}
     */
    public static AnyObjectFiqlSearchConditionBuilder getAnyObjectSearchConditionBuilder(final String type) {
        return new AnyObjectFiqlSearchConditionBuilder(type);
    }

    /**
     * Returns a new instance of {@link OrderByClauseBuilder}, for assisted building of {@code orderby} clauses.
     *
     * @return default instance of {@link OrderByClauseBuilder}
     */
    public static OrderByClauseBuilder getOrderByClauseBuilder() {
        return new OrderByClauseBuilder();
    }

    /**
     * Returns the JWT in used by this instance, passed with the {@link HttpHeaders#AUTHORIZATION} header
     * in all requests. It can be null (in case {@link NoAuthenticationHandler} or
     * {@link AnonymousAuthenticationHandler} were used).
     *
     * @return the JWT in used by this instance
     */
    public String getJWT() {
        List<String> headerValues = restClientFactory.getHeaders().get(HttpHeaders.AUTHORIZATION);
        String header = headerValues == null || headerValues.isEmpty()
                ? null
                : headerValues.get(0);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());

        }
        return null;
    }

    /**
     * Returns the domain configured for this instance, or {@link SyncopeConstants#MASTER_DOMAIN} if not set.
     *
     * @return the domain configured for this instance
     */
    public String getDomain() {
        List<String> headerValues = restClientFactory.getHeaders().get(RESTHeaders.DOMAIN);
        return headerValues == null || headerValues.isEmpty()
                ? SyncopeConstants.MASTER_DOMAIN
                : headerValues.get(0);
    }

    /**
     * Creates an instance of the given service class, with configured content type and authentication.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @return service instance of the given reference class
     */
    public <T> T getService(final Class<T> serviceClass) {
        synchronized (restClientFactory) {
            restClientFactory.setServiceClass(serviceClass);
            T serviceInstance = restClientFactory.create(serviceClass);

            Client client = WebClient.client(serviceInstance);
            client.type(mediaType).accept(mediaType);

            ClientConfiguration config = WebClient.getConfig(client);
            config.getRequestContext().put(HEADER_SPLIT_PROPERTY, true);
            config.getRequestContext().put(URLConnectionHTTPConduit.HTTPURL_CONNECTION_METHOD_REFLECTION, true);
            if (useCompression) {
                config.getInInterceptors().add(new GZIPInInterceptor());
                config.getOutInterceptors().add(new GZIPOutInterceptor());
            }

            return serviceInstance;
        }
    }

    @SuppressWarnings("unchecked")
    public Pair<Map<String, Set<String>>, UserTO> self() {
        // Explicitly disable header value split because it interferes with JSON deserialization below
        UserSelfService service = getService(UserSelfService.class);
        WebClient.getConfig(WebClient.client(service)).getRequestContext().put(HEADER_SPLIT_PROPERTY, false);

        Response response = service.read();
        if (response.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
            Exception ex = exceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        try {
            return new ImmutablePair<>(
                    (Map<String, Set<String>>) new ObjectMapper().readValue(
                            response.getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS),
                            new TypeReference<HashMap<String, Set<String>>>() {
                    }),
                    response.readEntity(UserTO.class));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sets the given header on the give service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param key HTTP header key
     * @param values HTTP header values
     * @return given service instance, with given header set
     */
    public <T> T header(final T service, final String key, final Object... values) {
        WebClient.client(service).header(key, values);
        return service;
    }

    /**
     * Sets the {@code Prefer} header on the give service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param preference preference to be set via {@code Prefer} header
     * @return given service instance, with {@code Prefer} header set
     */
    public <T> T prefer(final T service, final Preference preference) {
        return header(service, RESTHeaders.PREFER, preference.toString());
    }

    /**
     * Asks for asynchronous propagation towards external resources with null priority.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param nullPriorityAsync whether asynchronous propagation towards external resources with null priority is
     * requested
     * @return service instance of the given reference class, with related header set
     */
    public <T> T nullPriorityAsync(final T service, final boolean nullPriorityAsync) {
        return header(service, RESTHeaders.NULL_PRIORITY_ASYNC, nullPriorityAsync);
    }

    /**
     * Sets the {@code If-Match} or {@code If-None-Match} header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @param ifNot if true then {@code If-None-Match} is set, {@code If-Match} otherwise
     * @return given service instance, with {@code If-Match} or {@code If-None-Match} set
     */
    private <T> T match(final T service, final EntityTag etag, final boolean ifNot) {
        WebClient.client(service).match(etag, ifNot);
        return service;
    }

    /**
     * Sets the {@code If-Match} header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @return given service instance, with {@code If-Match} set
     */
    public <T> T ifMatch(final T service, final EntityTag etag) {
        return match(service, etag, false);
    }

    /**
     * Sets the {@code If-None-Match} header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @return given service instance, with {@code If-None-Match} set
     */
    public <T> T ifNoneMatch(final T service, final EntityTag etag) {
        return match(service, etag, true);
    }

    /**
     * Fetches {@code ETag} header value from latest service run (if available).
     *
     * @param <T> any service class
     * @param service service class instance
     * @return {@code ETag} header value from latest service run (if available)
     */
    public <T> EntityTag getLatestEntityTag(final T service) {
        return WebClient.client(service).getResponse().getEntityTag();
    }
}
