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
package org.apache.syncope.core.provisioning.java.data.wa;

import org.apache.syncope.common.lib.policy.AuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.core.persistence.api.dao.auth.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppBinder;

@Component
public class WAClientAppBinderImpl implements WAClientAppBinder {

    private static final Logger LOG = LoggerFactory.getLogger(WAClientAppBinder.class);

    @Autowired
    private ClientAppDataBinder clientAppDataBinder;

    @Autowired
    private AuthModuleDAO authModuleDAO;

    @Override
    public WAClientApp getWAClientApp(final ClientApp clientApp) {
        WAClientApp waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(clientAppDataBinder.getClientAppTO(clientApp));

        try {
            AuthPolicyConf authPolicyConf = null;
            if (clientApp.getAuthPolicy() != null) {
                authPolicyConf = clientApp.getAuthPolicy().getConf();
                waClientApp.setAuthPolicyConf(clientApp.getAuthPolicy().getConf());
            } else if (clientApp.getRealm().getAuthPolicy() != null) {
                authPolicyConf = clientApp.getRealm().getAuthPolicy().getConf();
                waClientApp.setAuthPolicyConf(clientApp.getRealm().getAuthPolicy().getConf());
            }

            if (clientApp.getAccessPolicy() != null) {
                waClientApp.setAccessPolicyConf(clientApp.getAccessPolicy().getConf());
            } else if (clientApp.getRealm().getAccessPolicy() != null) {
                waClientApp.setAccessPolicyConf(clientApp.getRealm().getAccessPolicy().getConf());
            }

            if (clientApp.getAttrReleasePolicy() != null) {
                waClientApp.setAttrReleasePolicyConf(clientApp.getAttrReleasePolicy().getConf());
            } else if (clientApp.getRealm().getAttrReleasePolicy() != null) {
                waClientApp.setAttrReleasePolicyConf(clientApp.getRealm().getAttrReleasePolicy().getConf());
            }

            if (authPolicyConf instanceof DefaultAuthPolicyConf
                    && !((DefaultAuthPolicyConf) authPolicyConf).getAuthModules().isEmpty()) {
                ((DefaultAuthPolicyConf) authPolicyConf).getAuthModules().forEach(authModuleKey -> {
                    AuthModule authModule = authModuleDAO.find(authModuleKey);
                    if (authModule == null) {
                        LOG.warn("AuthModule " + authModuleKey + " not found");
                    } else {
                        authModule.getItems().forEach(item -> waClientApp.getReleaseAttrs().put(
                                item.getExtAttrName(), item.getIntAttrName()));
                    }
                });
            }
            if (waClientApp.getReleaseAttrs().isEmpty()) {
                if (clientApp.getAttrReleasePolicy() != null) {
                    waClientApp.setAttrReleasePolicyConf(clientApp.getAttrReleasePolicy().getConf());
                } else if (clientApp.getRealm().getAttrReleasePolicy() != null) {
                    waClientApp.setAttrReleasePolicyConf(clientApp.getRealm().getAttrReleasePolicy().getConf());
                }
            }
        } catch (Exception e) {
            LOG.error("While building the configuration from an application's policy ", e);
        }

        return waClientApp;
    }
}
