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
package org.apache.syncope.core.logic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
    "issuer",
    "authorization_endpoint",
    "token_endpoint",
    "userinfo_endpoint",
    "end_session_endpoint",
    "jwks_uri",
    "registration_endpoint"
})
public class OIDCProviderDiscoveryDocument {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("end_session_endpoint")
    private String endSessionEndpoint;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;

    @JsonProperty("issuer")
    public String getIssuer() {
        return issuer;
    }

    @JsonProperty("issuer")
    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    @JsonProperty("authorization_endpoint")
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    @JsonProperty("authorization_endpoint")
    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    @JsonProperty("token_endpoint")
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    @JsonProperty("token_endpoint")
    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @JsonProperty("userinfo_endpoint")
    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    @JsonProperty("userinfo_endpoint")
    public void setUserinfoEndpoint(final String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    @JsonProperty("end_session_endpoint")
    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    @JsonProperty("end_session_endpoint")
    public void setEndSessionEndpoint(final String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    @JsonProperty("jwks_uri")
    public String getJwksUri() {
        return jwksUri;
    }

    @JsonProperty("jwks_uri")
    public void setJwksUri(final String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @JsonProperty("registration_endpoint")
    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    @JsonProperty("registration_endpoint")
    public void setRegistrationEndpoint(final String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

}
