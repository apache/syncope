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
package org.apache.syncope.common.lib;

import java.io.Serializable;

public abstract class AbstractAzureActiveDirectoryConf implements Serializable {

    private static final long serialVersionUID = 282571926999684266L;

    private String clientId;

    private String clientSecret;

    /**
     * This URL of the security token service that CAS goes to for acquiring tokens for resources and users.
     * This URL allows CAS to establish what is called an 'authority'.
     * You can think of the authority as the directory issuing the identities/tokens. The login URL here is then
     * composed of {@code https://<instance>/<tenant>}, where 'instance' is the Azure AD host
     * (such as {@code https://login.microsoftonline.com}) and 'tenant' is the domain name
     * (such as {@code contoso.onmicrosoft.com}) or tenant ID of the directory.
     * Examples of authority URL are:
     *
     * <ul>
     * <li>{@code https://login.microsoftonline.com/f31e6716-26e8-4651-b323-2563936b4163}: for a single tenant
     * application defined in the tenant</li>
     * <li>{@code https://login.microsoftonline.com/contoso.onmicrosoft.com}: This representation is like the previous
     * one, but uses the tenant domain name instead of the tenant Id.</li>
     * <li>{@code https://login.microsoftonline.de/contoso.de}: also uses a domain name, but in this case the Azure AD
     * tenant admins have set a custom domain for their tenant, and the
     * instance URL here is for the German national cloud.</li>
     * <li>{@code https://login.microsoftonline.com/common}: in the case of a multi-tenant application, that is an
     * application available in several Azure AD tenants.</li>
     * <li>It can finally be an Active Directory Federation Services (ADFS) URL, which is recognized
     * with the convention that the URL should contain adfs like {@code https://contoso.com/adfs}.</li>
     * </ul>
     */
    private String loginUrl = "https://login.microsoftonline.com/common/";

    /**
     * Resource url for the graph API to fetch attributes.
     */
    private String resource = "https://graph.microsoft.com/";

    /**
     * The microsoft tenant id.
     */
    private String tenant;

    /**
     * Scope used when fetching access tokens.
     * Multiple scopes may be separated using a comma.
     */
    private String scope = "openid,email,profile,address";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
