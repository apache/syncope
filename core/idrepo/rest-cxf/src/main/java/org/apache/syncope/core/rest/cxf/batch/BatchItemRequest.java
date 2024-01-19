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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class BatchItemRequest extends HttpServletRequestWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BatchItemRequest.class);

    private final CommonBatchHttpServletRequest commonRequest;

    private final String basePath;

    private final BatchRequestItem batchItem;

    private final ServletInputStream inputStream;

    public BatchItemRequest(
            final CommonBatchHttpServletRequest commonRequest,
            final String basePath,
            final BatchRequestItem batchItem) {

        super(commonRequest.getServletRequest());
        this.commonRequest = commonRequest;
        this.basePath = basePath;
        this.batchItem = batchItem;
        this.inputStream = new ServletInputStream() {

            private final ByteArrayInputStream bais = new ByteArrayInputStream(batchItem.getContent().getBytes());

            private boolean isFinished = false;

            private boolean isReady = true;

            @Override
            public boolean isFinished() {
                return isFinished;
            }

            @Override
            public boolean isReady() {
                return isReady;
            }

            @Override
            public void setReadListener(final ReadListener readListener) {
                // nope
            }

            @Override
            public int read() {
                isFinished = true;
                isReady = false;
                return bais.read();
            }
        };
    }

    @Override
    public String getContextPath() {
        return commonRequest.getContextPath();
    }

    @Override
    public String getServletPath() {
        return commonRequest.getServletPath();
    }

    @Override
    public String getPathInfo() {
        return commonRequest.getPathInfo();
    }

    @Override
    public Principal getUserPrincipal() {
        return commonRequest.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(final String role) {
        return false;
    }

    @Override
    public String getMethod() {
        return batchItem.getMethod();
    }

    @Override
    public String getServerName() {
        return commonRequest.getServerName();
    }

    @Override
    public int getServerPort() {
        return commonRequest.getServerPort();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(basePath).append(getRequestURI());
    }

    @Override
    public String getRequestURI() {
        return batchItem.getRequestURI();
    }

    @Override
    public String getQueryString() {
        return batchItem.getQueryString();
    }

    @Override
    public String getContentType() {
        return batchItem.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)
                ? batchItem.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0).toString()
                : MediaType.ALL_VALUE;
    }

    @Override
    public int getContentLength() {
        int contentLength = 0;
        if (batchItem.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)) {
            try {
                contentLength = Integer.parseInt(
                        batchItem.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0).toString());
            } catch (NumberFormatException e) {
                LOG.error("Invalid value found for {}: {}",
                        HttpHeaders.CONTENT_LENGTH, batchItem.getHeaders().get(HttpHeaders.CONTENT_LENGTH), e);
            }
        }
        return contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    @Override
    public String getHeader(final String name) {
        return batchItem.getHeaders().containsKey(name)
                ? batchItem.getHeaders().get(name).get(0).toString()
                : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                ? MediaType.ALL_VALUE
                : commonRequest.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return batchItem.getHeaders().containsKey(name)
                ? Collections.enumeration(batchItem.getHeaders().get(name).stream().
                        map(Object::toString).toList())
                : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                ? Collections.enumeration(List.of(MediaType.ALL_VALUE))
                : commonRequest.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return commonRequest.getHeaderNames();
    }

    @Override
    public Object getAttribute(final String name) {
        return commonRequest.getAttribute(name);
    }

    @Override
    public void setAttribute(final String name, final Object o) {
        commonRequest.setAttribute(name, o);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return commonRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return commonRequest.getCharacterEncoding();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }
}
