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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.OrderByClauseBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.UserSelfService;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * {@link SyncopeClientFactoryBean}.
 */
public class SyncopeClient {

    private final MediaType mediaType;

    private final RestClientFactoryBean restClientFactory;

    private final RestClientExceptionMapper exceptionMapper;

    private final String username;

    private final String password;

    private final boolean useCompression;

    public SyncopeClient(
            final MediaType mediaType,
            final RestClientFactoryBean restClientFactory,
            final RestClientExceptionMapper exceptionMapper,
            final String username, final String password,
            final boolean useCompression) {

        this.mediaType = mediaType;
        this.restClientFactory = restClientFactory;
        this.exceptionMapper = exceptionMapper;
        this.username = username;
        this.password = password;
        this.useCompression = useCompression;
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
     * Creates an instance of the given service class, with configured content type and authentication.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @return service instance of the given reference class
     */
    public <T> T getService(final Class<T> serviceClass) {
        synchronized (restClientFactory) {
            return restClientFactory.createServiceInstance(serviceClass, mediaType, username, password, useCompression);
        }
    }

    @SuppressWarnings("unchecked")
    public Pair<Map<String, Set<String>>, UserTO> self() {
        // Explicitly disable header value split because it interferes with JSON deserialization below
        UserSelfService service = getService(UserSelfService.class);
        WebClient.getConfig(WebClient.client(service)).
                getRequestContext().put(RestClientFactoryBean.HEADER_SPLIT_PROPERTY, false);

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
     * Creates an instance of the given service class and sets the given header.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param key HTTP header key
     * @param values HTTP header values
     * @return service instance of the given reference class, with given header set
     */
    public <T> T header(final Class<T> serviceClass, final String key, final Object... values) {
        return header(getService(serviceClass), key, values);
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
     * Creates an instance of the given service class, with {@code Prefer} header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param preference preference to be set via {@code Prefer} header
     * @return service instance of the given reference class, with {@code Prefer} header set
     */
    public <T> T prefer(final Class<T> serviceClass, final Preference preference) {
        return header(serviceClass, RESTHeaders.PREFER, preference.toString());
    }

    /**
     * Asks for asynchronous propagation towards external resources with null priority.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param nullPriorityAsync whether asynchronous propagation towards external resources with null priority is
     * requested
     * @return service instance of the given reference class, with related header set
     */
    public <T> T nullPriorityAsync(final Class<T> serviceClass, final boolean nullPriorityAsync) {
        return header(serviceClass, RESTHeaders.NULL_PRIORITY_ASYNC, nullPriorityAsync);
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
     * Creates an instance of the given service class, with {@code If-Match} header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param etag ETag value
     * @return given service instance, with {@code If-Match} set
     */
    public <T> T ifMatch(final Class<T> serviceClass, final EntityTag etag) {
        return match(getService(serviceClass), etag, false);
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
     * Creates an instance of the given service class, with {@code If-None-Match} header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param etag ETag value
     * @return given service instance, with {@code If-None-Match} set
     */
    public <T> T ifNoneMatch(final Class<T> serviceClass, final EntityTag etag) {
        return match(getService(serviceClass), etag, true);
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
