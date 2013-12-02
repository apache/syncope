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
package org.apache.syncope.client;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.rest.RestClientFactoryBean;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * <tt>SyncopeClientFactoryBean</tt>.
 *
 * @see SyncopeClientFactoryBean
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
     * Checks whether self-registration is allowed by calling <tt>UserSelfService</tt>'s options.
     *
     * @return whether self-registration is allowed
     * @see UserSelfService#getOptions()
     */
    public boolean isSelfRegistrationAllowed() {
        return Boolean.valueOf(restClientFactory.createServiceInstance(UserSelfService.class, mediaType, null, null).
                getOptions().getHeaderString(RESTHeaders.SELFREGISTRATION_ALLOWED));
    }

    /**
     * Checks whether Activiti workflow is enabled for users / roles, by calling <tt>WorkflowService</tt>'s options.
     *
     * @param attributableType user / role
     * @return whether Activiti workflow is enabled for given attributable type
     * @see WorkflowService#getOptions(org.apache.syncope.common.types.AttributableType)
     */
    public boolean isActivitiEnabledFor(final AttributableType attributableType) {
        Response options = getService(WorkflowService.class).getOptions(attributableType);

        boolean result;
        switch (attributableType) {
            case USER:
                result = Boolean.valueOf(options.getHeaderString(RESTHeaders.ACTIVITI_USER_ENABLED));
                break;

            case ROLE:
                result = Boolean.valueOf(options.getHeaderString(RESTHeaders.ACTIVITI_ROLE_ENABLED));
                break;

            case MEMBERSHIP:
            default:
                result = false;
        }

        return result;
    }

    /**
     * Fetches <tt>Etag</tt> header value from latest service run (if available).
     *
     * @param <T> any service class
     * @param service service class instance
     * @return <tt>Etag</tt> header value from latest service run (if available)
     */
    public <T> EntityTag getLatestEntityTag(final T service) {
        return WebClient.client(service).getResponse().getEntityTag();
    }
}
