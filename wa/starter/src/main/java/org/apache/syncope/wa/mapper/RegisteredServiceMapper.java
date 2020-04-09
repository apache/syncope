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
package org.apache.syncope.wa.mapper;

import java.util.Arrays;
import java.util.HashSet;
import org.apache.syncope.common.lib.policy.AllowedAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyCriteriaConf;
import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.DefaultRegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.support.saml.services.SamlRegisteredService;

public class RegisteredServiceMapper {

    public RegisteredService toRegisteredService(final RegisteredClientAppTO clientApp) {

        DefaultRegisteredServiceAuthenticationPolicy authenticationPolicy =
                new DefaultRegisteredServiceAuthenticationPolicy();
        AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria criteria =
                new AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria();
        criteria.setTryAll(((DefaultAuthPolicyCriteriaConf) clientApp.getAuthPolicyConf().getCriteria()).isAll());
        authenticationPolicy.setCriteria(criteria);

        RegisteredServiceAccessStrategy accessStrategy =
                new DefaultRegisteredServiceAccessStrategy(
                        clientApp.getAccessPolicyConf().isEnabled(), clientApp.getAccessPolicyConf().isSsoEnabled());
        accessStrategy.getRequiredAttributes().putAll(clientApp.getAccessPolicyConf().getRequiredAttributes());

        RegisteredServiceAttributeReleasePolicy attributeReleasePolicy;
        if (clientApp.getAttrReleasePolicyConf() != null
                && clientApp.getAttrReleasePolicyConf() instanceof AllowedAttrReleasePolicyConf
                && !((AllowedAttrReleasePolicyConf) clientApp.getAttrReleasePolicyConf()).
                        getAllowedAttributes().isEmpty()) {
            attributeReleasePolicy = new ReturnAllowedAttributeReleasePolicy();
            ((AllowedAttrReleasePolicyConf) clientApp.getAttrReleasePolicyConf()).getAllowedAttributes();
            ((ReturnAllowedAttributeReleasePolicy) attributeReleasePolicy).getAllowedAttributes().addAll(
                    ((AllowedAttrReleasePolicyConf) clientApp.getAttrReleasePolicyConf()).getAllowedAttributes());
        } else {
            attributeReleasePolicy = new DenyAllAttributeReleasePolicy();
        }

        if (clientApp.getClientAppTO() instanceof OIDCRPTO) {
            OIDCRPTO rp = OIDCRPTO.class.cast(clientApp.getClientAppTO());
            OidcRegisteredService registeredService = new OidcRegisteredService();

            String redirectURIs = String.join("|", rp.getRedirectUris());
            registeredService.setServiceId(redirectURIs);
            registeredService.setName(rp.getName());
            registeredService.setDescription(rp.getDescription());
            registeredService.setAccessStrategy(accessStrategy);
            registeredService.setAuthenticationPolicy(authenticationPolicy);
            registeredService.setAttributeReleasePolicy(attributeReleasePolicy);

            registeredService.setClientId(rp.getClientId());
            registeredService.setClientSecret(rp.getClientSecret());
            registeredService.setSignIdToken(rp.isSignIdToken());
            registeredService.setJwks(rp.getJwks());
            registeredService.setSubjectType(rp.getSubjectType().name());
            registeredService.setRedirectUrl(redirectURIs);
            registeredService.setSupportedGrantTypes((HashSet<String>) rp.getSupportedGrantTypes());
            registeredService.setSupportedResponseTypes((HashSet<String>) rp.getSupportedResponseTypes());

            return registeredService;
        } else if (clientApp.getClientAppTO() instanceof SAML2SPTO) {
            SAML2SPTO sp = SAML2SPTO.class.cast(clientApp.getClientAppTO());
            SamlRegisteredService registeredService = new SamlRegisteredService();

            registeredService.setServiceId(sp.getEntityId());
            registeredService.setName(sp.getName());
            registeredService.setDescription(sp.getDescription());
            registeredService.setAccessStrategy(accessStrategy);
            registeredService.setAuthenticationPolicy(authenticationPolicy);
            registeredService.setAttributeReleasePolicy(attributeReleasePolicy);

            registeredService.setMetadataLocation(sp.getMetadataLocation());
            registeredService.setMetadataSignatureLocation(sp.getMetadataSignatureLocation());
            registeredService.setSignAssertions(sp.isSignAssertions());
            registeredService.setSignResponses(sp.isSignResponses());
            registeredService.setEncryptionOptional(sp.isEncryptionOptional());
            registeredService.setEncryptAssertions(sp.isEncryptAssertions());
            registeredService.setRequiredAuthenticationContextClass(sp.getRequiredAuthenticationContextClass());
            registeredService.setRequiredNameIdFormat(sp.getRequiredNameIdFormat().getNameId());
            registeredService.setSkewAllowance(sp.getSkewAllowance());
            registeredService.setNameIdQualifier(sp.getNameIdQualifier());
            registeredService.setAssertionAudiences(sp.getAssertionAudiences());
            registeredService.setServiceProviderNameIdQualifier(sp.getServiceProviderNameIdQualifier());
            return registeredService;
        }
        return null;
    }

