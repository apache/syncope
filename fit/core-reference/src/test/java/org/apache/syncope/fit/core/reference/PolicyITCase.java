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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AccountPolicyTO;
import org.apache.syncope.common.lib.to.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.SyncPolicyTO;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PolicyITCase extends AbstractITCase {

    private SyncPolicyTO buildSyncPolicyTO() {
        SyncPolicyTO policy = new SyncPolicyTO();

        SyncPolicySpec spec = new SyncPolicySpec();
        spec.setUserJavaRule(TestSyncRule.class.getName());

        policy.setSpecification(spec);
        policy.setDescription("Sync policy");

        return policy;
    }

    @Test
    public void listByType() {
        List<SyncPolicyTO> policyTOs = policyService.list(PolicyType.SYNC);

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public void getAccountPolicy() {
        AccountPolicyTO policyTO = policyService.read(6L);

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().isEmpty());
        assertTrue(policyTO.getUsedByRealms().contains("/odd"));
    }

    @Test
    public void getPasswordPolicy() {
        PasswordPolicyTO policyTO = policyService.read(4L);

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_NOPROPAGATION));
        assertTrue(policyTO.getUsedByRealms().containsAll(Arrays.asList("/", "/odd", "/even")));
    }

    @Test
    public void getSyncPolicy() {
        SyncPolicyTO policyTO = policyService.read(1L);

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void createMissingDescription() {
        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());

        try {
            createPolicy(policy);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPolicy, e.getType());
        }
    }

    @Test
    public void create() {
        SyncPolicyTO policy = buildSyncPolicyTO();

        SyncPolicyTO policyTO = createPolicy(policy);

        assertNotNull(policyTO);
        assertEquals(PolicyType.SYNC, policyTO.getType());
        assertEquals(TestSyncRule.class.getName(), policyTO.getSpecification().getUserJavaRule());
    }

    @Test
    public void update() {
        // get global password
        PasswordPolicyTO globalPolicy = policyService.read(2L);

        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setDescription("A simple password policy");
        policy.setSpecification(globalPolicy.getSpecification());

        // create a new password policy using global password as a template
        policy = createPolicy(policy);

        // read new password policy
        policy = policyService.read(policy.getKey());

        assertNotNull("find to update did not work", policy);

        PasswordPolicySpec policySpec = policy.getSpecification();
        policySpec.setMaxLength(22);
        policy.setSpecification(policySpec);

        // update new password policy
        policyService.update(policy.getKey(), policy);
        policy = policyService.read(policy.getKey());

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals(22, policy.getSpecification().getMaxLength());
        assertEquals(8, policy.getSpecification().getMinLength());
    }

    @Test
    public void delete() {
        SyncPolicyTO policy = buildSyncPolicyTO();

        SyncPolicyTO policyTO = createPolicy(policy);
        assertNotNull(policyTO);

        policyService.delete(policyTO.getKey());

        try {
            policyService.read(policyTO.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getCorrelationRules() {
        assertEquals(1, syncopeService.info().getSyncCorrelationRules().size());
    }

    @Test
    public void issueSYNCOPE553() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setDescription("SYNCOPE553");

        final AccountPolicySpec accountPolicySpec = new AccountPolicySpec();
        accountPolicySpec.setMinLength(3);
        accountPolicySpec.setMaxLength(8);
        policy.setSpecification(accountPolicySpec);

        policy = createPolicy(policy);
        assertNotNull(policy);
    }
}
