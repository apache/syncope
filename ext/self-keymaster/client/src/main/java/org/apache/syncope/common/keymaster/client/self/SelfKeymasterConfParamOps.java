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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.ext.self.keymaster.api.service.ConfParamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfKeymasterConfParamOps implements ConfParamOps {

    private static final Logger LOG = LoggerFactory.getLogger(ConfParamOps.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JAXRSClientFactoryBean clientFactory;

    public SelfKeymasterConfParamOps(final JAXRSClientFactoryBean clientFactory) {
        this.clientFactory = clientFactory;
    }

    private ConfParamService client(final String domain) {
        ConfParamService service;
        synchronized (clientFactory) {
            clientFactory.setServiceClass(ConfParamService.class);
            clientFactory.setHeaders(Map.of(RESTHeaders.DOMAIN, domain));
            service = clientFactory.create(ConfParamService.class);
        }

        Client client = WebClient.client(service);
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        ClientConfiguration config = WebClient.getConfig(client);
        config.getInInterceptors().add(new GZIPInInterceptor());
        config.getOutInterceptors().add(new GZIPOutInterceptor());

        return service;
    }

    @Override
    public Map<String, Object> list(final String domain) {
        return client(domain).list();
    }

    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        Response response = client(domain).get(key);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            return defaultValue;
        }
        try {
            return MAPPER.readValue(response.readEntity(InputStream.class), reference);
        } catch (IOException e) {
            LOG.error("Could not deserialize response", e);
            return defaultValue;
        }
    }

    @Override
    public <T> void set(final String domain, final String key, final T value) {
        if (value == null) {
            remove(domain, key);
        } else {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MAPPER.writeValue(baos, value);

                client(domain).set(key, new ByteArrayInputStream(baos.toByteArray()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not serialize " + value, e);
            }
        }
    }

    @Override
    public void remove(final String domain, final String key) {
        client(domain).remove(key);
    }
}
