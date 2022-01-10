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

import org.apache.syncope.core.logic.init.SAML2SP4UILoader;
import org.apache.syncope.core.logic.saml2.SAML2ClientCache;
import org.apache.syncope.core.logic.saml2.SAML2SP4UIUserManager;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2SP4UIIdPDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

@EnableConfigurationProperties(SAML2SP4UIProperties.class)
@Configuration(proxyBeanMethods = false)
public class SAML2SP4UILogicContext {

    @ConditionalOnMissingBean
    @Bean
    public SAML2ClientCache saml2ClientCache() {
        return new SAML2ClientCache();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UILoader saml2SP4UILoader(final ResourcePatternResolver resourceResolver,
                                             final SAML2SP4UIProperties props) {
        return new SAML2SP4UILoader(props, resourceResolver);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIIdPLogic saml2SP4UIIdPLogic(
            final SAML2SP4UIIdPDAO idpDAO,
            final SAML2ClientCache saml2ClientCache,
            final SAML2SP4UILoader loader,
            final SAML2SP4UIIdPDataBinder binder) {

        return new SAML2SP4UIIdPLogic(loader, saml2ClientCache, binder, idpDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIUserManager saml2SP4UIUserManager(
            final SAML2SP4UIIdPDAO idpDAO,
            final InboundMatcher inboundMatcher,
            final UserDAO userDAO,
            final IntAttrNameParser intAttrNameParser,
            final TemplateUtils templateUtils,
            final UserProvisioningManager provisioningManager,
            final UserDataBinder binder) {

        return new SAML2SP4UIUserManager(
                idpDAO,
                inboundMatcher,
                userDAO,
                intAttrNameParser,
                templateUtils,
                provisioningManager,
                binder);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UILogic saml2SP4UILogic(
            final SAML2SP4UIIdPDAO idpDAO,
            final SAML2ClientCache saml2ClientCache,
            final SAML2SP4UILoader loader,
            final AccessTokenDataBinder accessTokenDataBinder,
            final SAML2SP4UIUserManager userManager,
            final AuthDataAccessor authDataAccessor) {

        return new SAML2SP4UILogic(
                loader,
                accessTokenDataBinder,
                saml2ClientCache,
                userManager,
                idpDAO,
                authDataAccessor);
    }
}
