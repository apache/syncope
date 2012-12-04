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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.apache.syncope.to.AccountPolicyTO;
import org.apache.syncope.to.PasswordPolicyTO;
import org.apache.syncope.to.PolicyTO;
import org.apache.syncope.to.SyncPolicyTO;
import org.apache.syncope.types.PasswordPolicySpec;
import org.apache.syncope.types.PolicyType;
import org.apache.syncope.types.SyncPolicySpec;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;

public class PolicyTestITCase extends AbstractTest {

    @Override
    public void setupService() {
    }

    @Test
    public void listByType() {
        List<SyncPolicyTO> policyTOs = Arrays.asList(restTemplate.getForObject(BASE_URL + "policy/{kind}/list",
                SyncPolicyTO[].class, PolicyType.SYNC.toString()));

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public void read() {
        SyncPolicyTO policyTO = restTemplate.getForObject(BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 1L);

        assertNotNull(policyTO);
    }

    @Test
    public void getGlobalPasswordPolicy() {
        PasswordPolicyTO policyTO = restTemplate.getForObject(BASE_URL + "policy/password/global/read",
                PasswordPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_PASSWORD, policyTO.getType());
        assertEquals(8, policyTO.getSpecification().getMinLength());
    }

    @Test
    public void getGlobalAccountPolicy() {
        AccountPolicyTO policyTO = restTemplate.getForObject(BASE_URL + "policy/account/global/read",
                AccountPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_ACCOUNT, policyTO.getType());
    }

    @Test
    public void createWithException() {
        PasswordPolicyTO policy = new PasswordPolicyTO(true);
        policy.setSpecification(new PasswordPolicySpec());
        policy.setDescription("global password policy");

        Throwable t = null;
        try {
            restTemplate.postForObject(BASE_URL + "policy/password/create", policy, PasswordPolicyTO.class);
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
            restTemplate.postForObject(BASE_URL + "policy/sync/create", policy, PasswordPolicyTO.class);
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

        SyncPolicyTO policyTO = restTemplate.postForObject(BASE_URL + "policy/sync/create", policy, SyncPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.SYNC, policyTO.getType());
    }

    @Test
    public void update() {
        // get global password
        PasswordPolicyTO globalPolicy = restTemplate.getForObject(BASE_URL + "policy/read/{id}",
                PasswordPolicyTO.class, 2L);

        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setDescription("A simple password policy");
        policy.setSpecification(globalPolicy.getSpecification());

        // create a new password policy using global password as a template
        policy = restTemplate.postForObject(BASE_URL + "policy/password/create", policy, PasswordPolicyTO.class);

        // read new password policy
        policy = restTemplate.getForObject(BASE_URL + "policy/read/{id}", PasswordPolicyTO.class, policy.getId());

        assertNotNull("find to update did not work", policy);

        PasswordPolicySpec policySpec = policy.getSpecification();
        policySpec.setMaxLength(22);
        policy.setSpecification(policySpec);

        // update new password policy
        policy = restTemplate.postForObject(BASE_URL + "policy/password/update", policy, PasswordPolicyTO.class);

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals(22, policy.getSpecification().getMaxLength());
        assertEquals(8, policy.getSpecification().getMinLength());
    }

    @Test
    public void delete() {
        final PolicyTO policyTO = restTemplate.getForObject(BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 7L);

        assertNotNull("find to delete did not work", policyTO);

        PolicyTO policyToDelete =
                restTemplate.getForObject(BASE_URL + "policy/delete/{id}", SyncPolicyTO.class, 7L);
        assertNotNull(policyToDelete);

        Throwable t = null;
        try {
            restTemplate.getForObject(BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 7L);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
        }

        assertNotNull(t);
    }
}
