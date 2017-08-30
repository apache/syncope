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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;

@Transactional("Master")
public class PolicyTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findAll() {
        List<Policy> policies = policyDAO.findAll();
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public void findByKey() {
        PullPolicy policy = policyDAO.find("880f8553-069b-4aed-9930-2cd53873f544");
        assertNotNull(policy);

        PullPolicySpec spec = policy.getSpecification();
        assertNotNull(spec);

        String rule = spec.getCorrelationRules().get(AnyTypeKind.USER.name());
        assertNotNull(rule);
        String[] plainSchemas = POJOHelper.deserialize(rule, String[].class);
        assertNotNull(plainSchemas);
        assertEquals(2, plainSchemas.length);
        assertTrue(ArrayUtils.contains(plainSchemas, "username"));
        assertTrue(ArrayUtils.contains(plainSchemas, "firstname"));
    }

    @Test
    public void findByType() {
        List<PullPolicy> policies = policyDAO.find(PullPolicy.class);
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public void create() {
        PullPolicy policy = entityFactory.newEntity(PullPolicy.class);

        final String pullURuleName = "net.tirasa.pull.correlation.TirasaURule";
        final String pullGRuleName = "net.tirasa.pull.correlation.TirasaGRule";

        PullPolicySpec pullPolicySpec = new PullPolicySpec();

        pullPolicySpec.getCorrelationRules().put(anyTypeDAO.findUser().getKey(), pullURuleName);
        pullPolicySpec.getCorrelationRules().put(anyTypeDAO.findGroup().getKey(), pullGRuleName);

        policy.setSpecification(pullPolicySpec);
        policy.setDescription("Pull policy");

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(pullURuleName,
                policy.getSpecification().getCorrelationRules().get(anyTypeDAO.findUser().getKey()));
        assertEquals(pullGRuleName,
                policy.getSpecification().getCorrelationRules().get(anyTypeDAO.findGroup().getKey()));
    }

    @Test
    public void update() {
        PasswordPolicy policy = policyDAO.find("ce93fcda-dc3a-4369-a7b0-a6108c261c85");
        assertNotNull(policy);
        assertEquals(1, policy.getRules().size());

        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setMaxLength(8);
        ruleConf.setMinLength(6);

        Implementation rule = entityFactory.newEntity(Implementation.class);
        rule.setKey("PasswordRule" + UUID.randomUUID().toString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(ImplementationType.PASSWORD_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        rule = implementationDAO.save(rule);

        policy.add(rule);

        policy = policyDAO.save(policy);

        assertNotNull(policy);

        rule = policy.getRules().get(1);

        DefaultPasswordRuleConf actual = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        assertEquals(actual.getMaxLength(), 8);
        assertEquals(actual.getMinLength(), 6);
    }

    @Test
    public void delete() {
        Policy policy = policyDAO.find("66691e96-285f-4464-bc19-e68384ea4c85");
        assertNotNull(policy);

        policyDAO.delete(policy);

        Policy actual = policyDAO.find("66691e96-285f-4464-bc19-e68384ea4c85");
        assertNull(actual);
    }
}
