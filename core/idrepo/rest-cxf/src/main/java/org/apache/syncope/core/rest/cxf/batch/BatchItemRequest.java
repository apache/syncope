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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class BatchItemRequest extends HttpServletRequestWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(BatchItemRequest.class);

    private final String basePath;

    private final BatchRequestItem batchItem;

    private final ServletInputStream inputStream;

    public BatchItemRequest(
            final String basePath,
            final HttpServletRequest request,
            final BatchRequestItem batchItem) {

        super(request);
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
    public String getMethod() {
        return batchItem.getMethod();
    }

    @Override
    public String getServerName() {
        try {
            return super.getServerName();
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return Optional.ofNullable(getHeader(HttpHeaders.HOST)).
                    map(host -> StringUtils.substringBefore(host, ":")).orElse(null);
        }
    }

    @Override
    public int getServerPort() {
        try {
            return super.getServerPort();
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return Optional.ofNullable(getHeader(HttpHeaders.HOST)).
                    map(host -> NumberUtils.toInt(StringUtils.substringAfter(host, ":"))).orElse(0);
        }
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
                contentLength = Integer.valueOf(
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
        try {
            return batchItem.getHeaders().containsKey(name)
                    ? batchItem.getHeaders().get(name).get(0).toString()
                    : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                    ? MediaType.ALL_VALUE
                    : super.getHeader(name);
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return null;
        }
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        try {
            return batchItem.getHeaders().containsKey(name)
                    ? Collections.enumeration(batchItem.getHeaders().get(name).stream().
                            map(Object::toString).collect(Collectors.toList()))
                    : HttpHeaders.CONTENT_TYPE.equals(name) || HttpHeaders.ACCEPT.equals(name)
                    ? Collections.enumeration(List.of(MediaType.ALL_VALUE))
                    : super.getHeaders(name);
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return Collections.emptyEnumeration();
        }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        try {
            return super.getHeaderNames();
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);

            Set<String> names = new HashSet<>(batchItem.getHeaders().keySet());
            names.add(HttpHeaders.CONTENT_TYPE);
            names.add(HttpHeaders.ACCEPT);
            return Collections.enumeration(names);
        }
    }

    @Override
    public Object getAttribute(final String name) {
        try {
            return super.getAttribute(name);
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return null;
        }
    }

    @Override
    public String getCharacterEncoding() {
        try {
            return super.getCharacterEncoding();
        } catch (Exception e) {
            LOG.debug("While delegating to wrapped request", e);
            return StandardCharsets.UTF_8.name();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }
}
