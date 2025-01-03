/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.rest.cxf.batch;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class BatchProcess implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcess.class);

    @Autowired
    private BatchDAO batchDAO;

    private String boundary;

    private String scheme;

    private String serverName;

    private int serverPort;

    private String contextPath;

    private String servletPath;

    private String pathInfo;

    private String characterEncoding;

    private String baseURI;

    private List<BatchRequestItem> batchRequestItems;

    private DestinationRegistry destinationRegistry;

    private ServletConfig servletConfig;

    private HttpServletRequest servletRequest;

    private Authentication authentication;

    public void setBoundary(final String boundary) {
        this.boundary = boundary;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public void setServerPort(final int serverPort) {
        this.serverPort = serverPort;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public void setServletPath(final String servletPath) {
        this.servletPath = servletPath;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public void setCharacterEncoding(final String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setBaseURI(final String baseURI) {
        this.baseURI = baseURI;
    }

    public void setBatchRequestItems(final List<BatchRequestItem> batchRequestItems) {
        this.batchRequestItems = batchRequestItems;
    }

    public void setDestinationRegistry(final DestinationRegistry destinationRegistry) {
        this.destinationRegistry = destinationRegistry;
    }

    public void setServletConfig(final ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    public void setServletRequest(final HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public void setAuthentication(final Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public void run() {
        SecurityContextHolder.getContext().setAuthentication(authentication);

        List<BatchResponseItem> batchResponseItems = new ArrayList<>(batchRequestItems.size());

        batchRequestItems.forEach(reqItem -> {
            LOG.debug("Batch Request item:\n{}", reqItem);

            AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(reqItem.getRequestURI(), true);
            if (dest == null) {
                dest = destinationRegistry.checkRestfulRequest(reqItem.getRequestURI());
            }
            LOG.debug("Destination found for {}: {}", reqItem.getRequestURI(), dest);

            BatchResponseItem resItem = new BatchResponseItem();
            batchResponseItems.add(resItem);
            if (dest == null) {
                resItem.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                BatchItemRequest request = new BatchItemRequest(
                        scheme, serverName, serverPort, contextPath, servletPath, pathInfo, characterEncoding,
                        baseURI, servletRequest, reqItem);
                BatchItemResponse response = new BatchItemResponse();
                try {
                    dest.invoke(servletConfig, servletConfig.getServletContext(), request, response);

                    resItem.setStatus(response.getStatus());
                    resItem.setHeaders(response.getHeaders());
                    String output = new String(response.getUnderlyingOutputStream().toByteArray());
                    if (!output.isEmpty()) {
                        resItem.setContent(output);
                    }

                    LOG.debug("Returned:\nstatus: {}\nheaders: {}\nbody:\n{}",
                            response.getStatus(), response.getHeaders(), output);
                } catch (IOException e) {
                    LOG.error("Invocation of {} failed", dest.getPath(), e);

                    resItem.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

            LOG.debug("Batch Response item:\n{}", resItem);
        });

        String results = BatchPayloadGenerator.generate(batchResponseItems, JAXRSService.DOUBLE_DASH + boundary);

        batchDAO.findById(boundary).ifPresentOrElse(
                batch -> {
                    batch.setResults(results);
                    batchDAO.save(batch);
                },
                () -> LOG.error("Could not find batch {}, cannot save results hence reporting here:\n{}",
                        boundary, results));
    }
}