    public RegisteredClientAppTO fromRegisteredService(final RegisteredService registeredService) {
        RegisteredClientAppTO clientApp = new RegisteredClientAppTO();

        if (registeredService.getAuthenticationPolicy() != null) {
            DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
            DefaultAuthPolicyCriteriaConf criteria = new DefaultAuthPolicyCriteriaConf();
            criteria.setAll(((DefaultAuthPolicyCriteriaConf) registeredService.
                    getAuthenticationPolicy().getCriteria()).isAll());
            authPolicyConf.setCriteria(criteria);

            clientApp.setAuthPolicyConf(authPolicyConf);
        }

        if (registeredService.getAccessStrategy() != null) {
            DefaultAccessPolicyConf accessPolicyConf = new DefaultAccessPolicyConf();
            accessPolicyConf.setEnabled(
                    ((DefaultRegisteredServiceAccessStrategy) registeredService.getAccessStrategy()).
                            isEnabled());
            accessPolicyConf.setSsoEnabled(((DefaultRegisteredServiceAccessStrategy) registeredService.
                    getAccessStrategy()).
                    isSsoEnabled());
            accessPolicyConf.getRequiredAttributes().putAll(((DefaultRegisteredServiceAccessStrategy) registeredService.
                    getAccessStrategy()).getRejectedAttributes());

            clientApp.setAccessPolicyConf(accessPolicyConf);
        }

        if (registeredService.getAttributeReleasePolicy() != null) {

            if (registeredService.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy) {
                ReturnAllowedAttributeReleasePolicy returnAllowedAttributeReleasePolicy =
                        ReturnAllowedAttributeReleasePolicy.class.cast(registeredService.getAttributeReleasePolicy());
                AllowedAttrReleasePolicyConf allowedAttrReleasePolicyConf = new AllowedAttrReleasePolicyConf();
                allowedAttrReleasePolicyConf.getAllowedAttributes().addAll(returnAllowedAttributeReleasePolicy.
                        getAllowedAttributes());

                clientApp.setAttrReleasePolicyConf(allowedAttrReleasePolicyConf);
            }
        }

        if (registeredService instanceof OidcRegisteredService) {
            OidcRegisteredService oidcRegisteredService = OidcRegisteredService.class.cast(registeredService);
            OIDCRPTO oidcrpto = new OIDCRPTO();

            Arrays.asList(registeredService.getServiceId().split("|")).forEach(redirectURI
                    -> oidcrpto.getRedirectUris().add(redirectURI));
            oidcrpto.setName(oidcRegisteredService.getName());
            oidcrpto.setDescription(oidcRegisteredService.getDescription());
            oidcrpto.setClientId(oidcRegisteredService.getClientId());
            oidcrpto.setClientSecret(oidcRegisteredService.getClientSecret());
            oidcrpto.setSignIdToken(oidcRegisteredService.isSignIdToken());
            oidcrpto.setJwks(oidcRegisteredService.getJwks());
            oidcrpto.setSubjectType(OIDCSubjectType.valueOf(oidcRegisteredService.getSubjectType()));
            oidcrpto.getSupportedGrantTypes().addAll(oidcRegisteredService.getSupportedGrantTypes());
            oidcrpto.getSupportedResponseTypes().addAll(oidcRegisteredService.getSupportedResponseTypes());

            clientApp.setClientAppTO(oidcrpto);
        } else if (registeredService instanceof SamlRegisteredService) {
            SamlRegisteredService samlRegisteredService = SamlRegisteredService.class.cast(registeredService);
            SAML2SPTO saml2spto = new SAML2SPTO();

            saml2spto.setEntityId(samlRegisteredService.getServiceId());
            saml2spto.setName(samlRegisteredService.getName());
            saml2spto.setDescription(samlRegisteredService.getDescription());

            saml2spto.setMetadataLocation(samlRegisteredService.getMetadataLocation());
            saml2spto.setMetadataSignatureLocation(samlRegisteredService.getMetadataSignatureLocation());
            saml2spto.setSignAssertions(samlRegisteredService.isSignAssertions());
            saml2spto.setSignResponses(samlRegisteredService.isSignResponses());
            saml2spto.setEncryptionOptional(samlRegisteredService.isEncryptionOptional());
            saml2spto.setEncryptAssertions(samlRegisteredService.isEncryptAssertions());
            saml2spto.setRequiredAuthenticationContextClass(samlRegisteredService.
                    getRequiredAuthenticationContextClass());
            saml2spto.setRequiredNameIdFormat(SAML2SPNameId.valueOf(samlRegisteredService.getRequiredNameIdFormat()));
            saml2spto.setSkewAllowance(samlRegisteredService.getSkewAllowance());
            saml2spto.setNameIdQualifier(samlRegisteredService.getNameIdQualifier());
            saml2spto.setAssertionAudiences(samlRegisteredService.getAssertionAudiences());
            saml2spto.setServiceProviderNameIdQualifier(samlRegisteredService.getServiceProviderNameIdQualifier());

            clientApp.setClientAppTO(saml2spto);
        }
        return clientApp;
    }
}
