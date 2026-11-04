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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.syncope.core.logic.init.OIDCC4UILoader;
import org.apache.syncope.core.logic.oidc.JCacheSessionStore;
import org.apache.syncope.core.logic.oidc.NoOpSessionStore;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.logic.oidc.OIDCUserManager;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.OIDCC4UIProviderDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(OIDCC4UIProperties.class)
@Configuration(proxyBeanMethods = false)
public class OIDCC4UILogicContext {

    @ConditionalOnMissingBean(name = "oidcSessionStore")
    @Bean
    public SessionStore oidcSessionStore(
            final CacheManager cacheManager,
            final EncryptorManager encryptorManager,
            final OIDCC4UIProperties props) {

        if (props.isSessionStoreEnabled()) {
            @SuppressWarnings("unchecked")
            MutableConfiguration<String, Map<String, Object>> config =
                    new MutableConfiguration<String, Map<String, Object>>().
                            setTypes(String.class, (Class<Map<String, Object>>) (Class<?>) Map.class).
                            setStoreByValue(false).
                            setReadThrough(false).
                            setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(
                                    new Duration(TimeUnit.MINUTES, props.getSessionStoreExpirationMinutes())));
            Cache<String, Map<String, Object>> cache = cacheManager.createCache("oidcSessionStore", config);

            return new JCacheSessionStore(cache, encryptorManager);
        }

        return NoOpSessionStore.INSTANCE;
    }

    @ConditionalOnMissingBean(name = "oidcClientCacheLogin")
    @Bean
    public OIDCClientCache oidcClientCacheLogin(
            @Qualifier("oidcSessionStore")
            final SessionStore oidcSessionStore) {

        return new OIDCClientCache(oidcSessionStore);
    }

    @ConditionalOnMissingBean(name = "oidcClientCacheLogout")
    @Bean
    public OIDCClientCache oidcClientCacheLogout(
            @Qualifier("oidcSessionStore")
            final SessionStore oidcSessionStore) {

        return new OIDCClientCache(oidcSessionStore);
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
            final AccessTokenDAO accessTokenDAO,
            final OIDCUserManager userManager,
            final EncryptorManager encryptorManager,
            @Qualifier("oidcSessionStore")
            final SessionStore oidcSessionStore) {

        return new OIDCC4UILogic(
                oidcClientCacheLogin,
                oidcClientCacheLogout,
                authDataAccessor,
                accessTokenDataBinder,
                opDAO,
                accessTokenDAO,
                userManager,
                encryptorManager,
                oidcSessionStore);
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
