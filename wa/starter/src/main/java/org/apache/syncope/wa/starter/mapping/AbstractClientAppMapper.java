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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apereo.cas.services.BaseWebBasedRegisteredService;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTicketGrantingTicketExpirationPolicy;

abstract class AbstractClientAppMapper implements ClientAppMapper {

    protected void setCommon(final BaseWebBasedRegisteredService service, final ClientAppTO clientApp) {
        service.setId(clientApp.getClientAppId());
        service.setEvaluationOrder(clientApp.getEvaluationOrder());
        service.setName(clientApp.getName());
        service.setDescription(clientApp.getDescription());
        service.setLogo(clientApp.getLogo());
        service.setTheme(clientApp.getTheme());
        service.setInformationUrl(clientApp.getInformationUrl());
        service.setPrivacyUrl(clientApp.getPrivacyUrl());
        Optional.ofNullable(clientApp.getUsernameAttributeProviderConf()).
                ifPresent(conf -> conf.map(new DefaultUsernameAttributeProviderConfMapper(service)));

        if (!clientApp.getProperties().isEmpty()) {
            Map<String, RegisteredServiceProperty> properties = clientApp.getProperties().stream().
                    collect(Collectors.toMap(
                            Attr::getSchema,
                            attr -> new DefaultRegisteredServiceProperty(attr.getValues()),
                            (existing, replacement) -> existing));
            service.setProperties(properties);
        }

        service.setLogoutType(RegisteredServiceLogoutType.valueOf(clientApp.getLogoutType().name()));
    }

    protected void setPolicies(
            final BaseWebBasedRegisteredService service,
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceMultifactorPolicy mfaPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy,
            final RegisteredServiceTicketGrantingTicketExpirationPolicy tgtExpirationPolicy,
            final RegisteredServiceServiceTicketExpirationPolicy stExpirationPolicy,
            final RegisteredServiceProxyGrantingTicketExpirationPolicy tgtProxyExpirationPolicy,
            final RegisteredServiceProxyTicketExpirationPolicy stProxyExpirationPolicy) {

        Optional.ofNullable(authPolicy).ifPresent(service::setAuthenticationPolicy);

        Optional.ofNullable(mfaPolicy).ifPresent(service::setMultifactorAuthenticationPolicy);

        Optional.ofNullable(accessStrategy).ifPresent(service::setAccessStrategy);

        Optional.ofNullable(attributeReleasePolicy).ifPresent(service::setAttributeReleasePolicy);

        Optional.ofNullable(tgtExpirationPolicy).ifPresent(service::setTicketGrantingTicketExpirationPolicy);

        if (service instanceof final CasRegisteredService casRegisteredService) {

            Optional.ofNullable(stExpirationPolicy).
                    ifPresent(casRegisteredService::setServiceTicketExpirationPolicy);

            Optional.ofNullable(tgtProxyExpirationPolicy).
                    ifPresent(casRegisteredService::setProxyGrantingTicketExpirationPolicy);

            Optional.ofNullable(stProxyExpirationPolicy).
                    ifPresent(casRegisteredService::setProxyTicketExpirationPolicy);
        }
    }
}
