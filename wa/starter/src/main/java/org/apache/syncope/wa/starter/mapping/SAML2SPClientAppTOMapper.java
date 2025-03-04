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

import java.util.Optional;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.configuration.support.TriStateBoolean;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.RegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTicketGrantingTicketExpirationPolicy;
import org.apereo.cas.support.saml.services.SamlRegisteredService;

public class SAML2SPClientAppTOMapper extends AbstractClientAppMapper {

    @Override
    public boolean supports(final ClientAppTO clientApp) {
        return SAML2SPClientAppTO.class.equals(clientApp.getClass());
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

        SAML2SPClientAppTO sp = SAML2SPClientAppTO.class.cast(clientApp.getClientAppTO());
        SamlRegisteredService service = new SamlRegisteredService();
        setCommon(service, sp);

        service.setServiceId(sp.getEntityId());

        service.setMetadataLocation(sp.getMetadataLocation());
        service.setMetadataSignatureLocation(sp.getMetadataSignatureLocation());
        service.setSignAssertions(TriStateBoolean.fromBoolean(sp.isSignAssertions()));
        service.setSignResponses(TriStateBoolean.fromBoolean(sp.isSignResponses()));
        service.setEncryptionOptional(sp.isEncryptionOptional());
        service.setEncryptAssertions(sp.isEncryptAssertions());
        service.setRequiredAuthenticationContextClass(sp.getRequiredAuthenticationContextClass());
        service.setRequiredNameIdFormat(sp.getRequiredNameIdFormat().getNameId());
        service.setSkewAllowance(Optional.ofNullable(sp.getSkewAllowance()).orElse(0));
        service.setNameIdQualifier(sp.getNameIdQualifier());
        if (!sp.getAssertionAudiences().isEmpty()) {
            service.setAssertionAudiences(String.join(",", sp.getAssertionAudiences()));
        }
        service.setServiceProviderNameIdQualifier(sp.getServiceProviderNameIdQualifier());

        setPolicies(service, authPolicy, mfaPolicy, accessStrategy, attributeReleasePolicy,
                tgtExpirationPolicy, stExpirationPolicy, tgtProxyExpirationPolicy, stProxyExpirationPolicy);

        return service;
    }
}
