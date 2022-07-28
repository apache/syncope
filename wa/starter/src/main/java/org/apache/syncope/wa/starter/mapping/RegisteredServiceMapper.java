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
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceDelegatedAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;

public class RegisteredServiceMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RegisteredServiceMapper.class);

    protected final ConfigurableApplicationContext ctx;

    protected final String pac4jCoreName;

    protected final ObjectProvider<AuthenticationEventExecutionPlan> authEventExecPlan;

    protected final Map<String, AuthMapper> authPolicyConfMappers;

    protected final Map<String, AccessMapper> accessPolicyConfMappers;

    protected final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers;

    protected final Map<String, ClientAppMapper> clientAppTOMappers;

    public RegisteredServiceMapper(
            final ConfigurableApplicationContext ctx,
            final String pac4jCoreName,
            final ObjectProvider<AuthenticationEventExecutionPlan> authEventExecPlan,
            final Map<String, AuthMapper> authPolicyConfMappers,
            final Map<String, AccessMapper> accessPolicyConfMappers,
            final Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers,
            final Map<String, ClientAppMapper> clientAppTOMappers) {

        this.ctx = ctx;
        this.pac4jCoreName = pac4jCoreName;
        this.authEventExecPlan = authEventExecPlan;
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
        RegisteredServiceDelegatedAuthenticationPolicy delegatedAuthPolicy = null;
        if (clientApp.getAuthPolicy() != null) {
            AuthMapper authMapper = authPolicyConfMappers.get(
                    clientApp.getAuthPolicy().getConf().getClass().getName());
            AuthMapperResult result = Optional.ofNullable(authMapper).map(mapper -> mapper.build(
                    ctx, pac4jCoreName, authEventExecPlan, clientApp.getAuthPolicy(), clientApp.getAuthModules())).
                    orElseGet(() -> new AuthMapperResult(null, null, null));
            authPolicy = result.getAuthPolicy();
            mfaPolicy = result.getMfaPolicy();
            delegatedAuthPolicy = result.getDelegateAuthPolicy();
        }

        RegisteredServiceAccessStrategy accessStrategy = null;
        if (clientApp.getAccessPolicy() != null) {
            AccessMapper accessPolicyConfMapper = accessPolicyConfMappers.get(
                    clientApp.getAccessPolicy().getConf().getClass().getName());
            accessStrategy = Optional.ofNullable(accessPolicyConfMapper).
                    map(mapper -> mapper.build(clientApp.getAccessPolicy())).
                    orElse(null);
        }
        if (delegatedAuthPolicy != null) {
            if (accessStrategy == null) {
                accessStrategy = new DefaultRegisteredServiceAccessStrategy();
            }
            if (accessStrategy instanceof DefaultRegisteredServiceAccessStrategy) {
                ((DefaultRegisteredServiceAccessStrategy) accessStrategy).
                        setDelegatedAuthenticationPolicy(delegatedAuthPolicy);
            } else {
                LOG.warn("Could not set delegated auth policy because access strategy is instance of {}",
                        accessStrategy.getClass().getName());
            }
        }

        AttrReleasePolicyTO attrReleasePolicyTO = Optional.ofNullable(clientApp.getAttrReleasePolicy()).
                orElseGet(() -> {
                    AttrReleasePolicyTO arpTO = new AttrReleasePolicyTO();
                    arpTO.setConf(new DefaultAttrReleasePolicyConf());
                    return arpTO;
                });
        AttrReleaseMapper attrReleasePolicyConfMapper = attrReleasePolicyConfMappers.get(
                attrReleasePolicyTO.getConf().getClass().getName());
        RegisteredServiceAttributeReleasePolicy attributeReleasePolicy =
                Optional.ofNullable(attrReleasePolicyConfMapper).
                        map(mapper -> mapper.build(attrReleasePolicyTO, clientApp.getReleaseAttrs())).
                        orElse(null);

        return clientAppMapper.map(ctx, clientApp, authPolicy, mfaPolicy, accessStrategy, attributeReleasePolicy);
    }
}
