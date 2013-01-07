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
        SyncPolicyTO policyTO = policyService.read(1L, SyncPolicyTO.class);

        assertNotNull(policyTO);
    }

    @Test
    public void getGlobalPasswordPolicy() {
        PasswordPolicyTO policyTO = policyService.readGlobal(PolicyType.PASSWORD, PasswordPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_PASSWORD, policyTO.getType());
        assertEquals(8, ((PasswordPolicySpec) policyTO.getSpecification()).getMinLength());
    }

    @Test
    public void getGlobalAccountPolicy() {
        AccountPolicyTO policyTO = policyService.readGlobal(PolicyType.ACCOUNT, AccountPolicyTO.class);

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
            policyService.create(policy);
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
            policyService.create(policy);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(SyncopeClientExceptionType.InvalidSyncPolicy);
        }
        assertNotNull(t);
    }

    @Test
    public void create() {
        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());
        policy.setDescription("Sync policy");

        SyncPolicyTO policyTO = policyService.create(policy);

        assertNotNull(policyTO);
        assertEquals(PolicyType.SYNC, policyTO.getType());
    }

    @Test
    public void update() {
        // get global password
        PasswordPolicyTO globalPolicy = policyService.read(2L, PasswordPolicyTO.class);

        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setDescription("A simple password policy");
        policy.setSpecification(globalPolicy.getSpecification());

        // create a new password policy using global password as a template
        policy = policyService.create(policy);

        // read new password policy
        policy = policyService.read(policy.getId(), PasswordPolicyTO.class);

        assertNotNull("find to update did not work", policy);

        PasswordPolicySpec policySpec = ((PasswordPolicyTO) policy).getSpecification();
        policySpec.setMaxLength(22);
        policy.setSpecification(policySpec);

        // update new password policy
        policy = policyService.update(policy.getId(), policy);

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals(22, ((PasswordPolicyTO) policy).getSpecification().getMaxLength());
        assertEquals(8, ((PasswordPolicyTO) policy).getSpecification().getMinLength());
    }

    @Test
    public void delete() {
        final SyncPolicyTO policyTO = policyService.read(7L, SyncPolicyTO.class);

        assertNotNull("find to delete did not work", policyTO);

        PolicyTO policyToDelete =
                policyService.delete(7L, SyncPolicyTO.class);
        assertNotNull(policyToDelete);

        Throwable t = null;
        try {
        	policyService.read(7L, SyncPolicyTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
        }

        assertNotNull(t);
    }
    
}
