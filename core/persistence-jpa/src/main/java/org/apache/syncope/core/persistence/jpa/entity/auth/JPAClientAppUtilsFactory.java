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

import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtils;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.springframework.stereotype.Component;

@Component
public class JPAClientAppUtilsFactory implements ClientAppUtilsFactory {

    @Override
    public ClientAppUtils getInstance(final ClientAppType type) {
        return new JPAClientAppUtils(type);
    }

    @Override
    public ClientAppUtils getInstance(final ClientApp clientApp) {
        ClientAppType type;
        if (clientApp instanceof SAML2SP) {
            type = ClientAppType.SAML2SP;
        } else if (clientApp instanceof OIDCRP) {
            type = ClientAppType.OIDCRP;
        } else {
            throw new IllegalArgumentException("Invalid client app: " + clientApp);
        }

        return getInstance(type);
    }

    @Override
    public ClientAppUtils getInstance(final Class<? extends ClientAppTO> clientAppClass) {
        ClientAppType type;
        if (clientAppClass == SAML2SPTO.class) {
            type = ClientAppType.SAML2SP;
        } else if (clientAppClass == OIDCRPTO.class) {
            type = ClientAppType.OIDCRP;
        } else {
            throw new IllegalArgumentException("Invalid ClientAppTO app: " + clientAppClass.getName());
        }

        return getInstance(type);
    }

    @Override
    public ClientAppUtils getInstance(final ClientAppTO clientAppTO) {
        return getInstance(clientAppTO.getClass());
    }
}
