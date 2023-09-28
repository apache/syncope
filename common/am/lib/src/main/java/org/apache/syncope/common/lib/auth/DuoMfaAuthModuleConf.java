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

public class DuoMfaAuthModuleConf implements MFAAuthModuleConf {

    private static final long serialVersionUID = -2883257599439312426L;

    private String integrationKey;

    private String secretKey;

    private String apiHost;

    @Override
    public String getFriendlyName() {
        return "Duo Security";
    }

    public String getIntegrationKey() {
        return integrationKey;
    }

    public void setIntegrationKey(final String integrationKey) {
        this.integrationKey = integrationKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(final String apiHost) {
        this.apiHost = apiHost;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
