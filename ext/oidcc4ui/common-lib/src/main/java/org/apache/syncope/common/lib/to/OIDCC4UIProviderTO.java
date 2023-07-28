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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;

public class OIDCC4UIProviderTO extends ItemContainer implements EntityTO {

    private static final long serialVersionUID = -1229802774546135794L;

    private String key;

    private String name;

    private String clientID;

    private String clientSecret;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String jwksUri;

    private String issuer;

    private String userinfoEndpoint;

    private String endSessionEndpoint;

    private final List<String> scopes = new ArrayList<>();

    private boolean hasDiscovery;

    private boolean createUnmatching;

    private boolean updateMatching;

    private boolean selfRegUnmatching;

    private UserTO userTemplate;

    private final List<String> actions = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(final String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(final String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(final String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    @JacksonXmlElementWrapper(localName = "scopes")
    @JacksonXmlProperty(localName = "scope")
    public List<String> getScopes() {
        return scopes;
    }

    public UserTO getUserTemplate() {
        return userTemplate;
    }

    public boolean getHasDiscovery() {
        return hasDiscovery;
    }

    public void setHasDiscovery(final boolean hasDiscovery) {
        this.hasDiscovery = hasDiscovery;
    }

    public void setUserTemplate(final UserTO userTemplate) {
        this.userTemplate = userTemplate;
    }

    public boolean isCreateUnmatching() {
        return createUnmatching;
    }

    public void setCreateUnmatching(final boolean createUnmatching) {
        this.createUnmatching = createUnmatching;
    }

    public boolean isUpdateMatching() {
        return updateMatching;
    }

    public void setUpdateMatching(final boolean updateMatching) {
        this.updateMatching = updateMatching;
    }

    public boolean isSelfRegUnmatching() {
        return selfRegUnmatching;
    }

    public void setSelfRegUnmatching(final boolean selfRegUnmatching) {
        this.selfRegUnmatching = selfRegUnmatching;
    }

    @JacksonXmlElementWrapper(localName = "actions")
    @JacksonXmlProperty(localName = "action")
    public List<String> getActions() {
        return actions;
    }
}
