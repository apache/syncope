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

import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.springframework.stereotype.Component;

@ClientAppMapFor(clientAppClass = SAML2SPTO.class, registeredServiceClass = SamlRegisteredService.class)
@Component
public class SAML2SPTOMapper implements ClientAppMapper {

    @Override
    public RegisteredService build(
            final ClientAppTO clientAppTO,
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        SAML2SPTO sp = SAML2SPTO.class.cast(clientAppTO);

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

    @Override
    public ClientAppTO buid(final RegisteredService service) {
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

        return saml2spto;
    }
}
