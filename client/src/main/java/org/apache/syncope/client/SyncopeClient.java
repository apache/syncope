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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.rest.RestClientFactoryBean;

/**
 * Entry point for client access to all REST services exposed by Syncope core; obtain instances via
 * <tt>SyncopeClientFactoryBean</tt>.
 *
 * @see SyncopeClientFactoryBean
 */
public class SyncopeClient {

    private final RestClientFactoryBean restClientFactory;

    private final String username;

    private final String password;

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<Class<?>, Object>();

    public SyncopeClient(final RestClientFactoryBean restClientFactory, final String username, final String password) {
        this.restClientFactory = restClientFactory;
        this.username = username;
        this.password = password;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(final Class<T> serviceClass) {
        if (!services.containsKey(serviceClass)) {
            services.put(serviceClass, restClientFactory.createServiceInstance(serviceClass, username, password));
        }
        return (T) services.get(serviceClass);
    }

    public <T> T getObject(final URI location, final Class<?> serviceClass, final Class<T> resultClass) {
        WebClient webClient = WebClient.fromClient(WebClient.client(getService(serviceClass)));
        webClient.accept(restClientFactory.getContentType()).to(location.toASCIIString(), false);

        return webClient.get(resultClass);
    }
}
