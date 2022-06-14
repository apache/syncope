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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.ReturnMappedAttributeReleasePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;

public class RegisteredServiceMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RegisteredServiceMapper.class);

    protected final ConfigurableApplicationContext ctx;

    protected final ObjectProvider<AuthenticationEventExecutionPlan> authenticationEventExecutionPlan;

    protected final Map<String, AuthMapper> authPolicyConfMappers;

    protected final Map<String, AccessMapper> accessPolicyConfMappers;

    protected final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers;

    protected final Map<String, ClientAppMapper> clientAppTOMappers;

    public RegisteredServiceMapper(
            final ConfigurableApplicationContext ctx,
            final ObjectProvider<AuthenticationEventExecutionPlan> authenticationEventExecutionPlan,
            final Map<String, AuthMapper> authPolicyConfMappers,
            final Map<String, AccessMapper> accessPolicyConfMappers,
            final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers,
            final Map<String, ClientAppMapper> clientAppTOMappers) {

        this.ctx = ctx;
        this.authenticationEventExecutionPlan = authenticationEventExecutionPlan;
        this.authPolicyConfMappers = authPolicyConfMappers;
        this.accessPolicyConfMappers = accessPolicyConfMappers;
        this.attrReleasePolicyConfMappers = attrReleasePolicyConfMappers;
        this.clientAppTOMappers = clientAppTOMappers;
    }

    public RegisteredService toRegisteredService(final WAClientApp clientApp) {
        String key = clientApp.getClientAppTO().getClass().getName();
        ClientAppMapper clientAppMapper = clientAppTOMappers.get(key);
        if (clientAppMapper == null) {
            LOG.warn("Unable to locate ClientAppMapper using key {}", key);
            return null;
        }

        RegisteredServiceAuthenticationPolicy authPolicy = null;
        RegisteredServiceMultifactorPolicy mfaPolicy = null;
        if (clientApp.getAuthPolicy() != null) {
            AuthMapper authMapper = authPolicyConfMappers.get(
                    clientApp.getAuthPolicy().getConf().getClass().getName());
            Pair<RegisteredServiceAuthenticationPolicy, RegisteredServiceMultifactorPolicy> mapped =
                    Optional.ofNullable(authMapper).map(mapper -> mapper.build(
                    ctx, authenticationEventExecutionPlan, clientApp.getAuthPolicy(), clientApp.getAuthModules())).
                            orElseGet(() -> Pair.of(null, null));
            if (mapped.getLeft() != null) {
                authPolicy = mapped.getLeft();
            }
            if (mapped.getRight() != null) {
                mfaPolicy = mapped.getRight();
            }
        }

        RegisteredServiceAccessStrategy accessStrategy = null;
        if (clientApp.getAccessPolicy() != null) {
            AccessMapper accessPolicyConfMapper = accessPolicyConfMappers.get(
                    clientApp.getAccessPolicy().getConf().getClass().getName());
            accessStrategy = Optional.ofNullable(accessPolicyConfMapper).
                    map(mapper -> mapper.build(clientApp.getAccessPolicy())).orElse(null);
        }

        RegisteredServiceAttributeReleasePolicy attributeReleasePolicy = null;
        if (clientApp.getAttrReleasePolicy() == null) {
            if (!clientApp.getReleaseAttrs().isEmpty()) {
                attributeReleasePolicy = new ReturnMappedAttributeReleasePolicy(clientApp.getReleaseAttrs());
            }
        } else {
            AttrReleaseMapper attrReleasePolicyConfMapper = attrReleasePolicyConfMappers.get(
                    clientApp.getAttrReleasePolicy().getConf().getClass().getName());
            attributeReleasePolicy = Optional.ofNullable(attrReleasePolicyConfMapper).
                    map(mapper -> mapper.build(clientApp.getAttrReleasePolicy())).orElse(null);
        }

        return clientAppMapper.map(clientApp, authPolicy, mfaPolicy, accessStrategy, attributeReleasePolicy);
    }
}
