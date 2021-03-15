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

import org.apache.syncope.common.lib.policy.AllowedAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyConf;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceConsentPolicy;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy;
import org.apereo.cas.util.model.TriStateBoolean;

@AttrReleaseMapFor(attrReleasePolicyConfClass = AllowedAttrReleasePolicyConf.class)
public class AllowedAttrReleaseMapper implements AttrReleaseMapper {

    @Override
    public RegisteredServiceAttributeReleasePolicy build(final AttrReleasePolicyConf conf) {
        AllowedAttrReleasePolicyConf aarpc = (AllowedAttrReleasePolicyConf) conf;

        RegisteredServiceAttributeReleasePolicy attributeReleasePolicy;
        if (aarpc.getAllowedAttrs().isEmpty()) {
            attributeReleasePolicy = new DenyAllAttributeReleasePolicy();
        } else {
            attributeReleasePolicy = new ReturnAllowedAttributeReleasePolicy();
            ((ReturnAllowedAttributeReleasePolicy) attributeReleasePolicy).
                    setAllowedAttributes((aarpc.getAllowedAttrs()));

            if (aarpc.getConsentPolicy() != null) {
                RegisteredServiceConsentPolicy consentPolicy =
                        new DefaultRegisteredServiceConsentPolicy(aarpc.getConsentPolicy().getExcludedAttrs(),
                                aarpc.getConsentPolicy().getIncludeOnlyAttrs());
                ((DefaultRegisteredServiceConsentPolicy) consentPolicy).setStatus(
                        aarpc.getConsentPolicy().getStatus() == null ? TriStateBoolean.UNDEFINED
                        : TriStateBoolean.fromBoolean(aarpc.getConsentPolicy().getStatus()));
                ((ReturnAllowedAttributeReleasePolicy) attributeReleasePolicy).setConsentPolicy(consentPolicy);
            }
        }

        return attributeReleasePolicy;
    }
}
