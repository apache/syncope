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

import java.util.Optional;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.HttpRequestAccessPolicyConf;
import org.apereo.cas.services.HttpRequestRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;

public class HttpRequestAccessMapper implements AccessMapper {

    @Override
    public boolean supports(final AccessPolicyConf conf) {
        return HttpRequestAccessPolicyConf.class.equals(conf.getClass());
    }

    @Override
    public RegisteredServiceAccessStrategy build(final AccessPolicyTO policy) {
        HttpRequestAccessPolicyConf conf = (HttpRequestAccessPolicyConf) policy.getConf();

        HttpRequestRegisteredServiceAccessStrategy accessStrategy = new HttpRequestRegisteredServiceAccessStrategy();

        Optional.ofNullable(conf.getIpAddress()).ifPresent(accessStrategy::setIpAddress);
        Optional.ofNullable(conf.getUserAgent()).ifPresent(accessStrategy::setUserAgent);

        return accessStrategy;
    }
}
