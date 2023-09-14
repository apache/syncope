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
package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.AuthProfileService;
import org.apache.syncope.common.rest.api.service.ClientAppService;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.common.rest.api.service.wa.MfaTrustStorageService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.core.logic.AttrRepoLogic;
import org.apache.syncope.core.logic.AuthModuleLogic;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.apache.syncope.core.logic.ClientAppLogic;
import org.apache.syncope.core.logic.OIDCJWKSLogic;
import org.apache.syncope.core.logic.SAML2IdPEntityLogic;
import org.apache.syncope.core.logic.SAML2SPEntityLogic;
import org.apache.syncope.core.logic.SRARouteLogic;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthAccountLogic;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.logic.wa.ImpersonationLogic;
import org.apache.syncope.core.logic.wa.MfaTrusStorageLogic;
import org.apache.syncope.core.logic.wa.WAClientAppLogic;
import org.apache.syncope.core.logic.wa.WAConfigLogic;
import org.apache.syncope.core.logic.wa.WebAuthnRegistrationLogic;
import org.apache.syncope.core.rest.cxf.service.AttrRepoServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AuthModuleServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AuthProfileServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ClientAppServiceImpl;
import org.apache.syncope.core.rest.cxf.service.OIDCJWKSServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SAML2IdPEntityServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SAML2SPEntityServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SRARouteServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.GoogleMfaAuthAccountServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.GoogleMfaAuthTokenServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.ImpersonationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.MfaTrustStorageServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.WAClientAppServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.WAConfigServiceImpl;
import org.apache.syncope.core.rest.cxf.service.wa.WebAuthnRegistrationServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AMRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleService authModuleService(final AuthModuleLogic authModuleLogic) {
        return new AuthModuleServiceImpl(authModuleLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoService attrRepoService(final AttrRepoLogic attrRepoLogic) {
        return new AttrRepoServiceImpl(attrRepoLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileService authProfileService(final AuthProfileLogic authProfileLogic) {
        return new AuthProfileServiceImpl(authProfileLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppService clientAppService(final ClientAppLogic clientAppLogic) {
        return new ClientAppServiceImpl(clientAppLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public GoogleMfaAuthAccountService googleMfaAuthAccountService(
            final GoogleMfaAuthAccountLogic googleMfaAuthAccountLogic) {

        return new GoogleMfaAuthAccountServiceImpl(googleMfaAuthAccountLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public GoogleMfaAuthTokenService googleMfaAuthTokenService(
            final GoogleMfaAuthTokenLogic googleMfaAuthTokenLogic) {

        return new GoogleMfaAuthTokenServiceImpl(googleMfaAuthTokenLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImpersonationService impersonationService(final ImpersonationLogic impersonationLogic) {
        return new ImpersonationServiceImpl(impersonationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSService oidcJWKSService(final OIDCJWKSLogic oidcJWKSLogic) {
        return new OIDCJWKSServiceImpl(oidcJWKSLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityService saml2IdPEntityService(final SAML2IdPEntityLogic saml2IdPEntityLogic) {
        return new SAML2IdPEntityServiceImpl(saml2IdPEntityLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityService saml2SPEntityService(final SAML2SPEntityLogic saml2SPEntityLogic) {
        return new SAML2SPEntityServiceImpl(saml2SPEntityLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteService sraRouteService(final SRARouteLogic sraRouteLogic) {
        return new SRARouteServiceImpl(sraRouteLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public MfaTrustStorageService mfaTrustStorageService(final MfaTrusStorageLogic mfaTrusStorageLogic) {
        return new MfaTrustStorageServiceImpl(mfaTrusStorageLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAClientAppService waClientAppService(final WAClientAppLogic waClientAppLogic) {
        return new WAClientAppServiceImpl(waClientAppLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigService waConfigService(final WAConfigLogic waConfigLogic) {
        return new WAConfigServiceImpl(waConfigLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public WebAuthnRegistrationService webAuthnRegistrationService(
            final WebAuthnRegistrationLogic webAuthnRegistrationLogic) {

        return new WebAuthnRegistrationServiceImpl(webAuthnRegistrationLogic);
    }
}
