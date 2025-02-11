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

import org.apache.syncope.core.logic.init.OIDCC4UILoader;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.logic.oidc.OIDCUserManager;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.OIDCC4UIProviderDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OIDCC4UILogicContext {

    @ConditionalOnMissingBean(name = "oidcClientCacheLogin")
    @Bean
    public OIDCClientCache oidcClientCacheLogin() {
        return new OIDCClientCache();
    }

    @ConditionalOnMissingBean(name = "oidcClientCacheLogout")
    @Bean
    public OIDCClientCache oidcClientCacheLogout() {
        return new OIDCClientCache();
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCC4UILoader oidcc4UILoader() {
        return new OIDCC4UILoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCUserManager oidcUserManager(
            final InboundMatcher inboundMatcher,
            final UserDAO userDAO,
            final ImplementationDAO implementationDAO,
            final IntAttrNameParser intAttrNameParser,
            final TemplateUtils templateUtils,
            final UserProvisioningManager provisioningManager,
            final UserDataBinder binder) {

        return new OIDCUserManager(
                inboundMatcher,
                userDAO,
                implementationDAO,
                intAttrNameParser,
                templateUtils,
                provisioningManager,
                binder);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCC4UILogic oidcc4UILogic(
            @Qualifier("oidcClientCacheLogin")
            final OIDCClientCache oidcClientCacheLogin,
            @Qualifier("oidcClientCacheLogout")
            final OIDCClientCache oidcClientCacheLogout,
            final AuthDataAccessor authDataAccessor,
            final AccessTokenDataBinder accessTokenDataBinder,
            final OIDCC4UIProviderDAO opDAO,
            final OIDCUserManager userManager,
            final EncryptorManager encryptorManager) {

        return new OIDCC4UILogic(
                oidcClientCacheLogin,
                oidcClientCacheLogout,
                authDataAccessor,
                accessTokenDataBinder,
                opDAO,
                userManager,
                encryptorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCC4UIProviderLogic oidcc4UIProviderLogic(
            @Qualifier("oidcClientCacheLogin")
            final OIDCClientCache oidcClientCacheLogin,
            @Qualifier("oidcClientCacheLogout")
            final OIDCClientCache oidcClientCacheLogout,
            final OIDCC4UIProviderDAO opDAO,
            final OIDCC4UIProviderDataBinder binder) {

        return new OIDCC4UIProviderLogic(oidcClientCacheLogin, oidcClientCacheLogout, opDAO, binder);
    }
}
