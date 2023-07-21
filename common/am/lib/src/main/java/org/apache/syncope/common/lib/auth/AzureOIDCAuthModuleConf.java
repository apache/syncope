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

public class AzureOIDCAuthModuleConf extends AbstractOIDCAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579522L;

    /**
     * Azure AD tenant name. After tenant is configured, #getDiscoveryUri() property will be overridden.
     *
     * Azure AD tenant name can take 4 different values:
     * - organizations: Only users with work or school accounts from Azure AD can sign in.
     * - consumers: Only users with a personal Microsoft account can sign in.
     * - Specific tenant domain name or ID: Only user with account under that the specified tenant can login
     */
    protected String tenant;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(final String tenant) {
        this.tenant = tenant;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
