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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
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

    private String basePath;

    private List<BatchRequestItem> batchRequestItems;

    private DestinationRegistry destinationRegistry;

    private ServletConfig servletConfig;

    private HttpServletRequest servletRequest;

    private Authentication authentication;

    public void setBoundary(final String boundary) {
        this.boundary = boundary;
    }

    public void setBasePath(final String basePath) {
        this.basePath = basePath;
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
            LOG.debug("Batch item:\n{}", reqItem);

            AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(reqItem.getRequestURI(), true);
            if (dest == null) {
                dest = destinationRegistry.checkRestfulRequest(reqItem.getRequestURI());
            }
            LOG.debug("Destination found for {}: {}", reqItem.getRequestURI(), dest);

            if (dest == null) {
                BatchResponseItem resItem = new BatchResponseItem();
                resItem.setStatus(404);
                batchResponseItems.add(resItem);
            } else {
                BatchItemRequest request = new BatchItemRequest(basePath, servletRequest, reqItem);
                BatchItemResponse response = new BatchItemResponse();
                try {
                    dest.invoke(servletConfig, servletConfig.getServletContext(), request, response);
                    LOG.debug("Returned:\nstatus: {}\nheaders: {}\nbody:\n{}", response.getStatus(),
                            response.getHeaders(), new String(response.getUnderlyingOutputStream().toByteArray()));

                    BatchResponseItem resItem = new BatchResponseItem();
                    resItem.setStatus(response.getStatus());
                    resItem.setHeaders(response.getHeaders());
                    String output = new String(response.getUnderlyingOutputStream().toByteArray());
                    if (output.length() > 0) {
                        resItem.setContent(output);
                    }
                    batchResponseItems.add(resItem);
                } catch (IOException e) {
                    LOG.error("Invocation of {} failed", dest.getPath(), e);

                    BatchResponseItem resItem = new BatchResponseItem();
                    resItem.setStatus(404);
                    batchResponseItems.add(resItem);
                }
            }
        });

        String results = BatchPayloadGenerator.generate(batchResponseItems, JAXRSService.DOUBLE_DASH + boundary);

        Batch batch = batchDAO.find(boundary);
        if (batch == null) {
            LOG.error("Could not find batch {}, cannot save results hence reporting here:\n{}", boundary, results);
        } else {
            batch.setResults(results);
            batchDAO.save(batch);
        }
    }
}
