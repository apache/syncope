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
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;

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
        PropagationPolicy propagationPolicy = policyDAO.find("89d322db-9878-420c-b49c-67be13df9a12");
        assertNotNull(propagationPolicy);
        assertEquals(BackOffStrategy.FIXED, propagationPolicy.getBackOffStrategy());
        assertEquals("10000", propagationPolicy.getBackOffParams());
        assertEquals(5, propagationPolicy.getMaxAttempts());

        PullPolicy pullPolicy = policyDAO.find("880f8553-069b-4aed-9930-2cd53873f544");
        assertNotNull(pullPolicy);

        PullCorrelationRuleEntity pullCR = pullPolicy.getCorrelationRule(anyTypeDAO.findUser()).orElse(null);
        assertNotNull(pullCR);
        DefaultPullCorrelationRuleConf pullCRConf =
                POJOHelper.deserialize(pullCR.getImplementation().getBody(), DefaultPullCorrelationRuleConf.class);
        assertNotNull(pullCRConf);
        assertEquals(2, pullCRConf.getSchemas().size());
        assertTrue(pullCRConf.getSchemas().contains("username"));
        assertTrue(pullCRConf.getSchemas().contains("firstname"));

        PushPolicy pushPolicy = policyDAO.find("fb6530e5-892d-4f47-a46b-180c5b6c5c83");
        assertNotNull(pushPolicy);

        PushCorrelationRuleEntity pushCR = pushPolicy.getCorrelationRule(anyTypeDAO.findUser()).orElse(null);
        assertNotNull(pushCR);
        DefaultPushCorrelationRuleConf pushCRConf =
                POJOHelper.deserialize(pushCR.getImplementation().getBody(), DefaultPushCorrelationRuleConf.class);
        assertNotNull(pushCRConf);
        assertEquals(1, pushCRConf.getSchemas().size());
        assertTrue(pushCRConf.getSchemas().contains("surname"));
    }

    @Test
    public void findByType() {
        List<PullPolicy> policies = policyDAO.find(PullPolicy.class);
        assertNotNull(policies);
        assertFalse(policies.isEmpty());
    }

    @Test
    public void createPropagation() {
        int beforeCount = policyDAO.findAll().size();

        PropagationPolicy propagationPolicy = entityFactory.newEntity(PropagationPolicy.class);
        propagationPolicy.setDescription("Propagation policy");
        propagationPolicy.setMaxAttempts(5);
        propagationPolicy.setBackOffStrategy(BackOffStrategy.EXPONENTIAL);
        propagationPolicy.setBackOffParams(propagationPolicy.getBackOffStrategy().getDefaultBackOffParams());

        propagationPolicy = policyDAO.save(propagationPolicy);
        assertNotNull(propagationPolicy);
        assertEquals(5, propagationPolicy.getMaxAttempts());
        assertEquals(BackOffStrategy.EXPONENTIAL, propagationPolicy.getBackOffStrategy());
        assertEquals(BackOffStrategy.EXPONENTIAL.getDefaultBackOffParams(), propagationPolicy.getBackOffParams());

        int afterCount = policyDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createPull() {
        PullPolicy policy = entityFactory.newEntity(PullPolicy.class);
        policy.setConflictResolutionAction(ConflictResolutionAction.IGNORE);
        policy.setDescription("Pull policy");

        final String pullURuleName = "net.tirasa.pull.correlation.TirasaURule";
        final String pullGRuleName = "net.tirasa.pull.correlation.TirasaGRule";

        Implementation impl1 = entityFactory.newEntity(Implementation.class);
        impl1.setKey(pullURuleName);
        impl1.setEngine(ImplementationEngine.JAVA);
        impl1.setType(ImplementationType.PULL_CORRELATION_RULE);
        impl1.setBody(PullCorrelationRule.class.getName());
        impl1 = implementationDAO.save(impl1);

        PullCorrelationRuleEntity rule1 = entityFactory.newEntity(PullCorrelationRuleEntity.class);
        rule1.setAnyType(anyTypeDAO.findUser());
        rule1.setPullPolicy(policy);
        rule1.setImplementation(impl1);
        policy.add(rule1);

        Implementation impl2 = entityFactory.newEntity(Implementation.class);
        impl2.setKey(pullGRuleName);
        impl2.setEngine(ImplementationEngine.JAVA);
        impl2.setType(ImplementationType.PULL_CORRELATION_RULE);
        impl2.setBody(PullCorrelationRule.class.getName());
        impl2 = implementationDAO.save(impl2);

        PullCorrelationRuleEntity rule2 = entityFactory.newEntity(PullCorrelationRuleEntity.class);
        rule2.setAnyType(anyTypeDAO.findGroup());
        rule2.setPullPolicy(policy);
        rule2.setImplementation(impl2);
        policy.add(rule2);

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(pullURuleName,
                policy.getCorrelationRule(anyTypeDAO.findUser()).get().getImplementation().getKey());
        assertEquals(pullGRuleName,
                policy.getCorrelationRule(anyTypeDAO.findGroup()).get().getImplementation().getKey());
    }

    @Test
    public void createPush() {
        PushPolicy policy = entityFactory.newEntity(PushPolicy.class);
        policy.setConflictResolutionAction(ConflictResolutionAction.IGNORE);
        policy.setDescription("Push policy");

        final String pushURuleName = "net.tirasa.push.correlation.TirasaURule";
        final String pushGRuleName = "net.tirasa.push.correlation.TirasaGRule";

        Implementation impl1 = entityFactory.newEntity(Implementation.class);
        impl1.setKey(pushURuleName);
        impl1.setEngine(ImplementationEngine.JAVA);
        impl1.setType(ImplementationType.PUSH_CORRELATION_RULE);
        impl1.setBody(PushCorrelationRule.class.getName());
        impl1 = implementationDAO.save(impl1);

        PushCorrelationRuleEntity rule1 = entityFactory.newEntity(PushCorrelationRuleEntity.class);
        rule1.setAnyType(anyTypeDAO.findUser());
        rule1.setPushPolicy(policy);
        rule1.setImplementation(impl1);
        policy.add(rule1);

        Implementation impl2 = entityFactory.newEntity(Implementation.class);
        impl2.setKey(pushGRuleName);
        impl2.setEngine(ImplementationEngine.JAVA);
        impl2.setType(ImplementationType.PUSH_CORRELATION_RULE);
        impl2.setBody(PushCorrelationRule.class.getName());
        impl2 = implementationDAO.save(impl2);

        PushCorrelationRuleEntity rule2 = entityFactory.newEntity(PushCorrelationRuleEntity.class);
        rule2.setAnyType(anyTypeDAO.findGroup());
        rule2.setPushPolicy(policy);
        rule2.setImplementation(impl2);
        policy.add(rule2);

        policy = policyDAO.save(policy);

        assertNotNull(policy);
        assertEquals(pushURuleName,
                policy.getCorrelationRule(anyTypeDAO.findUser()).get().getImplementation().getKey());
        assertEquals(pushGRuleName,
                policy.getCorrelationRule(anyTypeDAO.findGroup()).get().getImplementation().getKey());
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
