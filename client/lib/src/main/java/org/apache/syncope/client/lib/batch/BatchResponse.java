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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
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

    private final String boundary;

    private final String jwt;

    private final URI monitor;

    private Response response;

    public BatchResponse(final String boundary, final String jwt, final Response response) {
        this.boundary = boundary;
        this.jwt = jwt;
        this.monitor = response.getLocation();
        this.response = response;
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
        if (monitor != null) {
            response = WebClient.create(monitor).
                    header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).
                    type(RESTHeaders.multipartMixedWith(boundary.substring(2))).get();
        }

        return response;
    }

    /**
     * Parses the latest Response received into a list of {@link BatchResponseItem}s.
     *
     * @return the Batch Response parsed as list of {@link BatchResponseItem}s
     * @throws IOException if there are issues when reading the response body
     */
    public List<BatchResponseItem> getItems() throws IOException {
        String body = IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8.name());
        LOG.debug("Batch response body:\n{}", body);

        return BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem());
    }
}
