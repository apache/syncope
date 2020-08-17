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
package org.apache.syncope.common.lib.saml2;

import org.apache.commons.lang3.StringUtils;

public class SAML2Response extends SAML2Message {

    private static final long serialVersionUID = 6102419133516694822L;

    private String spEntityID;

    private String urlContext;

    private String samlResponse;

    private String relayState;

    public String getSpEntityID() {
        return spEntityID;
    }

    public void setSpEntityID(final String spEntityID) {
        this.spEntityID = StringUtils.appendIfMissing(spEntityID, "/");
    }

    public String getUrlContext() {
        return urlContext;
    }

    public void setUrlContext(final String urlContext) {
        this.urlContext = urlContext;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public void setSamlResponse(final String samlResponse) {
        this.samlResponse = samlResponse;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(final String relayState) {
        this.relayState = relayState;
    }
}
