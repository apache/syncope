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

public abstract class AbstractOIDCAuthModuleConf extends AbstractOAuth20AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    protected String discoveryUri;

    /**
     * Whether an initial nonce should be to used
     * initially for replay attack mitigation.
     */
    protected boolean useNonce;

    /**
     * The JWS algorithm to use forcefully when validating ID tokens.
     * If none is defined, the first algorithm from metadata will be used.
     */
    protected String preferredJwsAlgorithm;

    /**
     * Clock skew in order to account for drift, when validating id tokens.
     */
    protected String maxClockSkew;

    /**
     * The response mode specifies how the result of the authorization request is formatted.
     * Possible values includes "query", "fragment", "form_post", or "web_message"
     */
    protected String responseMode;

    /**
     * Checks if sessions expire with token expiration.
     */
    protected boolean expireSessionWithToken;

    /**
     * Default time period advance (in seconds) for considering an access token expired.
     * This settings supports the java.time.Duration syntax.
     * The format of the value will be PTnHnMnS, where n is the relevant hours, minutes or
     * seconds part of the duration. Any fractional seconds are placed after a decimal point in the seconds section.
     * If a section has a zero value, it is omitted. The hours, minutes and seconds will all have the same sign.
     * Example values could be in the form of PT20S, PT15M, PT10H, PT6D, P2DT3H4M.
     * If the value is set to 0 or never, the duration will be set to zero. If the value is blank, set to -1, or
     * infinite, the value will effectively represent an unending duration.
     */
    protected String tokenExpirationAdvance;

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

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(final String responseMode) {
        this.responseMode = responseMode;
    }

    public boolean isExpireSessionWithToken() {
        return expireSessionWithToken;
    }

    public void setExpireSessionWithToken(final boolean expireSessionWithToken) {
        this.expireSessionWithToken = expireSessionWithToken;
    }

    public String getTokenExpirationAdvance() {
        return tokenExpirationAdvance;
    }

    public void setTokenExpirationAdvance(final String tokenExpirationAdvance) {
        this.tokenExpirationAdvance = tokenExpirationAdvance;
    }
}
