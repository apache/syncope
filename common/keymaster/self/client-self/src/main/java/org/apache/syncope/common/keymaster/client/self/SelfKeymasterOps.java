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
package org.apache.syncope.common.keymaster.client.self;

import jakarta.ws.rs.client.CompletionStageRxInvoker;
import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

abstract class SelfKeymasterOps {

    protected final Map<Class<?>, Object> services = Collections.synchronizedMap(new HashMap<>());

    private final JAXRSClientFactoryBean clientFactory;

    protected HTTPClientPolicy httpClientPolicy;

    protected SelfKeymasterOps(final JAXRSClientFactoryBean clientFactory) {
        this.clientFactory = clientFactory;

        httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnection(ConnectionType.CLOSE);
    }

    @SuppressWarnings("unchecked")
    public <T> T client(final Class<T> serviceClass, final Map<String, String> headers) {
        T service;
        if (services.containsKey(serviceClass)) {
            service = (T) services.get(serviceClass);
        } else {
            synchronized (clientFactory) {
                clientFactory.setServiceClass(serviceClass);
                clientFactory.setHeaders(headers);
                service = clientFactory.create(serviceClass);
            }
            services.put(serviceClass, service);

            Client client = WebClient.client(service);
            client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

            ClientConfiguration config = WebClient.getConfig(client);
            config.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
            config.getInInterceptors().add(new GZIPInInterceptor());
            config.getOutInterceptors().add(new GZIPOutInterceptor());

            HTTPConduit httpConduit = (HTTPConduit) config.getConduit();
            httpConduit.setClient(httpClientPolicy);
        }

        return service;
    }

    protected CompletionStageRxInvoker rx(final String path) {
        synchronized (clientFactory) {
            String original = clientFactory.getAddress();
            clientFactory.setAddress(StringUtils.removeEnd(original, "/") + StringUtils.prependIfMissing(path, "/"));

            try {
                WebClient client = clientFactory.createWebClient().
                        type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
                return client.rx();
            } finally {
                clientFactory.setAddress(original);
            }
        }
    }
}
