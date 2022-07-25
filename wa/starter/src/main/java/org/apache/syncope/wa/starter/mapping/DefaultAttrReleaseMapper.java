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
import java.util.Map;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apereo.cas.authentication.principal.DefaultPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.AbstractPrincipalAttributesRepository;
import org.apereo.cas.authentication.principal.cache.CachingPrincipalAttributesRepository;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesCoreProperties;
import org.apereo.cas.services.AbstractRegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.ReturnMappedAttributeReleasePolicy;
import org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy;
import org.apereo.cas.util.model.TriStateBoolean;

@AttrReleaseMapFor(attrReleasePolicyConfClass = DefaultAttrReleasePolicyConf.class)
public class DefaultAttrReleaseMapper implements AttrReleaseMapper {

    @Override
    public RegisteredServiceAttributeReleasePolicy build(
            final AttrReleasePolicyTO policy, final Map<String, Object> releaseAttrs) {

        DefaultAttrReleasePolicyConf conf = (DefaultAttrReleasePolicyConf) policy.getConf();

        ReturnMappedAttributeReleasePolicy returnMapped = null;
        if (!releaseAttrs.isEmpty()) {
            returnMapped = new ReturnMappedAttributeReleasePolicy();
            returnMapped.setAllowedAttributes(releaseAttrs);
        }

        ReturnAllowedAttributeReleasePolicy returnAllowed = null;
        if (!conf.getAllowedAttrs().isEmpty()) {
            returnAllowed = new ReturnAllowedAttributeReleasePolicy();
        }

        AbstractRegisteredServiceAttributeReleasePolicy attributeReleasePolicy;
        if (returnMapped == null && returnAllowed == null) {
            attributeReleasePolicy = new DenyAllAttributeReleasePolicy();
        } else if (returnMapped != null) {
            attributeReleasePolicy = returnMapped;
        } else {
            attributeReleasePolicy = returnAllowed;
        }

        DefaultRegisteredServiceConsentPolicy consentPolicy = new DefaultRegisteredServiceConsentPolicy(
                new HashSet<>(conf.getExcludedAttrs()), new HashSet<>(conf.getIncludeOnlyAttrs()));
        consentPolicy.setOrder(policy.getOrder());
        consentPolicy.setStatus(policy.getStatus() == null
                ? TriStateBoolean.UNDEFINED
                : TriStateBoolean.fromBoolean(policy.getStatus()));
        attributeReleasePolicy.setConsentPolicy(consentPolicy);

        if (conf.getPrincipalIdAttr() != null) {
            attributeReleasePolicy.setPrincipalIdAttribute(conf.getPrincipalIdAttr());
        }

        if (conf.getPrincipalAttrRepoConf() != null && !conf.getPrincipalAttrRepoConf().getAttrRepos().isEmpty()) {
            DefaultAttrReleasePolicyConf.PrincipalAttrRepoConf parc = conf.getPrincipalAttrRepoConf();

            AbstractPrincipalAttributesRepository par = parc.getExpiration() > 0
                    ? new CachingPrincipalAttributesRepository(parc.getTimeUnit().name(), parc.getExpiration())
                    : new DefaultPrincipalAttributesRepository();

            par.setMergingStrategy(
                    PrincipalAttributesCoreProperties.MergingStrategyTypes.valueOf(parc.getMergingStrategy().name()));
            par.setIgnoreResolvedAttributes(par.isIgnoreResolvedAttributes());
            par.setAttributeRepositoryIds(new HashSet<>(parc.getAttrRepos()));
            attributeReleasePolicy.setPrincipalAttributesRepository(par);
        }

        if (returnMapped != null && returnAllowed != null) {
            ChainingAttributeReleasePolicy chain = new ChainingAttributeReleasePolicy();
            chain.addPolicies(returnMapped, returnAllowed);
            return chain;
        }

        return attributeReleasePolicy;
    }
}
