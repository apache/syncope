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
import org.apache.syncope.common.lib.OIDCScopeConstants;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apereo.cas.authentication.principal.DefaultPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.AbstractPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.CachingPrincipalAttributesRepository;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesCoreProperties.MergingStrategyTypes;
import org.apereo.cas.configuration.support.TriStateBoolean;
import org.apereo.cas.oidc.claims.BaseOidcScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcAddressScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcCustomScopeAttributeReleasePolicy;
import org.apereo.cas.oidc.claims.OidcEmailScopeAttributeReleasePolicy;
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

    protected Map<String, BaseOidcScopeAttributeReleasePolicy> buildOidc(
            final OIDCRPClientAppTO rp,
            final DefaultAttrReleasePolicyConf conf) {

        Map<String, BaseOidcScopeAttributeReleasePolicy> policies = new HashMap<>();

        conf.getReleaseAttrs().forEach((internal, external) -> {
            if (OidcProfileScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                if (rp.getScopes().contains(OIDCScopeConstants.PROFILE)) {
                    policies.computeIfAbsent(
                            OIDCScopeConstants.PROFILE,
                            k -> new OidcProfileScopeAttributeReleasePolicy()).
                            getClaimMappings().put(external.toString(), internal);
                } else {
                    warnMissingScope(rp.getName(), internal, external, OIDCScopeConstants.PROFILE);
                }
            } else if (OidcEmailScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                if (rp.getScopes().contains(OIDCScopeConstants.EMAIL)) {
                    policies.computeIfAbsent(
                            OIDCScopeConstants.EMAIL,
                            k -> new OidcEmailScopeAttributeReleasePolicy()).
                            getClaimMappings().put(external.toString(), internal);
                } else {
                    warnMissingScope(rp.getName(), internal, external, OIDCScopeConstants.EMAIL);
                }
            } else if (OidcAddressScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                if (rp.getScopes().contains(OIDCScopeConstants.ADDRESS)) {
                    policies.computeIfAbsent(
                            OIDCScopeConstants.ADDRESS,
                            k -> new OidcAddressScopeAttributeReleasePolicy()).
                            getClaimMappings().put(external.toString(), internal);
                } else {
                    warnMissingScope(rp.getName(), internal, external, OIDCScopeConstants.ADDRESS);
                }
            } else if (OidcPhoneScopeAttributeReleasePolicy.ALLOWED_CLAIMS.contains(external.toString())) {
                if (rp.getScopes().contains(OIDCScopeConstants.PHONE)) {
                    policies.computeIfAbsent(
                            OIDCScopeConstants.PHONE,
                            k -> new OidcPhoneScopeAttributeReleasePolicy()).
                            getClaimMappings().put(external.toString(), internal);
                } else {
                    warnMissingScope(rp.getName(), internal, external, OIDCScopeConstants.PHONE);
                }
            } else {
                BaseOidcScopeAttributeReleasePolicy custom = policies.computeIfAbsent(
                        OIDCScopeConstants.SYNCOPE,
                        k -> new OidcCustomScopeAttributeReleasePolicy(
                                OIDCScopeConstants.SYNCOPE, new ArrayList<>()));

                custom.getAllowedAttributes().add(external.toString());
                custom.getClaimMappings().put(external.toString(), internal);
            }
        });

        return policies;
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

    @Override
    public RegisteredServiceAttributeReleasePolicy build(final ClientAppTO app, final AttrReleasePolicyTO policy) {
        DefaultAttrReleasePolicyConf conf = (DefaultAttrReleasePolicyConf) policy.getConf();

        Map<String, BaseOidcScopeAttributeReleasePolicy> oidc = null;
        ReturnMappedAttributeReleasePolicy returnMapped = null;
        if (!conf.getReleaseAttrs().isEmpty()) {
            if (app instanceof OIDCRPClientAppTO rp) {
                oidc = buildOidc(rp, conf);
            } else {
                returnMapped = new ReturnMappedAttributeReleasePolicy();
                returnMapped.setAllowedAttributes(conf.getReleaseAttrs());
            }
        }

        ReturnAllowedAttributeReleasePolicy returnAllowed = null;
        if (!conf.getAllowedAttrs().isEmpty()) {
            returnAllowed = new ReturnAllowedAttributeReleasePolicy();
            returnAllowed.setAllowedAttributes(conf.getAllowedAttrs());
        }

        ChainingAttributeReleasePolicy chain = new ChainingAttributeReleasePolicy();
        AbstractRegisteredServiceAttributeReleasePolicy single = null;
        if (oidc == null) {
            if (returnMapped == null && returnAllowed == null) {
                single = new DenyAllAttributeReleasePolicy();
            } else if (returnMapped != null && returnAllowed == null) {
                single = returnMapped;
            } else if (returnMapped == null && returnAllowed != null) {
                single = returnAllowed;
            } else {
                chain.addPolicies(returnMapped, returnAllowed);
            }
        } else {
            if (oidc.size() == 1) {
                single = oidc.values().iterator().next();
            } else {
                // if present, add the custom scope at the end of the chain
                oidc.entrySet().stream().
                        filter(entry -> !OIDCScopeConstants.SYNCOPE.equals(entry.getKey())).
                        forEach(entry -> chain.addPolicies(entry.getValue()));
                Optional.ofNullable(oidc.get(OIDCScopeConstants.SYNCOPE)).ifPresent(chain::addPolicies);
            }
        }

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
}
