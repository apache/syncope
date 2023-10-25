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
package org.apache.syncope.core.logic.oidc;

import jakarta.ws.rs.HttpMethod;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;

public class OIDCC4UIContext implements WebContext {

    @Override
    public Optional<String> getQueryString() {
        return Optional.empty();
    }

    @Override
    public String getRequestMethod() {
        return HttpMethod.GET;
    }

    @Override
    public Optional<String> getRequestParameter(final String name) {
        return Optional.empty();
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> getRequestAttribute(final String name) {
        return Optional.empty();
    }

    @Override
    public void setRequestAttribute(final String name, final Object value) {
        // nothing to do
    }

    @Override
    public Optional<String> getRequestHeader(final String name) {
        return Optional.empty();
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public void setResponseHeader(final String name, final String value) {
        // nothing to do
    }

    @Override
    public Optional<String> getResponseHeader(final String s) {
        return Optional.empty();
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getFullRequestURL() {
        return null;
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        return Set.of();
    }

    @Override
    public void addResponseCookie(final Cookie cookie) {
        // nothing to do
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void setResponseContentType(final String content) {
        // nothing to do
    }
}
