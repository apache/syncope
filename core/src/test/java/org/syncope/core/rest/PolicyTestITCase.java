/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest;

import java.util.Arrays;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;
import org.syncope.client.mod.PasswordPolicyMod;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.PasswordPolicySpec;
import org.syncope.types.PolicyType;
import org.syncope.types.SyncPolicySpec;

public class PolicyTestITCase extends AbstractTest {

    @Test
    public final void listByType() {
        List<SyncPolicyTO> policyTOs = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "policy/{kind}/list",
                SyncPolicyTO[].class, PolicyType.SYNC.toString()));

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public final void read() {
        SyncPolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 1L);

        assertNotNull(policyTO);
    }

    @Test
    public final void getGlobalPasswordPolicy() {
        PasswordPolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/password/global/read", PasswordPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_PASSWORD, policyTO.getType());
        assertEquals(8,
                ((PasswordPolicySpec) policyTO.getSpecification()).getMinLength());
    }

    @Test
    public final void getGlobalAccountPolicy() {
        AccountPolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/account/global/read", AccountPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.GLOBAL_ACCOUNT, policyTO.getType());
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithException() {

        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setSpecification(new PasswordPolicySpec());
        policy.setType(PolicyType.GLOBAL_PASSWORD);
        policy.setDescription("global password policy");

        restTemplate.postForObject(
                BASE_URL + "policy/password/create",
                policy, PasswordPolicyTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createMissingDescription() {

        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());
        policy.setType(PolicyType.SYNC);

        restTemplate.postForObject(
                BASE_URL + "policy/sync/create",
                policy, PasswordPolicyTO.class);
    }

    @Test
    public final void create() {
        SyncPolicyTO policy = new SyncPolicyTO();
        policy.setSpecification(new SyncPolicySpec());
        policy.setType(PolicyType.SYNC);
        policy.setDescription("Sync policy");

        SyncPolicyTO policyTO = restTemplate.postForObject(
                BASE_URL + "policy/sync/create", policy, SyncPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.SYNC, policyTO.getType());
    }

    @Test
    public final void update() {
        // get global password
        PasswordPolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/read/{id}", PasswordPolicyTO.class, 2L);

        policyTO.setType(PolicyType.PASSWORD);
        policyTO.setId(0);

        // create a new password policy using global password as a template
        policyTO = restTemplate.postForObject(
                BASE_URL + "policy/password/create",
                policyTO, PasswordPolicyTO.class);

        // read new password policy
        policyTO = restTemplate.getForObject(
                BASE_URL + "policy/read/{id}",
                PasswordPolicyTO.class, policyTO.getId());

        assertNotNull("find to update did not work", policyTO);

        PasswordPolicySpec policy =
                ((PasswordPolicyTO) policyTO).getSpecification();
        policy.setMaxLength(22);

        PasswordPolicyMod policyMod = new PasswordPolicyMod();
        policyMod.setId(policyTO.getId());
        policyMod.setType(PolicyType.PASSWORD);
        policyMod.setSpecification(policy);
        policyMod.setDescription(policyTO.getDescription());

        // update new password policy
        policyTO = restTemplate.postForObject(
                BASE_URL + "policy/password/update",
                policyMod, PasswordPolicyTO.class);

        assertNotNull(policyTO);
        assertEquals(PolicyType.PASSWORD, policyTO.getType());
        assertEquals(22,
                ((PasswordPolicyTO) policyTO).getSpecification().getMaxLength());
        assertEquals(8,
                ((PasswordPolicyTO) policyTO).getSpecification().getMinLength());
    }

    @Test
    public final void delete() {
        final PolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 7L);

        assertNotNull("find to delete did not work", policyTO);

        restTemplate.delete(BASE_URL + "policy/delete/{id}", 7L);

        Throwable t = null;
        try {
            restTemplate.getForObject(
                    BASE_URL + "policy/read/{id}", SyncPolicyTO.class, 7L);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
        }

        assertNotNull(t);
    }
}
