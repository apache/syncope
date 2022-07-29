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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.auth.MFAAuthModuleConf;
import org.apache.syncope.common.lib.auth.Pac4jAuthModuleConf;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.MultifactorAuthenticationHandler;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria;
import org.apereo.cas.services.DefaultRegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.DefaultRegisteredServiceDelegatedAuthenticationPolicy;
import org.apereo.cas.services.DefaultRegisteredServiceMultifactorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;

@AuthMapFor(authPolicyConfClass = DefaultAuthPolicyConf.class)
public class DefaultAuthMapper implements AuthMapper {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultAuthMapper.class);

    @Override
    public AuthMapperResult build(
            final ConfigurableApplicationContext ctx,
            final String pac4jCoreName,
            final ObjectProvider<AuthenticationEventExecutionPlan> authEventExecPlan,
            final AuthPolicyTO policy,
            final List<AuthModuleTO> authModules) {

        DefaultRegisteredServiceAuthenticationPolicy authPolicy = new DefaultRegisteredServiceAuthenticationPolicy();

        Set<String> mfaAuthHandlers = new HashSet<>();
        Set<Pair<String, String>> delegatedAuthHandlers = new HashSet<>();

        DefaultAuthPolicyConf policyConf = (DefaultAuthPolicyConf) policy.getConf();
        if (!policyConf.getAuthModules().isEmpty()) {
            Set<String> authHandlers = new HashSet<>(policyConf.getAuthModules());
            mfaAuthHandlers.addAll(authEventExecPlan.getObject().getAuthenticationHandlers().stream().
                    filter(MultifactorAuthenticationHandler.class::isInstance).
                    filter(mfaAuthHander -> policyConf.getAuthModules().contains(mfaAuthHander.getName())).
                    map(AuthenticationHandler::getName).
                    collect(Collectors.toSet()));
            authHandlers.removeAll(mfaAuthHandlers);

            delegatedAuthHandlers.addAll(authModules.stream().
                    filter(m -> m.getConf() instanceof Pac4jAuthModuleConf).
                    map(m -> Pair.of(
                    m.getKey(),
                    Optional.ofNullable(((Pac4jAuthModuleConf) m.getConf()).getClientName()).orElse(m.getKey()))).
                    collect(Collectors.toSet()));
            if (!delegatedAuthHandlers.isEmpty()) {
                authHandlers.removeAll(delegatedAuthHandlers.stream().map(Pair::getLeft).collect(Collectors.toSet()));
                authHandlers.add(pac4jCoreName);
            }

            authPolicy.setRequiredAuthenticationHandlers(authHandlers);
        }

        AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria criteria =
                new AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria();
        criteria.setTryAll(policyConf.isTryAll());
        authPolicy.setCriteria(criteria);

        DefaultRegisteredServiceMultifactorPolicy mfaPolicy = null;
        if (!mfaAuthHandlers.isEmpty()) {
            Set<String> fns = mfaAuthHandlers.stream().
                    map(handler -> authModules.stream().filter(am -> handler.equals(am.getKey())).findFirst()).
                    filter(Optional::isPresent).
                    map(Optional::get).
                    filter(am -> am.getConf() instanceof MFAAuthModuleConf).
                    map(am -> ((MFAAuthModuleConf) am.getConf()).getFriendlyName()).
                    collect(Collectors.toSet());

            Set<String> mfaProviders = ctx.getBeansOfType(MultifactorAuthenticationProvider.class).values().stream().
                    filter(map -> fns.contains(map.getFriendlyName())).
                    map(MultifactorAuthenticationProvider::getId).
                    collect(Collectors.toSet());

            mfaPolicy = new DefaultRegisteredServiceMultifactorPolicy();
            mfaPolicy.setBypassEnabled(false);
            mfaPolicy.setForceExecution(true);
            mfaPolicy.setMultifactorAuthenticationProviders(mfaProviders);
        }

        DefaultRegisteredServiceDelegatedAuthenticationPolicy delegatedAuthPolicy = null;
        if (!delegatedAuthHandlers.isEmpty()) {
            delegatedAuthPolicy = new DefaultRegisteredServiceDelegatedAuthenticationPolicy();
            delegatedAuthPolicy.getAllowedProviders().addAll(
                    delegatedAuthHandlers.stream().map(Pair::getRight).collect(Collectors.toSet()));
        }

        return new AuthMapperResult(authPolicy, mfaPolicy, delegatedAuthPolicy);
    }
}
