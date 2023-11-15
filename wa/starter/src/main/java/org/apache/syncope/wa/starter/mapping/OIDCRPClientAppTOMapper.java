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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.OIDCScopeConstants;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.oidc.claims.OidcAddressScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcEmailScopeAttributeReleasePolicy;
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
            final RegisteredServiceProxyTicketExpirationPolicy stProxyExpirationPolicy) {

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
        if (StringUtils.isNotBlank(rp.getJwksUri())) {
            service.setJwks(rp.getJwksUri());
        } else {
            service.setJwks(rp.getJwks());
        }
        service.setSupportedGrantTypes(rp.getSupportedGrantTypes().stream().
                map(OIDCGrantType::name).collect(Collectors.toSet()));
        service.setSupportedResponseTypes(rp.getSupportedResponseTypes().stream().
                map(OIDCResponseType::getExternalForm).collect(Collectors.toSet()));
        Optional.ofNullable(rp.getSubjectType()).ifPresent(st -> service.setSubjectType(st.name()));
        service.setLogoutUrl(rp.getLogoutUri());
        service.setTokenEndpointAuthenticationMethod(rp.getTokenEndpointAuthenticationMethod().name());

        service.setScopes(new HashSet<>(rp.getScopes()));

        Set<String> customClaims = new HashSet<>();
        if (attributeReleasePolicy instanceof BaseMappedAttributeReleasePolicy baseMapped) {
            customClaims.addAll(baseMapped.
                    getAllowedAttributes().values().stream().
                    map(Objects::toString).collect(Collectors.toSet()));
        } else if (attributeReleasePolicy instanceof ReturnAllowedAttributeReleasePolicy returnAllowed) {
            customClaims.addAll(returnAllowed.
                    getAllowedAttributes().stream().collect(Collectors.toSet()));
        } else if (attributeReleasePolicy instanceof ChainingAttributeReleasePolicy chaining) {
            chaining.getPolicies().stream().
                    filter(ReturnAllowedAttributeReleasePolicy.class::isInstance).
                    findFirst().map(ReturnAllowedAttributeReleasePolicy.class::cast).
                    map(p -> p.getAllowedAttributes().stream().collect(Collectors.toSet())).
                    ifPresent(customClaims::addAll);
        }
        if (rp.getScopes().contains(OIDCScopeConstants.PROFILE)) {
            customClaims.removeAll(OidcProfileScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScopeConstants.ADDRESS)) {
            customClaims.removeAll(OidcAddressScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScopeConstants.EMAIL)) {
            customClaims.removeAll(OidcEmailScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }
        if (rp.getScopes().contains(OIDCScopeConstants.PHONE)) {
            customClaims.removeAll(OidcPhoneScopeAttributeReleasePolicy.ALLOWED_CLAIMS);
        }

        if (!customClaims.isEmpty()) {
            service.getScopes().add(OIDCScopeConstants.SYNCOPE);
        }

        // never set attribute relase policy for OIDC services to avoid becoming scope-free for CAS
        setPolicies(service, authPolicy, mfaPolicy, accessStrategy, null,
                tgtExpirationPolicy, stExpirationPolicy, tgtProxyExpirationPolicy, stProxyExpirationPolicy);

        return service;
    }
}
