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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.RegisteredClientAppBinder;
import org.apache.syncope.core.spring.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisteredClientAppBinderImpl implements RegisteredClientAppBinder {

    private static final Logger LOG = LoggerFactory.getLogger(RegisteredClientAppBinder.class);

    @Autowired
    private ClientAppDataBinder clientAppDataBinder;

    @Override
    public RegisteredClientAppTO getRegisteredClientAppTO(final ClientApp clientApp) {
        RegisteredClientAppTO registeredClientAppTO = new RegisteredClientAppTO();
        registeredClientAppTO.setClientAppTO(clientAppDataBinder.getClientAppTO(clientApp));

        try {
            if (clientApp.getAuthPolicy() != null) {
                registeredClientAppTO.setAuthPolicyConf(build((clientApp.getAuthPolicy()).getConfiguration()));
            } else if (clientApp.getRealm().getAuthPolicy() != null) {
                registeredClientAppTO.
                        setAuthPolicyConf(build((clientApp.getRealm().getAuthPolicy()).getConfiguration()));
            } else {
                registeredClientAppTO.setAuthPolicyConf(null);
            }

            if (clientApp.getAccessPolicy() != null) {
                registeredClientAppTO.setAccessPolicyConf(build((clientApp.getAccessPolicy()).getConfiguration()));
            } else if (clientApp.getRealm().getAccessPolicy() != null) {
                registeredClientAppTO.setAccessPolicyConf(build((clientApp.getRealm().getAccessPolicy()).
                        getConfiguration()));
            } else {
                registeredClientAppTO.setAccessPolicyConf(null);
            }

            if (clientApp.getAttrReleasePolicy() != null) {
                registeredClientAppTO.setAttrReleasePolicyConf(build((clientApp.getAttrReleasePolicy()).
                        getConfiguration()));
            } else if (clientApp.getRealm().getAttrReleasePolicy() != null) {
                registeredClientAppTO.setAttrReleasePolicyConf(build((clientApp.getRealm().getAttrReleasePolicy()).
                        getConfiguration()));
            } else {
                registeredClientAppTO.setAttrReleasePolicyConf(null);
            }
        } catch (Exception e) {
            LOG.error("While building the configuration from an application's policy ", e);
        }

        return registeredClientAppTO;
    }

    private <T> T build(final Implementation impl) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        return ImplementationManager.build(impl);
    }
}
