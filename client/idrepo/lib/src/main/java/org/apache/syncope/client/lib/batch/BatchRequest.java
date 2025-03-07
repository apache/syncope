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
package org.apache.syncope.client.lib.batch;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the Batch request management via CXF Proxy Client.
 */
public class BatchRequest {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRequest.class);

    private final MediaType mediaType;

    private final String jwt;

    private final String address;

    private final List<?> providers;

    private final TLSClientParameters tlsClientParameters;

    private BatchClientFactoryBean bcfb;

    public BatchRequest(
            final MediaType mediaType,
            final String address,
            final List<?> providers,
            final String jwt,
            final TLSClientParameters tlsClientParameters) {

        this.mediaType = mediaType;
        this.jwt = jwt;
        this.address = address;
        this.providers = providers;
        this.tlsClientParameters = tlsClientParameters;
        initBatchClientFactoryBean();
    }

    private void initBatchClientFactoryBean() {
        this.bcfb = new BatchClientFactoryBean();
        this.bcfb.setAddress(address);
        this.bcfb.setProviders(providers);
    }

    public <T> T getService(final Class<T> serviceClass) {
        bcfb.setServiceClass(serviceClass);
        T serviceInstance = bcfb.create(serviceClass);

        Client client = WebClient.client(serviceInstance);
        client.type(mediaType).accept(mediaType);

        return serviceInstance;
    }

    public List<BatchRequestItem> getItems() {
        return bcfb.getBatchRequestItems();
    }

    /**
     * Sends the current request, with items accumulated by invoking methods on proxies obtained via
     * {@link #getService(java.lang.Class)}, to the Batch service, and awaits for synchronous response.
     * It also clears out the accumulated items, in case of reuse of this instance for subsequent requests.
     *
     * @return batch response
     */
    public BatchResponse commit() {
        return commit(false);
    }

    /**
     * Sends the current request, with items accumulated by invoking methods on proxies obtained via
     * {@link #getService(java.lang.Class)}, to the Batch service, and awaits for a synchronous or asynchronous
     * response, depending on the {@code async} parameter.
     * It also clears out the accumulated items, in case of reuse of this instance for subsequent requests.
     *
     * @param async whether asynchronous Batch process is requested, or not
     * @return batch response
     */
    public BatchResponse commit(final boolean async) {
        String boundary = "--batch_" + UUID.randomUUID();

        WebClient webClient = WebClient.create(bcfb.getAddress()).path("batch").
                header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2)));
        if (async) {
            webClient.header(RESTHeaders.PREFER, Preference.RESPOND_ASYNC);
        }
        if (tlsClientParameters != null) {
            ClientConfiguration config = WebClient.getConfig(webClient);
            HTTPConduit httpConduit = (HTTPConduit) config.getConduit();
            httpConduit.setTlsClientParameters(tlsClientParameters);
        }

        String body = BatchPayloadGenerator.generate(bcfb.getBatchRequestItems(), boundary);
        LOG.debug("Batch request body:\n{}", body);

        initBatchClientFactoryBean();

        return new BatchResponse(boundary, jwt, tlsClientParameters, webClient.post(body));
    }
}
