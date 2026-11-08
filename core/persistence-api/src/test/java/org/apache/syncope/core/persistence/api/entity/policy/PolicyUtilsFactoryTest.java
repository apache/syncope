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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.junit.jupiter.api.Test;

class PolicyUtilsFactoryTest {

    private final PolicyUtilsFactory factory = new PolicyUtilsFactory();

    @Test
    void getInstanceByType() {
        assertEquals(PolicyType.PROPAGATION, factory.getInstance(PolicyType.PROPAGATION).getType());
    }

    @Test
    void getInstanceByPolicy() {
        final AccountPolicy accountPolicy = mock(AccountPolicy.class);
        assertEquals(PolicyType.ACCOUNT, factory.getInstance(accountPolicy).getType());
    }

    @Test
    void getInstanceByInvalidPolicy() {
        assertThrows(IllegalArgumentException.class, () -> factory.getInstance(mock(Policy.class)));
    }

    @Test
    void getInstanceByPolicyTOClass() {
        assertEquals(PolicyType.PASSWORD, factory.getInstance(PasswordPolicyTO.class).getType());
    }

    @Test
    void getInstanceByInvalidPolicyTOClass() {
        assertThrows(IllegalArgumentException.class, () -> factory.getInstance(PolicyTO.class));
    }

    @Test
    void getInstanceByPolicyTO() {
        final AccountPolicyTO accountPolicyTO = mock(AccountPolicyTO.class);
        assertEquals(PolicyType.ACCOUNT, factory.getInstance(accountPolicyTO).getType());
    }
}
