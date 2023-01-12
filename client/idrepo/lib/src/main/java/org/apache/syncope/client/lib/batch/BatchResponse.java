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
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the Batch response management via CXF Proxy Client.
 */
public class BatchResponse {

    private static final Logger LOG = LoggerFactory.getLogger(BatchResponse.class);

    /**
     * If asynchronous processing was requested, queries the monitor URI.
     *
     * @param monitor monitor URI
     * @param jwt authorization JWT
     * @param boundary mutipart / mixed boundary
     * @param tlsClientParameters (optional) TLS client parameters
     *
     * @return the last Response received from the Batch service
     */
    public static Response poll(
            final URI monitor,
            final String jwt,
            final String boundary,
            final TLSClientParameters tlsClientParameters) {

        WebClient webClient = WebClient.create(monitor).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).
                type(RESTHeaders.multipartMixedWith(boundary.substring(2)));
        if (tlsClientParameters != null) {
            ClientConfiguration config = WebClient.getConfig(webClient);
            HTTPConduit httpConduit = (HTTPConduit) config.getConduit();
            httpConduit.setTlsClientParameters(tlsClientParameters);
        }

        return webClient.get();
    }

    /**
     * Parses the given Response into a list of {@link BatchResponseItem}s.
     *
     * @param response response to extract items from
     * @return the Batch Response parsed as list of {@link BatchResponseItem}s
     * @throws IOException if there are issues when reading the response body
     */
    public static List<BatchResponseItem> getItems(final Response response) throws IOException {
        String body = IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8.name());
        LOG.debug("Batch response body:\n{}", body);

        return BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem());
    }

    private final String boundary;

    private final String jwt;

    private final URI monitor;

    private final TLSClientParameters tlsClientParameters;

    private Response response;

    public BatchResponse(
            final String boundary,
            final String jwt,
            final TLSClientParameters tlsClientParameters,
            final Response response) {

        this.boundary = boundary;
        this.jwt = jwt;
        this.tlsClientParameters = tlsClientParameters;
        this.monitor = response.getLocation();
        this.response = response;
    }

    public String getBoundary() {
        return boundary;
    }

    public URI getMonitor() {
        return monitor;
    }

    /**
     * Gives the last Response received from the Batch service.
     *
     * @return the last Response received from the Batch service
     */
    public Response getResponse() {
        return response;
    }

    /**
     * If asynchronous processing was requested, queries the monitor URI.
     *
     * @return the last Response received from the Batch service
     */
    public Response poll() {
        response = poll(monitor, jwt, boundary, tlsClientParameters);
        return response;
    }

    /**
     * Parses the latest Response received into a list of {@link BatchResponseItem}s.
     *
     * @return the Batch Response parsed as list of {@link BatchResponseItem}s
     * @throws IOException if there are issues when reading the response body
     */
    public List<BatchResponseItem> getItems() throws IOException {
        return getItems(response);
    }
}
