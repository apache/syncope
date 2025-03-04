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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class BatchItemRequest extends HttpServletRequestWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BatchItemRequest.class);

    private final String scheme;

    private final String serverName;

    private final int serverPort;

    private final String contextPath;

    private final String servletPath;

    private final String pathInfo;

    private final String characterEncoding;

    private final String baseURI;

    private final BatchRequestItem batchItem;

    private final ServletInputStream inputStream;

    private final Map<String, Object> attributes = new HashMap<>();

    public BatchItemRequest(
            final String scheme,
            final String serverName,
            final int serverPort,
            final String contextPath,
            final String servletPath,
            final String pathInfo,
            final String characterEncoding,
            final String baseURI,
            final HttpServletRequest request,
            final BatchRequestItem batchItem) {

        super(request);
        this.scheme = scheme;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.characterEncoding = characterEncoding;
        this.baseURI = baseURI;
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
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getMethod() {
        return batchItem.getMethod();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(baseURI).append(getRequestURI());
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
                ? batchItem.getHeaders().get(HttpHeaders.CONTENT_TYPE).getFirst().toString()
                : MediaType.ALL_VALUE;
    }

    @Override
    public int getContentLength() {
        int contentLength = 0;
        if (batchItem.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)) {
            try {
                contentLength = Integer.parseInt(
                        batchItem.getHeaders().get(HttpHeaders.CONTENT_LENGTH).getFirst().toString());
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
                ? batchItem.getHeaders().get(name).getFirst().toString()
                : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                ? MediaType.ALL_VALUE
                : null;
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return batchItem.getHeaders().containsKey(name)
                ? Collections.enumeration(batchItem.getHeaders().get(name).stream().
                        map(Object::toString).collect(Collectors.toList()))
                : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                ? Collections.enumeration(List.of(MediaType.ALL_VALUE))
                : Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(batchItem.getHeaders().keySet());
        names.add(HttpHeaders.CONTENT_TYPE);
        names.add(HttpHeaders.ACCEPT);
        return Collections.enumeration(names);
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public ServletInputStream getInputStream() {
        return inputStream;
    }
}
