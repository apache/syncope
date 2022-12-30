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
package org.apache.syncope.core.rest.cxf.batch;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.cxf.helpers.HttpHeaderHelper;

public class CommonBatchHttpServletRequest {

    private final HttpServletRequest servletRequest;

    private final String serverName;

    private final int serverPort;

    private final String contextPath;

    private final String servletPath;

    private final String pathInfo;

    private final Principal userPrincipal;

    private final String characterEncoding;

    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Map<String, Object> attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public CommonBatchHttpServletRequest(final HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;

        serverName = servletRequest.getServerName();
        serverPort = servletRequest.getServerPort();
        contextPath = servletRequest.getContextPath();
        servletPath = servletRequest.getServletPath();
        pathInfo = servletRequest.getPathInfo();
        userPrincipal = servletRequest.getUserPrincipal();
        characterEncoding = servletRequest.getCharacterEncoding();

        for (Enumeration<String> ehn = servletRequest.getHeaderNames(); ehn.hasMoreElements();) {
            String fname = ehn.nextElement();
            String mappedName = HttpHeaderHelper.getHeaderKey(fname);

            List<String> values = Optional.ofNullable(headers.get(mappedName)).orElseGet(() -> {
                List<String> v = new ArrayList<>();
                headers.put(mappedName, v);
                return v;
            });

            for (Enumeration<String> eh = servletRequest.getHeaders(fname); eh.hasMoreElements();) {
                String value = eh.nextElement();
                if (HttpHeaders.ACCEPT.equals(mappedName) && !values.isEmpty()) {
                    //ensure we collapse Accept into first line
                    String firstAccept = values.get(0);
                    firstAccept = firstAccept + ", " + value;
                    values.set(0, firstAccept);
                }
                values.add(value);
            }
        }

        for (Enumeration<String> ea = servletRequest.getAttributeNames(); ea.hasMoreElements();) {
            String aname = ea.nextElement();
            attributes.put(aname, servletRequest.getAttribute(aname));
        }
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public String getServerName() {
        return serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getServletPath() {
        return servletPath;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    public String getHeader(final String name) {
        return Optional.ofNullable(headers.get(name)).
                filter(v -> !v.isEmpty()).
                map(v -> v.get(0)).
                orElse(null);
    }

    public Enumeration<String> getHeaders(final String name) {
        return headers.containsKey(name)
                ? Collections.enumeration(headers.get(name))
                : Collections.emptyEnumeration();
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    public void setAttribute(final String name, final Object o) {
        attributes.put(name, o);
    }
}
