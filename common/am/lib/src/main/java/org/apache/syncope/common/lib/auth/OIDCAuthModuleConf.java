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

import java.util.Map;
import org.apache.syncope.common.lib.to.AuthModuleTO;

public class OIDCAuthModuleConf extends AbstractOIDCAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    protected String discoveryUri;

    /**
     * Whether an initial nonce should be to used
     * initially for replay attack mitigation.
     */
    protected boolean useNonce;

    /**
     * Requested scope(s).
     */
    protected String scope;

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

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
