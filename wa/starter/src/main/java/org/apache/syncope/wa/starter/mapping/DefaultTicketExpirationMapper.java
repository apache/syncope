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
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apereo.cas.services.DefaultRegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.DefaultRegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.DefaultRegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.DefaultRegisteredServiceTicketGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTicketGrantingTicketExpirationPolicy;

public class DefaultTicketExpirationMapper implements TicketExpirationMapper {

    @Override
    public boolean supports(final TicketExpirationPolicyConf conf) {
        return DefaultTicketExpirationPolicyConf.class.equals(conf.getClass());
    }

    protected Optional<DefaultTicketExpirationPolicyConf> conf(final TicketExpirationPolicyTO policy) {
        if (policy.getConf() instanceof final DefaultTicketExpirationPolicyConf defaultTicketExpirationPolicyConf) {
            return Optional.of(defaultTicketExpirationPolicyConf);
        }
        return Optional.empty();
    }

    @Override
    public RegisteredServiceTicketGrantingTicketExpirationPolicy buildTGT(final TicketExpirationPolicyTO policy) {
        return conf(policy).flatMap(conf -> Optional.ofNullable(conf.getTgtConf())).
                map(conf -> {
                    DefaultRegisteredServiceTicketGrantingTicketExpirationPolicy result =
                            new DefaultRegisteredServiceTicketGrantingTicketExpirationPolicy();
                    result.setMaxTimeToLiveInSeconds(conf.getMaxTimeToLiveInSeconds());
                    return result;
                }).
                orElse(null);
    }

    @Override
    public RegisteredServiceServiceTicketExpirationPolicy buildST(final TicketExpirationPolicyTO policy) {
        return conf(policy).flatMap(conf -> Optional.ofNullable(conf.getStConf())).
                map(conf -> {
                    DefaultRegisteredServiceServiceTicketExpirationPolicy result =
                            new DefaultRegisteredServiceServiceTicketExpirationPolicy();
                    result.setNumberOfUses(conf.getNumberOfUses());
                    result.setTimeToLive(Long.toString(conf.getMaxTimeToLiveInSeconds()));
                    return result;
                }).
                orElse(null);
    }

    @Override
    public RegisteredServiceProxyGrantingTicketExpirationPolicy buildProxyTGT(final TicketExpirationPolicyTO policy) {
        return conf(policy).flatMap(conf -> Optional.ofNullable(conf.getProxyTgtConf())).
                map(conf -> {
                    DefaultRegisteredServiceProxyGrantingTicketExpirationPolicy result =
                            new DefaultRegisteredServiceProxyGrantingTicketExpirationPolicy();
                    result.setMaxTimeToLiveInSeconds(conf.getMaxTimeToLiveInSeconds());
                    return result;
                }).
                orElse(null);
    }

    @Override
    public RegisteredServiceProxyTicketExpirationPolicy buildProxyST(final TicketExpirationPolicyTO policy) {
        return conf(policy).flatMap(conf -> Optional.ofNullable(conf.getProxyStConf())).
                map(conf -> {
                    DefaultRegisteredServiceProxyTicketExpirationPolicy result =
                            new DefaultRegisteredServiceProxyTicketExpirationPolicy();
                    result.setNumberOfUses(conf.getNumberOfUses());
                    result.setTimeToLive(Long.toString(conf.getMaxTimeToLiveInSeconds()));
                    return result;
                }).
                orElse(null);
    }
}
