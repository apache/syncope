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
package org.apache.syncope.wa.starter.oidc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.claims.BaseOidcScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcRegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcScopeFreeAttributeReleasePolicy;
import org.apereo.cas.oidc.scopes.DefaultOidcAttributeReleasePolicyFactory;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAOidcAttributeReleasePolicyFactory extends DefaultOidcAttributeReleasePolicyFactory {

    protected static final Logger LOG = LoggerFactory.getLogger(WAOidcAttributeReleasePolicyFactory.class);

    public WAOidcAttributeReleasePolicyFactory(final CasConfigurationProperties casProperties) {
        super(casProperties);
    }

    @Override
    public Map<String, BaseOidcScopeAttributeReleasePolicy> resolvePolicies(
            final OidcRegisteredService registeredService) {

        Map<String, BaseOidcScopeAttributeReleasePolicy> policies = new HashMap<>(attributeReleasePoliciesByScope);

        Collection<OidcCustomScopeAttributeReleasePolicy> userScopes = getUserDefinedScopes();
        LOG.debug("Configuring attributes release policies for user-defined scopes [{}]", userScopes);
        userScopes.forEach(us -> policies.put(us.getScopeName(), us));

        LOG.debug("Configuring attributes release policies for user-defined scopes specified for service [{}]",
                registeredService.getName());

        List<OidcRegisteredServiceAttributeReleasePolicy> listOfOidcPolicies = new ArrayList<>();
        switch (registeredService.getAttributeReleasePolicy()) {
            case ChainingAttributeReleasePolicy chain ->
                listOfOidcPolicies.addAll(chain.getPolicies().stream().
                        filter(OidcRegisteredServiceAttributeReleasePolicy.class::isInstance).
                        map(OidcRegisteredServiceAttributeReleasePolicy.class::cast).
                        toList());

            case OidcRegisteredServiceAttributeReleasePolicy policy ->
                listOfOidcPolicies.add(policy);

            case null -> {
            }

            default -> {
            }
        }

        listOfOidcPolicies.stream().
                filter(OidcCustomScopeAttributeReleasePolicy.class::isInstance).
                map(OidcCustomScopeAttributeReleasePolicy.class::cast).
                forEach(policy -> policies.put(policy.getScopeName(), policy));
        listOfOidcPolicies.stream().
                filter(OidcScopeFreeAttributeReleasePolicy.class::isInstance).
                map(OidcScopeFreeAttributeReleasePolicy.class::cast).
                forEach(policy -> policies.put(UUID.randomUUID().toString(), policy));

        LOG.debug("Final set of scopes mapped to attribute release policies are [{}]", policies.keySet());
        return policies;
    }
}
