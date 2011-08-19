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

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.PolicyType;
import org.syncope.types.SyntaxPolicy;

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

        assertEquals("invalid policy values",
                8, ((SyntaxPolicy) policy.getSpecification()).getMaxLength());
    }

    @Test
    public final void findPasswordPolicy() {
        Policy policy = policyDAO.getPasswordPolicy();
        assertNotNull("findById did not work", policy);

        assertEquals(PolicyType.PASSWORD, policy.getType());

        assertEquals("invalid policy values",
                8, ((SyntaxPolicy) policy.getSpecification()).getMinLength());
    }

    @Test
    public final void save() {

        SyntaxPolicy syntaxPolicy = new SyntaxPolicy();
        syntaxPolicy.setMaxLength(8);
        syntaxPolicy.setMinLength(6);

        Policy policy = new Policy();
        policy.setSpecification(syntaxPolicy);
        policy.setType(PolicyType.PASSWORD);

        Throwable t = null;
        try {
            policy = policyDAO.save(policy);
        } catch (InvalidEntityException e) {
            t = e;
        }
        assertNotNull(t);

        Policy passwordPolicy = policyDAO.getPasswordPolicy();
        assertNotNull(passwordPolicy);
        passwordPolicy.setSpecification(policy.getSpecification());


        policy = policyDAO.save(passwordPolicy);

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals(
                ((SyntaxPolicy) policy.getSpecification()).getMaxLength(), 8);
        assertEquals(
                ((SyntaxPolicy) policy.getSpecification()).getMinLength(), 6);
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
