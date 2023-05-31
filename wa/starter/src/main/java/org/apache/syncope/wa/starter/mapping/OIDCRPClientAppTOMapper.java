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
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCScope;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.claims.OidcAddressScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcEmailScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcOpenIdScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcPhoneScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcProfileScopeAttributeReleasePolicy;
import org.apereo.cas.services.BaseMappedAttributeReleasePolicy;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.RegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTicketGrantingTicketExpirationPolicy;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;

public class OIDCRPClientAppTOMapper extends AbstractClientAppMapper {

    private static final String CUSTOM_SCOPE = "syncope";

    @Override
    public boolean supports(final ClientAppTO clientApp) {
        return OIDCRPClientAppTO.class.equals(clientApp.getClass());
    }

    @Override
    public RegisteredService map(
            final WAClientApp clientApp,
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceMultifactorPolicy mfaPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy,
            final RegisteredServiceTicketGrantingTicketExpirationPolicy tgtExpirationPolicy,
            final RegisteredServiceServiceTicketExpirationPolicy stExpirationPolicy,
            final RegisteredServiceProxyGrantingTicketExpirationPolicy tgtProxyExpirationPolicy,
            final RegisteredServiceProxyTicketExpirationPolicy stProxyExpirationPolicy,
            final CasConfigurationProperties properties) {

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
        service.setBypassApprovalPrompt(rp.isBypassApprovalPrompt());
        service.setSupportedGrantTypes(rp.getSupportedGrantTypes().stream().
                map(OIDCGrantType::name).collect(Collectors.toCollection(HashSet::new)));
        service.setSupportedResponseTypes(rp.getSupportedResponseTypes().stream().
                map(OIDCResponseType::getExternalForm).collect(Collectors.toCollection(HashSet::new)));
        if (rp.getSubjectType() != null) {
            service.setSubjectType(rp.getSubjectType().name());
        }
        service.setLogoutUrl(rp.getLogoutUri());

        ChainingAttributeReleasePolicy chain;
        if (attributeReleasePolicy instanceof ChainingAttributeReleasePolicy chainingAttributeReleasePolicy) {
            chain = chainingAttributeReleasePolicy;
        } else {
            chain = new ChainingAttributeReleasePolicy();
            if (attributeReleasePolicy != null) {
                chain.addPolicies(attributeReleasePolicy);
            }
        }

        if (rp.getScopes().contains(OIDCScope.OPENID)) {
            chain.addPolicies(new OidcOpenIdScopeAttributeReleasePolicy());
        }
        if (rp.getScopes().contains(OIDCScope.PROFILE)) {
            chain.addPolicies(new OidcProfileScopeAttributeReleasePolicy());
        }
        if (rp.getScopes().contains(OIDCScope.ADDRESS)) {
            chain.addPolicies(new OidcAddressScopeAttributeReleasePolicy());
        }
        if (rp.getScopes().contains(OIDCScope.EMAIL)) {
            chain.addPolicies(new OidcEmailScopeAttributeReleasePolicy());
        }
        if (rp.getScopes().contains(OIDCScope.PHONE)) {
            chain.addPolicies(new OidcPhoneScopeAttributeReleasePolicy());
        }

        Set<String> customClaims = new HashSet<>();
        if (attributeReleasePolicy instanceof BaseMappedAttributeReleasePolicy) {
            customClaims.addAll(((BaseMappedAttributeReleasePolicy) attributeReleasePolicy).
                    getAllowedAttributes().values().stream().
                    map(Objects::toString).collect(Collectors.toSet()));
        } else if (attributeReleasePolicy instanceof ReturnAllowedAttributeReleasePolicy) {
            customClaims.addAll(((ReturnAllowedAttributeReleasePolicy) attributeReleasePolicy).
                    getAllowedAttributes().stream().collect(Collectors.toSet()));
        } else if (attributeReleasePolicy instanceof ChainingAttributeReleasePolicy) {
            ((ChainingAttributeReleasePolicy) attributeReleasePolicy).getPolicies().stream().
                    filter(ReturnAllowedAttributeReleasePolicy.class::isInstance).
                    findFirst().map(ReturnAllowedAttributeReleasePolicy.class::cast).
                    map(p -> p.getAllowedAttributes().stream().collect(Collectors.toSet())).
                    ifPresent(customClaims::addAll);
        }
        if (rp.getScopes().contains(OIDCScope.PROFILE)) {
            customClaims.removeAll(OidcProfileScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScope.ADDRESS)) {
            customClaims.removeAll(OidcAddressScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScope.EMAIL)) {
            customClaims.removeAll(OidcEmailScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScope.PHONE)) {
            customClaims.removeAll(OidcPhoneScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (!customClaims.isEmpty()) {
            List<String> supportedClaims = properties.getAuthn().getOidc().getDiscovery().getClaims();
            if (!supportedClaims.containsAll(customClaims)) {
                properties.getAuthn().getOidc().getDiscovery().setClaims(
                        Stream.concat(supportedClaims.stream(), customClaims.stream()).
                                distinct().collect(Collectors.toList()));
            }

            chain.addPolicies(new OidcCustomScopeAttributeReleasePolicy(
                    CUSTOM_SCOPE, customClaims.stream().collect(Collectors.toList())));
        }

        setPolicies(service, authPolicy, mfaPolicy, accessStrategy, chain,
                tgtExpirationPolicy, stExpirationPolicy, tgtProxyExpirationPolicy, stProxyExpirationPolicy);

        return service;
    }
}
