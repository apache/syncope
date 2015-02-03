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

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.search.OrderByClauseBuilder;
import org.apache.syncope.common.lib.search.RoleFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * {@link SyncopeClientFactoryBean}.
 */
public class SyncopeClient {

    private final MediaType mediaType;

    private final RestClientFactoryBean restClientFactory;

    private final String username;

    private final String password;

    public SyncopeClient(final MediaType mediaType, final RestClientFactoryBean restClientFactory,
            final String username, final String password) {

        this.mediaType = mediaType;
        this.restClientFactory = restClientFactory;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns a new instance of <tt>UserFiqlSearchConditionBuilder</tt>, for assisted building of FIQL queries.
     *
     * @return default instance of <tt>UserFiqlSearchConditionBuilder</tt>
     */
    public static UserFiqlSearchConditionBuilder getUserSearchConditionBuilder() {
        return new UserFiqlSearchConditionBuilder();
    }

    /**
     * Returns a new instance of <tt>RoleFiqlSearchConditionBuilder</tt>, for assisted building of FIQL queries.
     *
     * @return default instance of <tt>RoleFiqlSearchConditionBuilder</tt>
     */
    public static RoleFiqlSearchConditionBuilder getRoleSearchConditionBuilder() {
        return new RoleFiqlSearchConditionBuilder();
    }

    /**
     * Returns a new instance of <tt>OrderByClauseBuilder</tt>, for assisted building of <tt>orderby</tt> clauses.
     *
     * @return default instance of <tt>OrderByClauseBuilder</tt>
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
        return restClientFactory.createServiceInstance(serviceClass, mediaType, username, password);
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
     * Sets the <tt>Prefer</tt> header on the give service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param preference preference to be set via <tt>Prefer</tt> header
     * @return given service instance, with <tt>Prefer</tt> header set
     */
    public <T> T prefer(final T service, final Preference preference) {
        return header(service, RESTHeaders.PREFER, preference.toString());
    }

    /**
     * Creates an instance of the given service class, with <tt>Prefer</tt> header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param preference preference to be set via <tt>Prefer</tt> header
     * @return service instance of the given reference class, with <tt>Prefer</tt> header set
     */
    public <T> T prefer(final Class<T> serviceClass, final Preference preference) {
        return header(serviceClass, RESTHeaders.PREFER, preference.toString());
    }

    /**
     * Sets the <tt>If-Match</tt> or <tt>If-None-Match</tt> header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @param ifNot if true then <tt>If-None-Match</tt> is set, <tt>If-Match</tt> otherwise
     * @return given service instance, with <tt>If-Match</tt> or <tt>If-None-Match</tt> set
     */
    private <T> T match(final T service, final EntityTag etag, final boolean ifNot) {
        WebClient.client(service).match(etag, ifNot);
        return service;
    }

    /**
     * Sets the <tt>If-Match</tt> header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @return given service instance, with <tt>If-Match</tt> set
     */
    public <T> T ifMatch(final T service, final EntityTag etag) {
        return match(service, etag, false);
    }

    /**
     * Creates an instance of the given service class, with <tt>If-Match</tt> header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param etag ETag value
     * @return given service instance, with <tt>If-Match</tt> set
     */
    public <T> T ifMatch(final Class<T> serviceClass, final EntityTag etag) {
        return match(getService(serviceClass), etag, false);
    }

    /**
     * Sets the <tt>If-None-Match</tt> header on the given service instance.
     *
     * @param <T> any service class
     * @param service service class instance
     * @param etag ETag value
     * @return given service instance, with <tt>If-None-Match</tt> set
     */
    public <T> T ifNoneMatch(final T service, final EntityTag etag) {
        return match(service, etag, true);
    }

    /**
     * Creates an instance of the given service class, with <tt>If-None-Match</tt> header set.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param etag ETag value
     * @return given service instance, with <tt>If-None-Match</tt> set
     */
    public <T> T ifNoneMatch(final Class<T> serviceClass, final EntityTag etag) {
        return match(getService(serviceClass), etag, true);
    }

    /**
     * Fetches <tt>ETag</tt> header value from latest service run (if available).
     *
     * @param <T> any service class
     * @param service service class instance
     * @return <tt>ETag</tt> header value from latest service run (if available)
     */
    public <T> EntityTag getLatestEntityTag(final T service) {
        return WebClient.client(service).getResponse().getEntityTag();
    }
}
