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
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.ConnObjectTOFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.OrderByClauseBuilder;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.ExecutableService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * {@link SyncopeClientFactoryBean}.
 */
public class SyncopeClient {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeClient.class);

    protected static final String HEADER_SPLIT_PROPERTY = "org.apache.cxf.http.header.split";

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final MediaType mediaType;

    protected final JAXRSClientFactoryBean restClientFactory;

    protected final RestClientExceptionMapper exceptionMapper;

    protected final boolean useCompression;

    protected final HTTPClientPolicy httpClientPolicy;

    protected final TLSClientParameters tlsClientParameters;

    public SyncopeClient(
            final MediaType mediaType,
            final JAXRSClientFactoryBean restClientFactory,
            final RestClientExceptionMapper exceptionMapper,
            final AuthenticationHandler authHandler,
            final boolean useCompression,
            final HTTPClientPolicy httpClientPolicy,
            final TLSClientParameters tlsClientParameters) {

        this.mediaType = mediaType;
        this.restClientFactory = restClientFactory;
        if (this.restClientFactory.getHeaders() == null) {
            this.restClientFactory.setHeaders(new HashMap<>());
        }
        this.exceptionMapper = exceptionMapper;
        this.useCompression = useCompression;
        this.httpClientPolicy = httpClientPolicy;
        this.tlsClientParameters = tlsClientParameters;

        init(authHandler);
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
     * @param authHandler authentication handler
     */
    protected void init(final AuthenticationHandler authHandler) {
        cleanup();

        if (authHandler instanceof final AnonymousAuthenticationHandler anonymousAuthenticationHandler) {
            restClientFactory.setUsername(anonymousAuthenticationHandler.getUsername());
            restClientFactory.setPassword(anonymousAuthenticationHandler.getPassword());
        } else if (authHandler instanceof final BasicAuthenticationHandler basicAuthenticationHandler) {
            restClientFactory.setUsername(basicAuthenticationHandler.getUsername());
            restClientFactory.setPassword(basicAuthenticationHandler.getPassword());

            String jwt = getService(AccessTokenService.class).login().getHeaderString(RESTHeaders.TOKEN);
            restClientFactory.getHeaders().put(HttpHeaders.AUTHORIZATION, List.of("Bearer " + jwt));

            restClientFactory.setUsername(null);
            restClientFactory.setPassword(null);
        } else if (authHandler instanceof final JWTAuthenticationHandler jwtAuthenticationHandler) {
            restClientFactory.getHeaders().put(
                    HttpHeaders.AUTHORIZATION,
                    List.of("Bearer " + jwtAuthenticationHandler.getJwt()));
        }
    }

    protected void cleanup() {
        restClientFactory.getHeaders().remove(HttpHeaders.AUTHORIZATION);
        restClientFactory.getHeaders().remove(RESTHeaders.DELEGATED_BY);
        restClientFactory.setUsername(null);
        restClientFactory.setPassword(null);
    }

    /**
     * Gives the base address for REST calls.
     *
     * @return the base address for REST calls
     */
    public String getAddress() {
        return restClientFactory.getAddress();
    }

    /**
     * Requests to invoke services as delegating user.
     *
     * @param delegating delegating username
     * @return this instance, for fluent usage
     */
    public SyncopeClient delegatedBy(final String delegating) {
        if (delegating == null) {
            restClientFactory.getHeaders().remove(RESTHeaders.DELEGATED_BY);
        } else {
            restClientFactory.getHeaders().put(RESTHeaders.DELEGATED_BY, List.of(delegating));
        }
        return this;
    }

    /**
     * Attempts to extend the lifespan of the JWT currently in use.
     */
    public void refresh() {
        String jwt = getService(AccessTokenService.class).refresh().getHeaderString(RESTHeaders.TOKEN);
        restClientFactory.getHeaders().put(HttpHeaders.AUTHORIZATION, List.of("Bearer " + jwt));
    }

    /**
     * Invalidates the JWT currently in use.
     */
    public void logout() {
        try {
            getService(AccessTokenService.class).logout();
        } catch (Exception e) {
            LOG.error("While logging out, cleaning up anyway", e);
        }
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
     * Returns a new instance of {@link ConnObjectTOFiqlSearchConditionBuilder}, for assisted building of FIQL queries.
     *
     * @return default instance of {@link ConnObjectTOFiqlSearchConditionBuilder}
     */
    public static ConnObjectTOFiqlSearchConditionBuilder getConnObjectTOFiqlSearchConditionBuilder() {
        return new ConnObjectTOFiqlSearchConditionBuilder();
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
     * in all requests. It can be null (in case {@link AnonymousAuthenticationHandler} was used).
     *
     * @return the JWT in used by this instance
     */
    public String getJWT() {
        List<String> headerValues = restClientFactory.getHeaders().get(HttpHeaders.AUTHORIZATION);
        String header = headerValues == null || headerValues.isEmpty()
                ? null
                : headerValues.getFirst();
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
                : headerValues.getFirst();
    }

    /**
     * Creates an instance of the given service class, with configured content type and authentication.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @return service instance of the given reference class
     */
    public <T> T getService(final Class<T> serviceClass) {
        T serviceInstance;
        synchronized (restClientFactory) {
            restClientFactory.setServiceClass(serviceClass);
            serviceInstance = restClientFactory.create(serviceClass);
        }

        Client client = WebClient.client(serviceInstance);
        client.type(mediaType).accept(mediaType);
        if (serviceInstance instanceof AnyService || serviceInstance instanceof ExecutableService) {
            client.accept(RESTHeaders.MULTIPART_MIXED);
        }

        ClientConfiguration config = WebClient.getConfig(client);
        config.getRequestContext().put(HEADER_SPLIT_PROPERTY, true);
        config.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
        if (useCompression) {
            config.getInInterceptors().add(new GZIPInInterceptor());
            config.getOutInterceptors().add(new GZIPOutInterceptor());
        }

        HTTPConduit httpConduit = (HTTPConduit) config.getConduit();
        Optional.ofNullable(httpClientPolicy).ifPresent(httpConduit::setClient);
        Optional.ofNullable(tlsClientParameters).ifPresent(httpConduit::setTlsClientParameters);

        return serviceInstance;
    }

    public Triple<Map<String, Set<String>>, List<String>, UserTO> self() {
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
            return Triple.of(
                    MAPPER.readValue(
                            response.getHeaderString(RESTHeaders.OWNED_ENTITLEMENTS), new TypeReference<>() {
                    }),
                    MAPPER.readValue(
                            response.getHeaderString(RESTHeaders.DELEGATIONS), new TypeReference<>() {
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
    public static <T> T header(final T service, final String key, final Object... values) {
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
    public static <T> T prefer(final T service, final Preference preference) {
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
    public static <T> T nullPriorityAsync(final T service, final boolean nullPriorityAsync) {
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
    protected static <T> T match(final T service, final EntityTag etag, final boolean ifNot) {
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
    public static <T> T ifMatch(final T service, final EntityTag etag) {
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
    public static <T> T ifNoneMatch(final T service, final EntityTag etag) {
        return match(service, etag, true);
    }

    /**
     * Fetches {@code ETag} header value from latest service run (if available).
     *
     * @param <T> any service class
     * @param service service class instance
     * @return {@code ETag} header value from latest service run (if available)
     */
    public static <T> EntityTag getLatestEntityTag(final T service) {
        return WebClient.client(service).getResponse().getEntityTag();
    }

    /**
     * Initiates a new Batch request.
     *
     * The typical operation flow is:
     * <pre>
     * BatchRequest batchRequest = syncopeClient.batch();
     * batchRequest.getService(UserService.class).create(...);
     * batchRequest.getService(UserService.class).update(...);
     * batchRequest.getService(GroupService.class).update(...);
     * batchRequest.getService(GroupService.class).delete(...);
     * ...
     * BatchResponse batchResponse = batchRequest().commit();
     * List&lt;BatchResponseItem&gt; items = batchResponse.getItems()
     * </pre>
     *
     * @return empty Batch request
     */
    public BatchRequest batch() {
        return new BatchRequest(
                mediaType,
                restClientFactory.getAddress(),
                restClientFactory.getProviders(),
                getJWT(),
                tlsClientParameters);
    }
}
