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
package org.apache.syncope.wa.starter;

import java.util.Arrays;
import java.util.HashSet;
import org.apache.syncope.common.lib.policy.AllowedAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyCriteriaConf;
import org.apache.syncope.common.lib.wa.WAClientApp;
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

public final class RegisteredServiceMapper {

    private RegisteredServiceMapper() {
        // private constructor for static utility class
    }

    public static RegisteredService toRegisteredService(final WAClientApp clientApp) {
        DefaultRegisteredServiceAuthenticationPolicy authPolicy = new DefaultRegisteredServiceAuthenticationPolicy();
        AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria criteria =
                new AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria();
        criteria.setTryAll(((DefaultAuthPolicyCriteriaConf) clientApp.getAuthPolicyConf().getCriteria()).isAll());
        authPolicy.setCriteria(criteria);

        RegisteredServiceAccessStrategy accessStrategy = new DefaultRegisteredServiceAccessStrategy(
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
            OidcRegisteredService service = new OidcRegisteredService();

            String redirectURIs = String.join("|", rp.getRedirectUris());
            service.setServiceId(redirectURIs);
            service.setName(rp.getName());
            service.setDescription(rp.getDescription());
            service.setAccessStrategy(accessStrategy);
            service.setAuthenticationPolicy(authPolicy);
            service.setAttributeReleasePolicy(attributeReleasePolicy);

            service.setClientId(rp.getClientId());
            service.setClientSecret(rp.getClientSecret());
            service.setSignIdToken(rp.isSignIdToken());
            service.setJwks(rp.getJwks());
            service.setSubjectType(rp.getSubjectType().name());
            service.setRedirectUrl(redirectURIs);
            service.setSupportedGrantTypes((HashSet<String>) rp.getSupportedGrantTypes());
            service.setSupportedResponseTypes((HashSet<String>) rp.getSupportedResponseTypes());

            return service;
        } else if (clientApp.getClientAppTO() instanceof SAML2SPTO) {
            SAML2SPTO sp = SAML2SPTO.class.cast(clientApp.getClientAppTO());
            SamlRegisteredService service = new SamlRegisteredService();

            service.setServiceId(sp.getEntityId());
            service.setName(sp.getName());
            service.setDescription(sp.getDescription());
            service.setAccessStrategy(accessStrategy);
            service.setAuthenticationPolicy(authPolicy);
            service.setAttributeReleasePolicy(attributeReleasePolicy);

            service.setMetadataLocation(sp.getMetadataLocation());
            service.setMetadataSignatureLocation(sp.getMetadataSignatureLocation());
            service.setSignAssertions(sp.isSignAssertions());
            service.setSignResponses(sp.isSignResponses());
            service.setEncryptionOptional(sp.isEncryptionOptional());
            service.setEncryptAssertions(sp.isEncryptAssertions());
            service.setRequiredAuthenticationContextClass(sp.getRequiredAuthenticationContextClass());
            service.setRequiredNameIdFormat(sp.getRequiredNameIdFormat().getNameId());
            service.setSkewAllowance(sp.getSkewAllowance());
            service.setNameIdQualifier(sp.getNameIdQualifier());
            service.setAssertionAudiences(sp.getAssertionAudiences());
            service.setServiceProviderNameIdQualifier(sp.getServiceProviderNameIdQualifier());

            return service;
        }

        return null;
    }

