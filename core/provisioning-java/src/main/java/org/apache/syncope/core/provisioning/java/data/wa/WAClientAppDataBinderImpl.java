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
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAClientAppDataBinderImpl implements WAClientAppDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(WAClientAppDataBinder.class);

    protected final ClientAppDataBinder clientAppDataBinder;

    protected final PolicyDataBinder policyDataBinder;

    protected final AuthModuleDataBinder authModuleDataBinder;

    protected final AuthModuleDAO authModuleDAO;

    public WAClientAppDataBinderImpl(
            final ClientAppDataBinder clientAppDataBinder,
            final PolicyDataBinder policyDataBinder,
            final AuthModuleDataBinder authModuleDataBinder,
            final AuthModuleDAO authModuleDAO) {

        this.clientAppDataBinder = clientAppDataBinder;
        this.policyDataBinder = policyDataBinder;
        this.authModuleDataBinder = authModuleDataBinder;
        this.authModuleDAO = authModuleDAO;
    }

    @Override
    public WAClientApp getWAClientApp(final ClientApp clientApp) {
        WAClientApp waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(clientAppDataBinder.getClientAppTO(clientApp));

        try {
            AuthPolicyConf authPolicyConf = null;
            if (clientApp.getAuthPolicy() != null) {
                authPolicyConf = clientApp.getAuthPolicy().getConf();
                waClientApp.setAuthPolicy(policyDataBinder.getPolicyTO(clientApp.getAuthPolicy()));
            } else if (clientApp.getRealm() != null && clientApp.getRealm().getAuthPolicy() != null) {
                authPolicyConf = clientApp.getRealm().getAuthPolicy().getConf();
                waClientApp.setAuthPolicy(policyDataBinder.getPolicyTO(clientApp.getRealm().getAuthPolicy()));
            }
            if (authPolicyConf instanceof DefaultAuthPolicyConf defaultAuthPolicyConf) {
                defaultAuthPolicyConf.getAuthModules().forEach(key -> authModuleDAO.findById(key).ifPresentOrElse(
                        module -> waClientApp.getAuthModules().add(authModuleDataBinder.getAuthModuleTO(module)),
                        () -> LOG.warn("AuthModule {} not found", key)));
            }

            if (clientApp.getAccessPolicy() != null) {
                waClientApp.setAccessPolicy(policyDataBinder.getPolicyTO(clientApp.getAccessPolicy()));
            } else if (clientApp.getRealm() != null && clientApp.getRealm().getAccessPolicy() != null) {
                waClientApp.setAccessPolicy(policyDataBinder.getPolicyTO(clientApp.getRealm().getAccessPolicy()));
            }

            if (clientApp.getAttrReleasePolicy() != null) {
                waClientApp.setAttrReleasePolicy(
                        policyDataBinder.getPolicyTO(clientApp.getAttrReleasePolicy()));
            } else if (clientApp.getRealm() != null && clientApp.getRealm().getAttrReleasePolicy() != null) {
                waClientApp.setAttrReleasePolicy(
                        policyDataBinder.getPolicyTO(clientApp.getRealm().getAttrReleasePolicy()));
            }

            if (clientApp.getTicketExpirationPolicy() != null) {
                waClientApp.setTicketExpirationPolicy(
                        policyDataBinder.getPolicyTO(clientApp.getTicketExpirationPolicy()));
            } else if (clientApp.getRealm() != null && clientApp.getRealm().getTicketExpirationPolicy() != null) {
                waClientApp.setTicketExpirationPolicy(
                        policyDataBinder.getPolicyTO(clientApp.getRealm().getTicketExpirationPolicy()));
            }
        } catch (Exception e) {
            LOG.error("While building the configuration from an application's policy ", e);
        }

        return waClientApp;
    }
}
