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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.common.lib.types.SyncPolicySpecItem;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.SyncPolicy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PolicyTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void findAll() {
        List<Policy> policies = policyDAO.findAll();
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public void findById() {
        Policy policy = policyDAO.find(1L);
        assertNotNull("findById did not work", policy);
    }

    @Test
    public void findByType() {
        List<? extends Policy> policies = policyDAO.find(PolicyType.SYNC);
        assertNotNull("findById did not work", policies);
        assertFalse(policies.isEmpty());
    }

    @Test(expected = InvalidEntityException.class)
    public void saveInvalidPolicy() {
        PasswordPolicySpec passwordPolicy = new PasswordPolicySpec();
        passwordPolicy.setMaxLength(8);
        passwordPolicy.setMinLength(6);

        SyncPolicy policy = entityFactory.newEntity(SyncPolicy.class);
        policy.setSpecification(passwordPolicy);
        policy.setDescription("sync policy");

        policyDAO.save(policy);
    }

    @Test
    public void create() {
        SyncPolicy policy = entityFactory.newEntity(SyncPolicy.class);

        final String syncURuleName = "net.tirasa.sync.correlation.TirasaURule";
        final String syncGRuleName = "net.tirasa.sync.correlation.TirasaGRule";

        SyncPolicySpec syncPolicySpec = new SyncPolicySpec();

        SyncPolicySpecItem item = new SyncPolicySpecItem();
        item.setAnyTypeKey(anyTypeDAO.findUser().getKey());
        item.setJavaRule(syncURuleName);
        syncPolicySpec.getItems().add(item);

        item = new SyncPolicySpecItem();
        item.setAnyTypeKey(anyTypeDAO.findGroup().getKey());
        item.setJavaRule(syncGRuleName);
        syncPolicySpec.getItems().add(item);

        policy.setSpecification(syncPolicySpec);
        policy.setDescription("Sync policy");

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(PolicyType.SYNC, policy.getType());
        assertEquals(syncURuleName,
                (policy.getSpecification(SyncPolicySpec.class)).getItem(anyTypeDAO.findUser().getKey()).getJavaRule());
        assertEquals(syncGRuleName,
                (policy.getSpecification(SyncPolicySpec.class)).getItem(anyTypeDAO.findGroup().getKey()).getJavaRule());
    }

    @Test
    public void update() {
        PasswordPolicySpec specification = new PasswordPolicySpec();
        specification.setMaxLength(8);
        specification.setMinLength(6);

        Policy policy = policyDAO.find(2L);
        assertNotNull(policy);
        policy.setSpecification(specification);

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(PolicyType.PASSWORD, policy.getType());
        assertEquals((policy.getSpecification(PasswordPolicySpec.class)).getMaxLength(), 8);
        assertEquals((policy.getSpecification(PasswordPolicySpec.class)).getMinLength(), 6);
    }

    @Test
    public void delete() {
        Policy policy = policyDAO.find(1L);
        assertNotNull("find to delete did not work", policy);

        policyDAO.delete(policy);

        Policy actual = policyDAO.find(1L);
        assertNull("delete did not work", actual);
    }
}
