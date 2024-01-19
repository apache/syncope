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

import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.core.logic.init.AMEntitlementLoader;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthAccountLogic;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.logic.wa.ImpersonationLogic;
import org.apache.syncope.core.logic.wa.MfaTrusStorageLogic;
import org.apache.syncope.core.logic.wa.WAClientAppLogic;
import org.apache.syncope.core.logic.wa.WAConfigLogic;
import org.apache.syncope.core.logic.wa.WebAuthnRegistrationLogic;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.SRARouteDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2SPEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.SRARouteDataBinder;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AMLogicContext {

    @ConditionalOnMissingBean
    @Bean
    public AMEntitlementLoader amEntitlementLoader() {
        return new AMEntitlementLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleLogic authModuleLogic(
            final AuthModuleDataBinder binder,
            final AuthModuleDAO authModuleDAO) {

        return new AuthModuleLogic(binder, authModuleDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoLogic attrRepoLogic(
            final AttrRepoDataBinder binder,
            final AttrRepoDAO attrRepoDAO) {

        return new AttrRepoLogic(binder, attrRepoDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileLogic authProfileLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new AuthProfileLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppLogic clientAppLogic(
            final ServiceOps serviceOps,
            final ClientAppUtilsFactory clientAppUtilsFactory,
            final ClientAppDataBinder binder,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO) {

        return new ClientAppLogic(
                serviceOps,
                clientAppUtilsFactory,
                binder,
                casSPClientAppDAO,
                oidcRPClientAppDAO,
                saml2SPClientAppDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSLogic oidcJWKSLogic(
            final OIDCJWKSDataBinder oidcJWKSDataBinder,
            final OIDCJWKSDAO dao,
            final EntityFactory entityFactory) {

        return new OIDCJWKSLogic(oidcJWKSDataBinder, dao, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityLogic saml2IdPEntityLogic(
            final SAML2IdPEntityDataBinder binder,
            final SAML2IdPEntityDAO saml2IdPEntityDAO) {

        return new SAML2IdPEntityLogic(binder, saml2IdPEntityDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityLogic saml2SPEntityLogic(
            final SAML2SPEntityDataBinder binder,
            final SAML2SPEntityDAO saml2SPEntityDAO) {

        return new SAML2SPEntityLogic(binder, saml2SPEntityDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteLogic sraRouteLogic(
            final EntityFactory entityFactory,
            final ServiceOps serviceOps,
            final SecurityProperties securityProperties,
            final SRARouteDAO routeDAO,
            final SRARouteDataBinder binder) {

        return new SRARouteLogic(routeDAO, binder, entityFactory, serviceOps, securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public GoogleMfaAuthAccountLogic googleMfaAuthAccountLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new GoogleMfaAuthAccountLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public GoogleMfaAuthTokenLogic googleMfaAuthTokenLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new GoogleMfaAuthTokenLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImpersonationLogic impersonationLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new ImpersonationLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public MfaTrusStorageLogic mfaTrusStorageLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new MfaTrusStorageLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAClientAppLogic waClientAppLogic(
            final WAClientAppDataBinder binder,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO) {

        return new WAClientAppLogic(binder, casSPClientAppDAO, oidcRPClientAppDAO, saml2SPClientAppDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigLogic waConfigLogic(
            final ServiceOps serviceOps,
            final SecurityProperties securityProperties,
            final WAConfigDataBinder binder,
            final WAConfigDAO waConfigDAO) {

        return new WAConfigLogic(serviceOps, binder, waConfigDAO, securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public WebAuthnRegistrationLogic webAuthnRegistrationLogic(
            final AuthProfileDataBinder authProfileDataBinder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        return new WebAuthnRegistrationLogic(authProfileDataBinder, authProfileDAO, entityFactory);
    }
}
