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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.core.sync.TestSyncRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PolicyTestITCase extends AbstractTest {

    @Test
    public void listByType() {
        List<SyncPolicyTO> policyTOs = policyService.list(PolicyType.SYNC);

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public void read() {
        SyncPolicyTO policyTO = policyService.read(1L);

        assertNotNull(policyTO);
    }

    @Test
    public void getGlobalPasswordPolicy() {
        PasswordPolicyTO policyTO = policyService.readGlobal(PolicyType.PASSWORD);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_PASSWORD, policyTO.getType());
        assertEquals(8, policyTO.getSpecification().getMinLength());
    }

    @Test
    public void getGlobalAccountPolicy() {
        AccountPolicyTO policyTO = policyService.readGlobal(PolicyType.ACCOUNT);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_ACCOUNT, policyTO.getType());
    }

    @Test
    public void createWithException() {
        PasswordPolicyTO policy = new PasswordPolicyTO(true);
        policy.setSpecification(new PasswordPolicySpec());
        policy.setDescription("global password policy");

        try {
            createPolicy(policy);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            assertNotNull(sccee.getException(SyncopeClientExceptionType.InvalidPasswordPolicy));
        }
    }

    @Test
    public void createMissingDescription() {
        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());

        try {
            createPolicy(policy);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            assertNotNull(sccee.getException(SyncopeClientExceptionType.InvalidSyncPolicy));
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
        policy = policyService.read(policy.getId());

        assertNotNull("find to update did not work", policy);

        PasswordPolicySpec policySpec = policy.getSpecification();
        policySpec.setMaxLength(22);
        policy.setSpecification(policySpec);

        // update new password policy
        policyService.update(policy.getId(), policy);
        policy = policyService.read(policy.getId());

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

        policyService.delete(policyTO.getId());

        Throwable t = null;
        try {
            policyService.read(policyTO.getId());
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
        }

        assertNotNull(t);
    }

    @Test
    public void getCorrelationRules() {
        assertEquals(1, policyService.getSyncCorrelationRuleClasses().size());
    }

    private SyncPolicyTO buildSyncPolicyTO() {
        SyncPolicyTO policy = new SyncPolicyTO();

        SyncPolicySpec spec = new SyncPolicySpec();
        spec.setUserJavaRule(TestSyncRule.class.getName());

        policy.setSpecification(spec);
        policy.setDescription("Sync policy");

        return policy;
    }
}
