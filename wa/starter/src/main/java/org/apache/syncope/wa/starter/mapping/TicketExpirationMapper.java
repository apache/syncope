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

import org.apache.syncope.common.lib.policy.TicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apereo.cas.services.RegisteredServiceProxyGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceProxyTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceServiceTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTicketGrantingTicketExpirationPolicy;

public interface TicketExpirationMapper {

    boolean supports(TicketExpirationPolicyConf conf);

    RegisteredServiceTicketGrantingTicketExpirationPolicy buildTGT(TicketExpirationPolicyTO policy);

    RegisteredServiceServiceTicketExpirationPolicy buildST(TicketExpirationPolicyTO policy);

    RegisteredServiceProxyGrantingTicketExpirationPolicy buildProxyTGT(TicketExpirationPolicyTO policy);

    RegisteredServiceProxyTicketExpirationPolicy buildProxyST(TicketExpirationPolicyTO policy);
}
