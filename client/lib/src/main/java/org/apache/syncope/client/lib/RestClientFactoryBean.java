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

import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;

/**
 * Provides shortcuts for creating JAX-RS service instances via CXF's {@link JAXRSClientFactoryBean}.
 */
public class RestClientFactoryBean extends JAXRSClientFactoryBean {

    public static final String HEADER_SPLIT_PROPERTY = "org.apache.cxf.http.header.split";

    /**
     * Creates an anonymous instance of the given service class, for the given content type.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param mediaType XML or JSON are supported
     * @return anonymous service instance of the given reference class
     */
    public <T> T createServiceInstance(final Class<T> serviceClass, final MediaType mediaType) {
        return createServiceInstance(serviceClass, mediaType, null, null, false);
    }

    /**
     * Creates an authenticated instance of the given service class, for the given content type.
     *
     * @param <T> any service class
     * @param serviceClass service class reference
     * @param mediaType XML or JSON are supported
     * @param username username for REST authentication
     * @param password password for REST authentication
     * @param useCompression whether transparent gzip <tt>Content-Encoding</tt> handling is to be enabled
     * @return anonymous service instance of the given reference class
     */
    public <T> T createServiceInstance(
            final Class<T> serviceClass,
            final MediaType mediaType,
            final String username,
            final String password,
            final boolean useCompression) {

        if (StringUtils.isNotBlank(username)) {
            setUsername(username);
        }
        if (StringUtils.isNotBlank(password)) {
            setPassword(password);
        }

        setServiceClass(serviceClass);
        T serviceInstance = create(serviceClass);

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
