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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.PolicyType;
import org.syncope.types.PasswordPolicySpec;
import org.syncope.types.SyncPolicySpec;

@Transactional
public class PolicyTest extends AbstractTest {

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public final void findAll() {
        List<Policy> policies = policyDAO.findAll();
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public final void findById() {
        Policy policy = policyDAO.find(1L);
        assertNotNull("findById did not work", policy);
    }

    @Test
    public final void findByType() {
        List<? extends Policy> policies = policyDAO.find(PolicyType.SYNC);
        assertNotNull("findById did not work", policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public final void findGlobalPasswordPolicy() {
        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        assertNotNull("findById did not work", policy);

        assertEquals(PolicyType.GLOBAL_PASSWORD, policy.getType());

        assertEquals("invalid policy values",
                8, ((PasswordPolicySpec) policy.getSpecification()).getMinLength());
    }

    @Test
    @ExpectedException(value = InvalidEntityException.class)
    public final void saveInvalidPolicy() {

        PasswordPolicySpec passwordPolicy = new PasswordPolicySpec();
        passwordPolicy.setMaxLength(8);
        passwordPolicy.setMinLength(6);

        SyncPolicy policy = new SyncPolicy();
        policy.setSpecification(passwordPolicy);
        policy.setDescription("sync policy");

        policyDAO.save(policy);
    }

    @Test
    @ExpectedException(value = InvalidEntityException.class)
    public final void saveSecondPasswordPolicy() {
        PasswordPolicySpec passwordPolicy = new PasswordPolicySpec();
        passwordPolicy.setMaxLength(8);
        passwordPolicy.setMinLength(6);

        PasswordPolicy policy = new PasswordPolicy(true);
        policy.setSpecification(passwordPolicy);
        policy.setDescription("global password policy");

        policyDAO.save(policy);
    }

    @Test
    public final void create() {
        SyncPolicy policy = new SyncPolicy();
        policy.setSpecification(new SyncPolicySpec());
        policy.setDescription("Sync policy");

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(PolicyType.SYNC, policy.getType());
    }

    @Test
    public final void update() {
        PasswordPolicySpec specification = new PasswordPolicySpec();
        specification.setMaxLength(8);
        specification.setMinLength(6);

        Policy policy = policyDAO.getGlobalPasswordPolicy();
        assertNotNull(policy);
        policy.setSpecification(specification);

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(PolicyType.GLOBAL_PASSWORD, policy.getType());
        assertEquals(
                ((PasswordPolicySpec) policy.getSpecification()).getMaxLength(),
                8);
        assertEquals(
                ((PasswordPolicySpec) policy.getSpecification()).getMinLength(),
                6);
    }

    @Test
    public final void delete() {
        Policy policy = policyDAO.find(1L);
        assertNotNull("find to delete did not work", policy);

        policyDAO.delete(policy.getId());

        Policy actual = policyDAO.find(1L);
        assertNull("delete did not work", actual);
    }
}
