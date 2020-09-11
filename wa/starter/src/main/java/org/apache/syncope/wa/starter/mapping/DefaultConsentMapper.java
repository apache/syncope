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

import org.apache.syncope.common.lib.policy.DefaultConsentPolicyConf;
import org.apereo.cas.services.RegisteredServiceConsentPolicy;
import org.apereo.cas.services.consent.DefaultRegisteredServiceConsentPolicy;
import org.apereo.cas.util.model.TriStateBoolean;
import org.springframework.stereotype.Component;
import org.apache.syncope.common.lib.policy.ConsentPolicyConf;

@ConsentMapFor(consentPolicyConfClass = DefaultConsentPolicyConf.class)
@Component
public class DefaultConsentMapper implements ConsentMapper {

    @Override
    public RegisteredServiceConsentPolicy build(final ConsentPolicyConf conf) {
        RegisteredServiceConsentPolicy consentPolicy =
                new DefaultRegisteredServiceConsentPolicy(conf.getExcludedAttrs(), conf.getIncludeOnlyAttrs());
        ((DefaultRegisteredServiceConsentPolicy) consentPolicy).setStatus(conf.getStatus() == null
                ? TriStateBoolean.UNDEFINED : TriStateBoolean.fromBoolean(conf.getStatus()));
        
        return consentPolicy;
    }
}