    public static WAClientApp fromRegisteredService(final RegisteredService service) {
        WAClientApp clientApp = new WAClientApp();

        if (service.getAuthenticationPolicy() != null) {
            DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
            DefaultAuthPolicyCriteriaConf criteria = new DefaultAuthPolicyCriteriaConf();
            criteria.setAll(((DefaultAuthPolicyCriteriaConf) service.getAuthenticationPolicy().getCriteria()).isAll());
            authPolicyConf.setCriteria(criteria);

            clientApp.setAuthPolicyConf(authPolicyConf);
        }

        if (service.getAccessStrategy() != null) {
            DefaultAccessPolicyConf accessPolicyConf = new DefaultAccessPolicyConf();
            accessPolicyConf.setEnabled(
                    ((DefaultRegisteredServiceAccessStrategy) service.getAccessStrategy()).isEnabled());
            accessPolicyConf.setSsoEnabled(
                    ((DefaultRegisteredServiceAccessStrategy) service.getAccessStrategy()).isSsoEnabled());
            accessPolicyConf.getRequiredAttributes().putAll(
                    ((DefaultRegisteredServiceAccessStrategy) service.getAccessStrategy()).getRejectedAttributes());

            clientApp.setAccessPolicyConf(accessPolicyConf);
        }

        if (service.getAttributeReleasePolicy() != null) {

            if (service.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy) {
                ReturnAllowedAttributeReleasePolicy returnAllowedAttributeReleasePolicy =
                        ReturnAllowedAttributeReleasePolicy.class.cast(service.getAttributeReleasePolicy());
                AllowedAttrReleasePolicyConf allowedAttrReleasePolicyConf = new AllowedAttrReleasePolicyConf();
                allowedAttrReleasePolicyConf.getAllowedAttributes().addAll(returnAllowedAttributeReleasePolicy.
                        getAllowedAttributes());

                clientApp.setAttrReleasePolicyConf(allowedAttrReleasePolicyConf);
            }
        }

        if (service instanceof OidcRegisteredService) {
            OidcRegisteredService oidc = OidcRegisteredService.class.cast(service);
            OIDCRPTO oidcrpto = new OIDCRPTO();

            oidcrpto.getRedirectUris().addAll(Arrays.asList(oidc.getServiceId().split("|")));
            oidcrpto.setName(oidc.getName());
            oidcrpto.setDescription(oidc.getDescription());
            oidcrpto.setClientId(oidc.getClientId());
            oidcrpto.setClientSecret(oidc.getClientSecret());
            oidcrpto.setSignIdToken(oidc.isSignIdToken());
            oidcrpto.setJwks(oidc.getJwks());
            oidcrpto.setSubjectType(OIDCSubjectType.valueOf(oidc.getSubjectType()));
            oidcrpto.getSupportedGrantTypes().addAll(oidc.getSupportedGrantTypes());
            oidcrpto.getSupportedResponseTypes().addAll(oidc.getSupportedResponseTypes());

            clientApp.setClientAppTO(oidcrpto);
        } else if (service instanceof SamlRegisteredService) {
            SamlRegisteredService saml = SamlRegisteredService.class.cast(service);
            SAML2SPTO saml2spto = new SAML2SPTO();

            saml2spto.setEntityId(saml.getServiceId());
            saml2spto.setName(saml.getName());
            saml2spto.setDescription(saml.getDescription());

            saml2spto.setMetadataLocation(saml.getMetadataLocation());
            saml2spto.setMetadataSignatureLocation(saml.getMetadataSignatureLocation());
            saml2spto.setSignAssertions(saml.isSignAssertions());
            saml2spto.setSignResponses(saml.isSignResponses());
            saml2spto.setEncryptionOptional(saml.isEncryptionOptional());
            saml2spto.setEncryptAssertions(saml.isEncryptAssertions());
            saml2spto.setRequiredAuthenticationContextClass(saml.getRequiredAuthenticationContextClass());
            saml2spto.setRequiredNameIdFormat(SAML2SPNameId.valueOf(saml.getRequiredNameIdFormat()));
            saml2spto.setSkewAllowance(saml.getSkewAllowance());
            saml2spto.setNameIdQualifier(saml.getNameIdQualifier());
            saml2spto.setAssertionAudiences(saml.getAssertionAudiences());
            saml2spto.setServiceProviderNameIdQualifier(saml.getServiceProviderNameIdQualifier());

            clientApp.setClientAppTO(saml2spto);
        }

        return clientApp;
    }
}
