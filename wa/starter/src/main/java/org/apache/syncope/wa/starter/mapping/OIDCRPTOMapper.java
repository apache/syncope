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

import java.util.Arrays;
import java.util.HashSet;
import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.springframework.stereotype.Component;

@ClientAppMapFor(clientAppClass = OIDCRPTO.class, registeredServiceClass = OidcRegisteredService.class)
@Component
public class OIDCRPTOMapper implements ClientAppMapper {

    @Override
    public RegisteredService build(
            final ClientAppTO clientAppTO,
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        OIDCRPTO rp = OIDCRPTO.class.cast(clientAppTO);

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
    }

    public ClientAppTO buid(final RegisteredService service) {
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

        return oidcrpto;
    }
}
