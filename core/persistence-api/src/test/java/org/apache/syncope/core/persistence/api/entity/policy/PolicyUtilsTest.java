/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.entity.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.syncope.common.lib.types.PolicyType;
import org.junit.jupiter.api.Test;

class PolicyUtilsTest {

    @Test
    void policyClassForAccount() {
        assertEquals(AccountPolicy.class, new PolicyUtils(PolicyType.ACCOUNT).policyClass());
    }

    @Test
    void policyClassForPassword() {
        assertEquals(PasswordPolicy.class, new PolicyUtils(PolicyType.PASSWORD).policyClass());
    }

    @Test
    void policyClassForAuth() {
        assertEquals(AuthPolicy.class, new PolicyUtils(PolicyType.AUTH).policyClass());
    }

    @Test
    void policyClassForAttrRelease() {
        assertEquals(AttrReleasePolicy.class, new PolicyUtils(PolicyType.ATTR_RELEASE).policyClass());
    }

    @Test
    void policyClassForAccess() {
        assertEquals(AccessPolicy.class, new PolicyUtils(PolicyType.ACCESS).policyClass());
    }

    @Test
    void policyClassForTicketExpiration() {
        assertEquals(TicketExpirationPolicy.class, new PolicyUtils(PolicyType.TICKET_EXPIRATION).policyClass());
    }

    @Test
    void policyClassForPropagation() {
        assertEquals(PropagationPolicy.class, new PolicyUtils(PolicyType.PROPAGATION).policyClass());
    }

    @Test
    void policyClassForInbound() {
        assertEquals(InboundPolicy.class, new PolicyUtils(PolicyType.INBOUND).policyClass());
    }

    @Test
    void policyClassForPush() {
        assertEquals(PushPolicy.class, new PolicyUtils(PolicyType.PUSH).policyClass());
    }
}
