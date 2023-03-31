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

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;

public class DefaultAccessMapper implements AccessMapper {

    @Override
    public boolean supports(final AccessPolicyConf conf) {
        return DefaultAccessPolicyConf.class.equals(conf.getClass());
    }

    @Override
    public RegisteredServiceAccessStrategy build(final AccessPolicyTO policy) {
        DefaultAccessPolicyConf conf = (DefaultAccessPolicyConf) policy.getConf();

        DefaultRegisteredServiceAccessStrategy accessStrategy =
                new DefaultRegisteredServiceAccessStrategy(conf.isEnabled(), conf.isSsoEnabled());

        accessStrategy.setOrder(conf.getOrder());

        accessStrategy.setRequireAllAttributes(conf.isRequireAllAttributes());

        accessStrategy.setCaseInsensitive(conf.isCaseInsensitive());

        accessStrategy.setUnauthorizedRedirectUrl(conf.getUnauthorizedRedirectUrl());

        conf.getRequiredAttrs().forEach(
                (k, v) -> accessStrategy.getRequiredAttributes().put(k,
                        Stream.of(StringUtils.split(v, ",")).map(String::trim).collect(Collectors.toSet())));

        conf.getRejectedAttrs().forEach(
                (k, v) -> accessStrategy.getRejectedAttributes().put(k,
                        Stream.of(StringUtils.split(v, ",")).map(String::trim).collect(Collectors.toSet())));

        return accessStrategy;
    }
}
