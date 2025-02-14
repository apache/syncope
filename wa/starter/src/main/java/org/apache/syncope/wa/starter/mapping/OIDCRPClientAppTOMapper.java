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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.OIDCScopeConstants;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
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

        service.setIdTokenIssuer(rp.getIdTokenIssuer());
        service.setSignIdToken(rp.isSignIdToken());
        if (service.isSignIdToken()) {
            Optional.ofNullable(rp.getIdTokenSigningAlg()).
                    filter(v -> v != OIDCTokenSigningAlg.none).
                    ifPresent(v -> service.setIdTokenSigningAlg(v.name()));
        } else {
            service.setIdTokenSigningAlg(OIDCTokenSigningAlg.none.name());
        }
        service.setEncryptIdToken(rp.isEncryptIdToken());
        if (service.isEncryptIdToken()) {
            Optional.ofNullable(rp.getIdTokenEncryptionAlg()).
                    filter(v -> v != OIDCTokenEncryptionAlg.none).
                    ifPresent(v -> service.setIdTokenEncryptionAlg(v.getExternalForm()));
            Optional.ofNullable(rp.getIdTokenEncryptionEncoding()).
                    ifPresent(v -> service.setIdTokenEncryptionEncoding(v.getExternalForm()));
        } else {
            service.setIdTokenEncryptionAlg(OIDCTokenEncryptionAlg.none.getExternalForm());
        }
        Optional.ofNullable(rp.getUserInfoSigningAlg()).ifPresent(v -> service.setUserInfoSigningAlg(v.name()));
        Optional.ofNullable(rp.getUserInfoEncryptedResponseAlg()).
                ifPresent(v -> service.setUserInfoEncryptedResponseAlg(v.getExternalForm()));
        Optional.ofNullable(rp.getUserInfoEncryptedResponseEncoding()).
                ifPresent(v -> service.setUserInfoEncryptedResponseEncoding(v.getExternalForm()));

        service.setJwtAccessToken(rp.isJwtAccessToken());
        service.setBypassApprovalPrompt(rp.isBypassApprovalPrompt());
        service.setGenerateRefreshToken(rp.isGenerateRefreshToken());
        if (StringUtils.isNotBlank(rp.getJwksUri())) {
            service.setJwks(rp.getJwksUri());
        } else {
            service.setJwks(rp.getJwks());
        }
        Optional.ofNullable(rp.getSubjectType()).ifPresent(v -> service.setSubjectType(v.getExternalForm()));
        Optional.ofNullable(rp.getApplicationType()).ifPresent(v -> service.setApplicationType(v.getExternalForm()));
        service.setSupportedGrantTypes(rp.getSupportedGrantTypes().stream().
                map(OIDCGrantType::getExternalForm).collect(Collectors.toSet()));
        service.setSupportedResponseTypes(rp.getSupportedResponseTypes().stream().
                map(OIDCResponseType::getExternalForm).collect(Collectors.toSet()));
        service.setLogoutUrl(rp.getLogoutUri());
        service.setTokenEndpointAuthenticationMethod(rp.getTokenEndpointAuthenticationMethod().name());

        service.setScopes(new HashSet<>(rp.getScopes()));

        if (attributeReleasePolicy instanceof OidcCustomScopeAttributeReleasePolicy
                || (attributeReleasePolicy instanceof ChainingAttributeReleasePolicy chain
                && chain.getPolicies().stream().anyMatch(OidcCustomScopeAttributeReleasePolicy.class::isInstance))) {

            service.getScopes().add(OIDCScopeConstants.SYNCOPE);
        }

        setPolicies(service, authPolicy, mfaPolicy, accessStrategy, attributeReleasePolicy,
                tgtExpirationPolicy, stExpirationPolicy, tgtProxyExpirationPolicy, stProxyExpirationPolicy);

        return service;
    }
}
