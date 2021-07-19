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
package org.apache.syncope.common.lib.auth;

import java.util.HashMap;
import java.util.Map;

public class OIDCAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    /**
     * The client id.
     */
    private String id;

    /**
     * The client secret.
     */
    private String secret;

    /**
     * The attribute value that should be used
     * for the authenticated username, upon a successful authentication
     * attempt.
     */
    private String userIdAttribute;

    private String discoveryUri;

    /**
     * Whether an initial nonce should be to used
     * initially for replay attack mitigation.
     */
    private boolean useNonce;

    /**
     * Requested scope(s).
     */
    private String scope;

    /**
     * The JWS algorithm to use forcefully when validating ID tokens.
     * If none is defined, the first algorithm from metadata will be used.
     */
    private String preferredJwsAlgorithm;

    /**
     * Clock skew in order to account for drift, when validating id tokens.
     */
    private String maxClockSkew;

    /**
     * Custom parameters to send along in authZ requests, etc.
     */
    private final Map<String, String> customParams = new HashMap<>(0);

    /**
     * The response mode specifies how the result of the authorization request is formatted.
     * Possible values includes "query", "fragment", "form_post", or "web_message"
     */
    private String responseMode;

    /**
     * The response type tells the authorization server which grant to execute.
     * Possibles values includes "code", "token" or "id_token".
     */
    private String responseType;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(final String secret) {
        this.secret = secret;
    }

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    public void setUserIdAttribute(final String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }

    public String getDiscoveryUri() {
        return discoveryUri;
    }

    public void setDiscoveryUri(final String discoveryUri) {
        this.discoveryUri = discoveryUri;
    }

    public boolean isUseNonce() {
        return useNonce;
    }

    public void setUseNonce(final boolean useNonce) {
        this.useNonce = useNonce;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getPreferredJwsAlgorithm() {
        return preferredJwsAlgorithm;
    }

    public void setPreferredJwsAlgorithm(final String preferredJwsAlgorithm) {
        this.preferredJwsAlgorithm = preferredJwsAlgorithm;
    }

    public String getMaxClockSkew() {
        return maxClockSkew;
    }

    public void setMaxClockSkew(final String maxClockSkew) {
        this.maxClockSkew = maxClockSkew;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(final String responseMode) {
        this.responseMode = responseMode;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }
}
