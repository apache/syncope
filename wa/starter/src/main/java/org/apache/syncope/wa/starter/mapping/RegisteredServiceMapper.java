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
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.springframework.stereotype.Component;

@Component
public class RegisteredServiceMapper {

    protected final Map<String, AuthMapper> authPolicyConfMappers;

    protected final Map<String, AuthMapper> registeredServiceAuthenticationPolicyMappers;

    protected final Map<String, AccessMapper> accessPolicyConfMappers;

    protected final Map<String, AccessMapper> registeredServiceAccessStrategyMappers;

    protected final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers;

    protected final Map<String, AttrReleaseMapper> registeredServiceAttributeReleasePolicyMappers;

    protected final Map<String, ClientAppMapper> clientAppTOMappers;

    protected final Map<String, ClientAppMapper> registeredServiceMappers;

    public RegisteredServiceMapper(
            final Map<String, AuthMapper> authPolicyConfMappers,
            final Map<String, AuthMapper> registeredServiceAuthenticationPolicyMappers,
            final Map<String, AccessMapper> accessPolicyConfMappers,
            final Map<String, AccessMapper> registeredServiceAccessStrategyMappers,
            final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers,
            final Map<String, AttrReleaseMapper> registeredServiceAttributeReleasePolicyMappers,
            final Map<String, ClientAppMapper> clientAppTOMappers,
            final Map<String, ClientAppMapper> registeredServiceMappers) {

        this.authPolicyConfMappers = authPolicyConfMappers;
        this.registeredServiceAuthenticationPolicyMappers = registeredServiceAuthenticationPolicyMappers;
        this.accessPolicyConfMappers = accessPolicyConfMappers;
        this.registeredServiceAccessStrategyMappers = registeredServiceAccessStrategyMappers;
        this.attrReleasePolicyConfMappers = attrReleasePolicyConfMappers;
        this.registeredServiceAttributeReleasePolicyMappers = registeredServiceAttributeReleasePolicyMappers;
        this.clientAppTOMappers = clientAppTOMappers;
        this.registeredServiceMappers = registeredServiceMappers;
    }

    public RegisteredService toRegisteredService(final WAClientApp clientApp) {
        RegisteredServiceAuthenticationPolicy authPolicy = null;
        if (clientApp.getAuthPolicyConf() != null) {
            AuthMapper authMapper =
                    authPolicyConfMappers.get(clientApp.getAuthPolicyConf().getClass().getName());
            authPolicy = Optional.ofNullable(authMapper).
                    map(mapper -> mapper.build(clientApp.getAuthPolicyConf())).orElse(null);
        }

        RegisteredServiceAccessStrategy accessStrategy = null;
        if (clientApp.getAccessPolicyConf() != null) {
            AccessMapper accessPolicyConfMapper =
                    accessPolicyConfMappers.get(clientApp.getAccessPolicyConf().getClass().getName());
            accessStrategy = Optional.ofNullable(accessPolicyConfMapper).
                    map(mapper -> mapper.build(clientApp.getAccessPolicyConf())).orElse(null);
        }

        RegisteredServiceAttributeReleasePolicy attributeReleasePolicy = null;
        if (clientApp.getAttrReleasePolicyConf() != null) {
            AttrReleaseMapper attrReleasePolicyConfMapper =
                    attrReleasePolicyConfMappers.get(clientApp.getAttrReleasePolicyConf().getClass().getName());
            attributeReleasePolicy = Optional.ofNullable(attrReleasePolicyConfMapper).
                    map(mapper -> mapper.build(clientApp.getAttrReleasePolicyConf())).orElse(null);
        }

        ClientAppMapper clientAppMapper = clientAppTOMappers.get(clientApp.getClientAppTO().getClass().getName());
        if (clientAppMapper == null) {
            return null;
        }
        return clientAppMapper.build(clientApp.getClientAppTO(), authPolicy, accessStrategy, attributeReleasePolicy);
    }

    public WAClientApp fromRegisteredService(final RegisteredService service) {
        WAClientApp clientApp = new WAClientApp();

        if (service.getAuthenticationPolicy() != null) {
            AuthMapper authMapper = registeredServiceAuthenticationPolicyMappers.get(
                    service.getAuthenticationPolicy().getClass().getName());
            clientApp.setAuthPolicyConf(Optional.ofNullable(authMapper).
                    map(mapper -> mapper.build(service.getAuthenticationPolicy())).orElse(null));
        }

        if (service.getAccessStrategy() != null) {
            AccessMapper accessPolicyConfMapper = registeredServiceAccessStrategyMappers.get(
                    service.getAccessStrategy().getClass().getName());
            clientApp.setAccessPolicyConf(Optional.ofNullable(accessPolicyConfMapper).
                    map(mapper -> mapper.build(service.getAccessStrategy())).orElse(null));
        }

        if (service.getAttributeReleasePolicy() != null) {
            AttrReleaseMapper attrReleasePolicyConfMapper = registeredServiceAttributeReleasePolicyMappers.get(
                    service.getAttributeReleasePolicy().getClass().getName());
            clientApp.setAttrReleasePolicyConf(Optional.ofNullable(attrReleasePolicyConfMapper).
                    map(mapper -> mapper.build(service.getAttributeReleasePolicy())).orElse(null));
        }

        ClientAppMapper clientAppMapper = registeredServiceMappers.get(service.getClass().getName());
        clientApp.setClientAppTO(Optional.ofNullable(clientAppMapper).
                map(mapper -> mapper.buid(service)).orElse(null));

        return clientApp;
    }
}
