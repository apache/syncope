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
package org.apache.syncope.core.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.AccessPolicyTO;
import org.apache.syncope.common.lib.to.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.to.AuthPolicyTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.RegisteredClientAppBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegisteredClientAppLogic {

    @Autowired
    private ImplementationLogic implementationLogic;

    @Autowired
    private PolicyLogic policyLogic;

    @Autowired
    private ClientAppDataBinder clientAppDataBinder;

    @Autowired
    private RegisteredClientAppBinder binder;

    @Autowired
    private SAML2SPDAO saml2spDAO;

    @Autowired
    private OIDCRPDAO oidcrpDAO;

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<RegisteredClientAppTO> list() {
        List<RegisteredClientAppTO> registeredApplications = new ArrayList<>();
        Arrays.asList(ClientAppType.values()).forEach(type -> {
            switch (type) {
                case OIDCRP:
                    registeredApplications.addAll(oidcrpDAO.findAll().stream().map(binder::getRegisteredClientAppTO).
                            collect(Collectors.toList()));
                    break;

                case SAML2SP:
                default:
                    registeredApplications.addAll(saml2spDAO.findAll().stream().map(binder::getRegisteredClientAppTO).
                            collect(Collectors.toList()));
            }
        });

        return registeredApplications;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public RegisteredClientAppTO read(final Long clientAppId, final ClientAppType type) {
        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.findByClientAppId(clientAppId);
                if (oidcrp != null) {
                    return binder.getRegisteredClientAppTO(oidcrp);
                }
            case SAML2SP:
                SAML2SP saml2sp = saml2spDAO.findByClientAppId(clientAppId);
                if (saml2sp != null) {
                    return binder.getRegisteredClientAppTO(saml2sp);
                }
            default:
                return null;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public RegisteredClientAppTO read(final Long clientAppId) {
        for (ClientAppType type : ClientAppType.values()) {
            RegisteredClientAppTO registeredClientAppTO = read(clientAppId, type);
            if (registeredClientAppTO != null) {
                return registeredClientAppTO;
            }
        }

        return null;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public RegisteredClientAppTO read(final String name, final ClientAppType type) {
        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.findByName(name);
                if (oidcrp != null) {
                    return binder.getRegisteredClientAppTO(oidcrp);
                }
            case SAML2SP:
                SAML2SP saml2sp = saml2spDAO.findByName(name);
                if (saml2sp != null) {
                    return binder.getRegisteredClientAppTO(saml2sp);
                }
            default:
                return null;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public RegisteredClientAppTO read(final String name) {
        for (ClientAppType type : ClientAppType.values()) {
            RegisteredClientAppTO registeredClientAppTO = read(name, type);
            if (registeredClientAppTO != null) {
                return registeredClientAppTO;
            }
        }
        return null;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public RegisteredClientAppTO create(final RegisteredClientAppTO registeredClientAppTO) {

        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        if (registeredClientAppTO.getAuthPolicyConf() != null) {
            String policyName = registeredClientAppTO.getClientAppTO().getName() + "AuthPolicy";
            ImplementationTO implementationTO = new ImplementationTO();
            implementationTO.setKey(policyName);
            implementationTO.setEngine(ImplementationEngine.JAVA);
            implementationTO.setType(AMImplementationType.AUTH_POLICY_CONFIGURATIONS);
            implementationTO.setBody(POJOHelper.serialize(registeredClientAppTO.getAuthPolicyConf()));

            ImplementationTO conf = implementationLogic.create(implementationTO);

            authPolicyTO.setConfiguration(conf.getKey());
            authPolicyTO = policyLogic.create(PolicyType.AUTH, authPolicyTO);
        }

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        if (registeredClientAppTO.getAccessPolicyConf() != null) {

            String policyName = registeredClientAppTO.getClientAppTO().getName() + "AccessPolicy";
            ImplementationTO implementationTO = new ImplementationTO();
            implementationTO.setKey(policyName);
            implementationTO.setEngine(ImplementationEngine.JAVA);
            implementationTO.setType(AMImplementationType.ACCESS_POLICY_CONFIGURATIONS);
            implementationTO.setBody(POJOHelper.serialize(registeredClientAppTO.getAuthPolicyConf()));

            ImplementationTO conf = implementationLogic.create(implementationTO);

            accessPolicyTO.setConfiguration(conf.getKey());
            accessPolicyTO = policyLogic.create(PolicyType.ACCESS, accessPolicyTO);
        }

        AttrReleasePolicyTO attrReleasePolicyTO = new AttrReleasePolicyTO();
        if (registeredClientAppTO.getAttrReleasePolicyConf() != null) {

            String policyName = registeredClientAppTO.getClientAppTO().getName() + "AttrReleasePolicy";
            ImplementationTO implementationTO = new ImplementationTO();
            implementationTO.setKey(policyName);
            implementationTO.setEngine(ImplementationEngine.JAVA);
            implementationTO.setType(AMImplementationType.ATTR_RELEASE_POLICY_CONFIGURATIONS);
            implementationTO.setBody(POJOHelper.serialize(registeredClientAppTO.getAttrReleasePolicyConf()));

            ImplementationTO conf = implementationLogic.create(implementationTO);

            attrReleasePolicyTO.setConfiguration(conf.getKey());
            attrReleasePolicyTO = policyLogic.create(PolicyType.ATTR_RELEASE, attrReleasePolicyTO);
        }

        if (registeredClientAppTO.getClientAppTO() instanceof OIDCRPTO) {
            OIDCRPTO oidcrpto = OIDCRPTO.class.cast(registeredClientAppTO.getClientAppTO());
            oidcrpto.setAccessPolicy(accessPolicyTO.getKey());
            oidcrpto.setAttrReleasePolicy(attrReleasePolicyTO.getKey());
            oidcrpto.setAuthPolicy(authPolicyTO.getKey());
            return binder.getRegisteredClientAppTO(oidcrpDAO.save(clientAppDataBinder.create(oidcrpto)));

        } else if (registeredClientAppTO.getClientAppTO() instanceof SAML2SPTO) {
            SAML2SPTO saml2spto = SAML2SPTO.class.cast(registeredClientAppTO.getClientAppTO());
            saml2spto.setAccessPolicy(accessPolicyTO.getKey());
            saml2spto.setAttrReleasePolicy(attrReleasePolicyTO.getKey());
            saml2spto.setAuthPolicy(authPolicyTO.getKey());
            return binder.getRegisteredClientAppTO(saml2spDAO.save(clientAppDataBinder.create(saml2spto)));
        }

        return null;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public boolean delete(final String name) {
        ClientAppTO clientAppTO = read(name).getClientAppTO();
        if (clientAppTO != null) {
            if (clientAppTO instanceof OIDCRPTO) {
                oidcrpDAO.delete(clientAppTO.getKey());
            } else if (clientAppTO instanceof SAML2SPTO) {
                saml2spDAO.delete(clientAppTO.getKey());
            }
            return true;
        }
        return false;
    }
}
