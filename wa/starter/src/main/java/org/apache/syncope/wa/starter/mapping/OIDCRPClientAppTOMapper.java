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
package org.apache.syncope.wa.starter.mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.claims.OidcAddressScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcEmailScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcPhoneScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcProfileScopeAttributeReleasePolicy;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.ReturnMappedAttributeReleasePolicy;
import org.apereo.cas.util.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

@ClientAppMapFor(clientAppClass = OIDCRPClientAppTO.class)
public class OIDCRPClientAppTOMapper extends AbstractClientAppMapper {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCRPClientAppTOMapper.class);

    private static final String CUSTOM_SCOPE = "syncope";

    @Override
    public RegisteredService map(
            final WAClientApp clientApp,
            final RegisteredServiceAuthenticationPolicy authenticationPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        OIDCRPClientAppTO rp = OIDCRPClientAppTO.class.cast(clientApp.getClientAppTO());
        OidcRegisteredService service = new OidcRegisteredService();
        setCommon(service, rp);

        service.setServiceId(rp.getRedirectUris().stream().
                filter(Objects::nonNull).
                collect(Collectors.joining("|")));
        service.setClientId(rp.getClientId());
        service.setClientSecret(rp.getClientSecret());
        service.setSignIdToken(rp.isSignIdToken());
        if (!service.isSignIdToken()) {
            service.setIdTokenSigningAlg("none");
        }
        service.setJwtAccessToken(rp.isJwtAccessToken());
        service.setSupportedGrantTypes(rp.getSupportedGrantTypes().stream().
                map(OIDCGrantType::name).collect(Collectors.toCollection(HashSet::new)));
        service.setSupportedResponseTypes(rp.getSupportedResponseTypes().stream().
                map(OIDCResponseType::getExternalForm).collect(Collectors.toCollection(HashSet::new)));
        if (rp.getSubjectType() != null) {
            service.setSubjectType(rp.getSubjectType().name());
        }
        service.setLogoutUrl(rp.getLogoutUri());

        setPolicies(service, authenticationPolicy, accessStrategy, attributeReleasePolicy);
        if (attributeReleasePolicy != null) {
            ChainingAttributeReleasePolicy chain = new ChainingAttributeReleasePolicy();
            if (attributeReleasePolicy instanceof ReturnMappedAttributeReleasePolicy) {
                chain.addPolicy(attributeReleasePolicy);
            } else {
                chain.addPolicy(new ReturnMappedAttributeReleasePolicy(clientApp.getReleaseAttrs()));
                chain.addPolicy(attributeReleasePolicy);
            }

            chain.addPolicy(new OidcProfileScopeAttributeReleasePolicy());
            chain.addPolicy(new OidcEmailScopeAttributeReleasePolicy());
            chain.addPolicy(new OidcAddressScopeAttributeReleasePolicy());
            chain.addPolicy(new OidcPhoneScopeAttributeReleasePolicy());

            Set<String> customClaims = clientApp.getReleaseAttrs().values().stream().
                    map(Objects::toString).collect(Collectors.toCollection(HashSet::new));
            customClaims.removeAll(OidcProfileScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
            customClaims.removeAll(OidcEmailScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
            customClaims.removeAll(OidcAddressScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
            customClaims.removeAll(OidcPhoneScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
            if (!customClaims.isEmpty()) {
                ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                if (ctx == null) {
                    LOG.warn("Could not locate the application context to add custom claims {}", customClaims);
                } else {
                    CasConfigurationProperties properties = ctx.getBean(CasConfigurationProperties.class);
                    List<String> supportedClaims = properties.getAuthn().getOidc().getDiscovery().getClaims();
                    if (!supportedClaims.containsAll(customClaims)) {
                        properties.getAuthn().getOidc().getDiscovery().setClaims(
                                Stream.concat(supportedClaims.stream(), customClaims.stream()).
                                        distinct().collect(Collectors.toList()));
                    }

                    chain.addPolicy(new OidcCustomScopeAttributeReleasePolicy(
                            CUSTOM_SCOPE, customClaims.stream().collect(Collectors.toList())));
                }
            }

            service.setAttributeReleasePolicy(chain);
        }

        return service;
    }
}
