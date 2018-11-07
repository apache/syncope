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

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "oidcLoginRequest")
@XmlType
public class OIDCLoginRequestTO implements Serializable {

    private static final long serialVersionUID = -3509031322459942441L;

    private String providerAddress;

    private String clientId;

    private String scope;

    private String responseType;

    private String redirectURI;

    private String state;

    public String getProviderAddress() {
        return providerAddress;
    }

    public void setProviderAddress(final String providerAddress) {
        this.providerAddress = providerAddress;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public void setRedirectURI(final String redirectURI) {
        this.redirectURI = redirectURI;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

}
