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
package org.apache.syncope.wa.bootstrap.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.syncope.common.lib.OIDCStandardScope;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apereo.cas.authentication.principal.DefaultPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.AbstractPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.CachingPrincipalAttributesRepository;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesCoreProperties.MergingStrategyTypes;
import org.apereo.cas.configuration.support.TriStateBoolean;
import org.apereo.cas.oidc.claims.BaseOidcScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcAddressScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcAssuranceScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcEmailScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcOfflineAccessScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcPhoneScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcProfileScopeAttributeReleasePolicy;
import org.apereo.cas.services.AbstractRegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.ReturnMappedAttributeReleasePolicy;
import org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultAttrReleaseMapper implements AttrReleaseMapper {

    protected static final Logger LOG = LoggerFactory.getLogger(AttrReleaseMapper.class);

    protected static void warnMissingScope(
            final String clientApp,
            final String internal,
            final Object external,
            final String scope) {

        LOG.warn("OIDC client app {} defines claim mapping {}={} but does not specify the related scope {}",
                clientApp, internal, external, scope);
    }

    @Override
    public boolean supports(final AttrReleasePolicyConf conf) {
        return DefaultAttrReleasePolicyConf.class.equals(conf.getClass());
    }

    protected void setPrincipalAttributesRepository(
            final DefaultAttrReleasePolicyConf.PrincipalAttrRepoConf parc,
            final AbstractRegisteredServiceAttributeReleasePolicy policy) {

        AbstractPrincipalAttributesRepository par = parc.getExpiration() > 0
                ? new CachingPrincipalAttributesRepository(parc.getTimeUnit().name(), parc.getExpiration())
                : new DefaultPrincipalAttributesRepository();

        par.setMergingStrategy(MergingStrategyTypes.valueOf(parc.getMergingStrategy().name()));
        par.setIgnoreResolvedAttributes(par.isIgnoreResolvedAttributes());
        par.setAttributeRepositoryIds(new HashSet<>(parc.getAttrRepos()));

        policy.setPrincipalAttributesRepository(par);
    }

    protected Optional<DefaultRegisteredServiceConsentPolicy> buildConsentPolicy(
            final AttrReleasePolicyTO policy,
            final DefaultAttrReleasePolicyConf conf) {

        if (conf.getExcludedAttrs().isEmpty() && conf.getIncludeOnlyAttrs().isEmpty()) {
            return Optional.empty();
        }

        DefaultRegisteredServiceConsentPolicy consentPolicy = new DefaultRegisteredServiceConsentPolicy(
                new HashSet<>(conf.getExcludedAttrs()), new HashSet<>(conf.getIncludeOnlyAttrs()));
        consentPolicy.setOrder(policy.getOrder());
        consentPolicy.setStatus(TriStateBoolean.fromBoolean(policy.getStatus()));
        return Optional.of(consentPolicy);
    }

    protected RegisteredServiceAttributeReleasePolicy build(
            final AttrReleasePolicyTO policy,
            final DefaultAttrReleasePolicyConf conf,
            final AbstractRegisteredServiceAttributeReleasePolicy single,
            final ChainingAttributeReleasePolicy chain) {

        Optional<DefaultRegisteredServiceConsentPolicy> consentPolicy = buildConsentPolicy(policy, conf);
        if (!chain.getPolicies().isEmpty()) {
            Optional.ofNullable(conf.getPrincipalAttrRepoConf()).
                    ifPresent(parc -> chain.setMergingPolicy(
                    MergingStrategyTypes.valueOf(parc.getMergingStrategy().name())));

            chain.getPolicies().stream().
                    filter(AbstractRegisteredServiceAttributeReleasePolicy.class::isInstance).
                    map(AbstractRegisteredServiceAttributeReleasePolicy.class::cast).
                    forEach(p -> {
                        consentPolicy.ifPresent(p::setConsentPolicy);
                        Optional.ofNullable(conf.getPrincipalIdAttr()).ifPresent(p::setPrincipalIdAttribute);

                        Optional.ofNullable(conf.getPrincipalAttrRepoConf()).
                                filter(parc -> !parc.getAttrRepos().isEmpty()).
                                ifPresent(parc -> setPrincipalAttributesRepository(parc, p));
                    });
        }
        Optional.ofNullable(single).ifPresent(p -> {
            consentPolicy.ifPresent(p::setConsentPolicy);
            Optional.ofNullable(conf.getPrincipalIdAttr()).ifPresent(p::setPrincipalIdAttribute);

            Optional.ofNullable(conf.getPrincipalAttrRepoConf()).
                    filter(parc -> !parc.getAttrRepos().isEmpty()).
                    ifPresent(parc -> setPrincipalAttributesRepository(parc, p));
        });

        return single == null ? chain : single;
    }

    @Override
    public RegisteredServiceAttributeReleasePolicy build(final ClientAppTO app, final AttrReleasePolicyTO policy) {
        DefaultAttrReleasePolicyConf conf = (DefaultAttrReleasePolicyConf) policy.getConf();

        ReturnMappedAttributeReleasePolicy returnMapped = null;
        ReturnAllowedAttributeReleasePolicy returnAllowed = null;
        if (!conf.getReleaseAttrs().isEmpty()) {
            returnMapped = new ReturnMappedAttributeReleasePolicy();
            returnMapped.setAllowedAttributes(conf.getReleaseAttrs());

            if (!conf.getAllowedAttrs().isEmpty()) {
                returnAllowed = new ReturnAllowedAttributeReleasePolicy();
                returnAllowed.setAllowedAttributes(conf.getAllowedAttrs());
            }
        }

        ChainingAttributeReleasePolicy chain = new ChainingAttributeReleasePolicy();
        AbstractRegisteredServiceAttributeReleasePolicy single = null;
        if (returnMapped == null && returnAllowed == null) {
            single = new DenyAllAttributeReleasePolicy();
        } else if (returnMapped != null && returnAllowed == null) {
            single = returnMapped;
        } else if (returnMapped == null && returnAllowed != null) {
            single = returnAllowed;
        } else {
            chain.addPolicies(returnMapped, returnAllowed);
        }

        return build(policy, conf, single, chain);
    }

    protected void buildForOIDCStandardScope(
            final OIDCRPClientAppTO clientApp,
            final Map<String, BaseOidcScopeAttributeReleasePolicy> policies,
            final Supplier<BaseOidcScopeAttributeReleasePolicy> attributeReleasePolicyCreator,
            final OIDCStandardScope scope,
            final String internal,
            final String external) {

        if (clientApp.getScopes().contains(scope.name())) {
            BaseOidcScopeAttributeReleasePolicy policy = policies.computeIfAbsent(
                    scope.name(), k -> attributeReleasePolicyCreator.get());

            policy.getClaimMappings().put(external, internal);
        } else {
            warnMissingScope(clientApp.getName(), internal, external, scope.name());
        }
    }

    protected void buildForOIDCustomScope(
            final OIDCRPClientAppTO clientApp,
            final Map<String, BaseOidcScopeAttributeReleasePolicy> policies,
            final String scope,
            final String internal,
            final String external) {

        if (clientApp.getScopes().contains(scope)) {
            BaseOidcScopeAttributeReleasePolicy policy = policies.computeIfAbsent(
                    scope, k -> new OidcCustomScopeAttributeReleasePolicy(scope, new ArrayList<>()));

            policy.getClaimMappings().put(external, internal);

            policy.getAllowedAttributes().add(external);
        } else {
            warnMissingScope(clientApp.getName(), internal, external, scope);
        }
    }

    @Override
    public RegisteredServiceAttributeReleasePolicy build(
            final OIDCRPClientAppTO clientApp,
            final AttrReleasePolicyTO policy,
            final OIDCOpEntityTO oidcOpEntity) {

        if (clientApp.getScopes().isEmpty()) {
            return build(clientApp, policy);
        }

        DefaultAttrReleasePolicyConf conf = (DefaultAttrReleasePolicyConf) policy.getConf();

        Map<String, BaseOidcScopeAttributeReleasePolicy> policies = new HashMap<>();

        conf.getReleaseAttrs().forEach((internal, external) -> {
            if (OidcProfileScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                buildForOIDCStandardScope(
                        clientApp,
                        policies,
                        OidcProfileScopeAttributeReleasePolicy::new,
                        OIDCStandardScope.profile,
                        internal,
                        external.toString());
            } else if (OidcEmailScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                buildForOIDCStandardScope(
                        clientApp,
                        policies,
                        OidcEmailScopeAttributeReleasePolicy::new,
                        OIDCStandardScope.email,
                        internal,
                        external.toString());
            } else if (OidcAddressScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                buildForOIDCStandardScope(
                        clientApp,
                        policies,
                        OidcAddressScopeAttributeReleasePolicy::new,
                        OIDCStandardScope.address,
                        internal,
                        external.toString());
            } else if (OidcPhoneScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                buildForOIDCStandardScope(
                        clientApp,
                        policies,
                        OidcPhoneScopeAttributeReleasePolicy::new,
                        OIDCStandardScope.phone,
                        internal,
                        external.toString());
            } else if (OidcAssuranceScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                buildForOIDCStandardScope(
                        clientApp,
                        policies,
                        OidcPhoneScopeAttributeReleasePolicy::new,
                        OIDCStandardScope.assurance,
                        internal,
                        external.toString());
            } else {
                oidcOpEntity.getCustomScopes().entrySet().stream().
                        filter(entry -> clientApp.getScopes().contains(entry.getKey())
                        && entry.getValue().contains(external.toString())).
                        map(Map.Entry::getKey).findFirst().ifPresentOrElse(
                        scope -> buildForOIDCustomScope(
                                clientApp, policies, scope, internal, external.toString()),
                        () -> LOG.warn(
                                "OIDC client app {} defines custom claim {}={} for which no valid scope could be found",
                                clientApp.getName(), internal, external));
            }
        });

        ChainingAttributeReleasePolicy chain = new ChainingAttributeReleasePolicy();
        AbstractRegisteredServiceAttributeReleasePolicy single = null;
        if (policies.size() == 1) {
            single = policies.values().iterator().next();
        } else {
            // add the custom scopes at the end of the chain
            policies.entrySet().stream().
                    filter(entry -> !(entry.getValue() instanceof OidcCustomScopeAttributeReleasePolicy)).
                    forEach(entry -> chain.addPolicies(entry.getValue()));
            policies.entrySet().stream().
                    filter(entry -> entry.getValue() instanceof OidcCustomScopeAttributeReleasePolicy).
                    forEach(entry -> chain.addPolicies(entry.getValue()));
        }

        if (single == null && chain.getPolicies().isEmpty()) {
            single = new DenyAllAttributeReleasePolicy();
        }

        return build(policy, conf, single, chain);
    }
}
