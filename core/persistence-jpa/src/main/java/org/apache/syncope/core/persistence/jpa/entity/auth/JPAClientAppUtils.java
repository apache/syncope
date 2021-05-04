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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtils;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRPClientApp;

public class JPAClientAppUtils implements ClientAppUtils {

    private final ClientAppType type;

    protected JPAClientAppUtils(final ClientAppType type) {
        this.type = type;
    }

    @Override
    public ClientAppType getType() {
        return type;
    }

    @Override
    public Class<? extends ClientApp> clientAppClass() {
        switch (type) {
            case OIDCRP:
                return OIDCRPClientApp.class;
            case CASSP:
                return CASSPClientApp.class;
            case SAML2SP:
            default:
                return SAML2SPClientApp.class;
        }
    }
}
