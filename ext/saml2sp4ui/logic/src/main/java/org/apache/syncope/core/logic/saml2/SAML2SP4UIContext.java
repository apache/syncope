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
package org.apache.syncope.core.logic.saml2;

import jakarta.ws.rs.HttpMethod;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.saml2.SAML2Constants;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;

public class SAML2SP4UIContext implements WebContext {

    private final String bindingType;

    private final SAML2Response saml2Response;

    public SAML2SP4UIContext(final String bindingType, final SAML2Response saml2Response) {
        this.bindingType = bindingType;
        this.saml2Response = saml2Response;
    }

    @Override
    public Optional<String> getQueryString() {
        return Optional.empty();
    }

    @Override
    public String getRequestMethod() {
        return SAML2BindingType.POST.getUri().equals(bindingType)
                ? HttpMethod.POST
                : HttpMethod.GET;
    }

    @Override
    public Optional<String> getRequestParameter(final String name) {
        return switch (name) {
            case SAML2Constants.SAML_RESPONSE ->
                Optional.ofNullable(saml2Response.getSamlResponse());
            case SAML2Constants.RELAY_STATE ->
                Optional.ofNullable(saml2Response.getRelayState());
            default ->
                Optional.empty();
        };
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        return Map.of();
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
        return -1;
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
