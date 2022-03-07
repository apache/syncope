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
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;

@AccessMapFor(accessPolicyConfClass = DefaultAccessPolicyConf.class)
public class DefaultAccessMapper implements AccessMapper {

    @Override
    public RegisteredServiceAccessStrategy build(final AccessPolicyTO policy) {
        DefaultRegisteredServiceAccessStrategy accessStrategy =
                new DefaultRegisteredServiceAccessStrategy(policy.isEnabled(), policy.isSsoEnabled());

        accessStrategy.setOrder(policy.getOrder());

        accessStrategy.setRequireAllAttributes(policy.isRequireAllAttributes());

        accessStrategy.setCaseInsensitive(policy.isCaseInsensitive());

        accessStrategy.setUnauthorizedRedirectUrl(policy.getUnauthorizedRedirectUrl());

        policy.getConf().getRequiredAttrs().forEach(
                attr -> accessStrategy.getRequiredAttributes().put(attr.getSchema(), new HashSet<>(attr.getValues())));

        policy.getConf().getRejectedAttrs().forEach(
                attr -> accessStrategy.getRejectedAttributes().put(attr.getSchema(), new HashSet<>(attr.getValues())));

        return accessStrategy;
    }
}
