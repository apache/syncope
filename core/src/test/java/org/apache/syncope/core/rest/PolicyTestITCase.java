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

import org.apache.syncope.client.to.AccountPolicyTO;
import org.apache.syncope.client.to.PasswordPolicyTO;
import org.apache.syncope.client.to.PolicyTO;
import org.apache.syncope.client.to.SyncPolicyTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.types.PasswordPolicySpec;
import org.apache.syncope.types.PolicyType;
import org.apache.syncope.types.SyncPolicySpec;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PolicyTestITCase extends AbstractTest {

    @Test
    public void listByType() {
        List<SyncPolicyTO> policyTOs = policyService.listByType(PolicyType.SYNC);

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public void read() {
        SyncPolicyTO policyTO = policyService.read(PolicyType.SYNC, 1L);

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
        System.out.println(policy.getType());

        Throwable t = null;
        try {
            policyService.create(PolicyType.PASSWORD, policy);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(SyncopeClientExceptionType.InvalidPasswordPolicy);
        }
        assertNotNull(t);
    }

    @Test
    public void createMissingDescription() {
        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());

        Throwable t = null;
        try {
            policyService.create(PolicyType.SYNC, policy);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(SyncopeClientExceptionType.InvalidSyncPolicy);
        }
        assertNotNull(t);
    }

    @Test
    public void create() {
        SyncPolicyTO policy = buildSyncPolicyTO();

        SyncPolicyTO policyTO = policyService.create(PolicyType.SYNC, policy);

        assertNotNull(policyTO);
        assertEquals(PolicyType.SYNC, policyTO.getType());
    }

    @Test
    public void update() {
        // get global password
        PasswordPolicyTO globalPolicy = policyService.read(PolicyType.PASSWORD, 2L);

        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setDescription("A simple password policy");
        policy.setSpecification(globalPolicy.getSpecification());

        // create a new password policy using global password as a template
        policy = policyService.create(PolicyType.PASSWORD, policy);

        // read new password policy
        policy = policyService.read(PolicyType.PASSWORD, policy.getId());

        assertNotNull("find to update did not work", policy);

        PasswordPolicySpec policySpec = policy.getSpecification();
        policySpec.setMaxLength(22);
        policy.setSpecification(policySpec);

        // update new password policy
        policy = policyService.update(PolicyType.PASSWORD, policy.getId(), policy);

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals(22, policy.getSpecification().getMaxLength());
        assertEquals(8, policy.getSpecification().getMinLength());
    }

    @Test
    public void delete() {
        SyncPolicyTO policy = buildSyncPolicyTO();
        SyncPolicyTO policyTO = policyService.create(PolicyType.SYNC, policy);
        assertNotNull(policyTO);

        PolicyTO policyToDelete =
                policyService.delete(PolicyType.SYNC, policyTO.getId());
        assertNotNull(policyToDelete);

        Throwable t = null;
        try {
        	policyService.read(PolicyType.SYNC, policyTO.getId());
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
        }

        assertNotNull(t);
    }

	private SyncPolicyTO buildSyncPolicyTO() {
		SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());
        policy.setDescription("Sync policy");
		return policy;
	}
}
