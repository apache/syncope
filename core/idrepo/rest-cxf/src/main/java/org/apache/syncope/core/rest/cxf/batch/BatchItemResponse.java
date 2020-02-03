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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

public class BatchItemResponse implements HttpServletResponse {

    private final Set<Cookie> cookies = new HashSet<>();

    private final Map<String, List<Object>> headers = new HashMap<>();

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private final ServletOutputStream servletOuputStream = new ServletOutputStream() {

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(final WriteListener writeListener) {
            // nope
        }

        @Override
        public void write(final int b) throws IOException {
            baos.write(b);
        }
    };

    private final PrintWriter writer = new PrintWriter(baos);

    private int status;

    private Locale locale;

    public Set<Cookie> getCookies() {
        return cookies;
    }

    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    @Override
    public void addCookie(final Cookie cookie) {
        this.cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(final String name) {
        return headers.containsKey(name);
    }

    @Override
    public void setDateHeader(final String name, final long date) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        } else {
            values.clear();
        }
        values.add(date);
    }

    @Override
    public void addDateHeader(final String name, final long date) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(date);
    }

    @Override
    public void setHeader(final String name, final String value) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        } else {
            values.clear();
        }
        values.add(value);
    }

    @Override
    public void addHeader(final String name, final String value) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);
    }

    @Override
    public void setIntHeader(final String name, final int value) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        } else {
            values.clear();
        }
        values.add(value);
    }

    @Override
    public void addIntHeader(final String name, final int value) {
        List<Object> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);
    }

    @Override
    public String getHeader(final String name) {
        return headers.containsKey(name) ? headers.get(name).get(0).toString() : null;
    }

    @Override
    public Collection<String> getHeaders(final String name) {
        return headers.containsKey(name)
                ? headers.get(name).stream().map(Object::toString).collect(Collectors.toList())
                : List.of();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public String encodeURL(final String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(final String url) {
        return url;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String encodeUrl(final String url) {
        return encodeURL(url);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String encodeRedirectUrl(final String url) {
        return encodeRedirectURL(url);
    }

    @Override
    public void sendError(final int sc, final String msg) throws IOException {
        setStatus(sc);
    }

    @Override
    public void sendError(final int sc) throws IOException {
        setStatus(sc);
    }

    @Override
    public void sendRedirect(final String location) throws IOException {
        setStatus(SC_MOVED_TEMPORARILY);
        setHeader(HttpHeaders.LOCATION, location);
    }

    @Override
    public void setStatus(final int sc) {
        this.status = sc;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setStatus(final int sc, final String sm) {
        setStatus(sc);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }

    public ByteArrayOutputStream getUnderlyingOutputStream() {
        return baos;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return servletOuputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    @Override
    public void setCharacterEncoding(final String charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLength(final int len) {
        setIntHeader(HttpHeaders.CONTENT_LENGTH, len);
    }

    @Override
    public void setContentLengthLong(final long len) {
        setContentLength((int) len);
    }

    @Override
    public void setContentType(final String type) {
        setHeader(HttpHeaders.CONTENT_TYPE, type);
    }

    @Override
    public void setBufferSize(final int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(final Locale loc) {
        this.locale = loc;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}
