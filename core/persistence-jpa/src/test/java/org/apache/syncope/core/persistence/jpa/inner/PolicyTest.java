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
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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

        AccessPolicy accessPolicy = policyDAO.find("617735c7-deb3-40b3-8a9a-683037e523a2");
        assertNull(accessPolicy);
        accessPolicy = policyDAO.find("419935c7-deb3-40b3-8a9a-683037e523a2");
        assertNotNull(accessPolicy);
        accessPolicy = policyDAO.find(UUID.randomUUID().toString());
        assertNull(accessPolicy);

        AuthPolicy authPolicy = policyDAO.find("b912a0d4-a890-416f-9ab8-84ab077eb028");
        assertNotNull(authPolicy);
        authPolicy = policyDAO.find("659b9906-4b6e-4bc0-aca0-6809dff346d4");
        assertNotNull(authPolicy);
        authPolicy = policyDAO.find(UUID.randomUUID().toString());
        assertNull(authPolicy);

        AttrReleasePolicy attrReleasePolicy = policyDAO.find("019935c7-deb3-40b3-8a9a-683037e523a2");
        assertNull(attrReleasePolicy);
        attrReleasePolicy = policyDAO.find("319935c7-deb3-40b3-8a9a-683037e523a2");
        assertNotNull(attrReleasePolicy);
        attrReleasePolicy = policyDAO.find(UUID.randomUUID().toString());
        assertNull(attrReleasePolicy);
    }

    @Test
    public void findByType() {
        List<PropagationPolicy> propagationPolicies = policyDAO.find(PropagationPolicy.class);
        assertNotNull(propagationPolicies);
        assertFalse(propagationPolicies.isEmpty());

        List<PullPolicy> pullPolicies = policyDAO.find(PullPolicy.class);
        assertNotNull(pullPolicies);
        assertFalse(pullPolicies.isEmpty());

        List<AccessPolicy> accessPolicies = policyDAO.find(AccessPolicy.class);
        assertNotNull(accessPolicies);
        assertEquals(1, accessPolicies.size());

        List<AuthPolicy> authPolicies = policyDAO.find(AuthPolicy.class);
        assertNotNull(authPolicies);
        assertEquals(2, authPolicies.size());

        List<AttrReleasePolicy> attrReleasePolicies = policyDAO.find(AttrReleasePolicy.class);
        assertNotNull(attrReleasePolicies);
        assertEquals(2, attrReleasePolicies.size());
    }

    @Test
    public void createPropagation() {
        int beforeCount = policyDAO.findAll().size();

        PropagationPolicy propagationPolicy = entityFactory.newEntity(PropagationPolicy.class);
        propagationPolicy.setName("Propagation policy");
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
        PullPolicy pullPolicy = entityFactory.newEntity(PullPolicy.class);
        pullPolicy.setConflictResolutionAction(ConflictResolutionAction.IGNORE);
        pullPolicy.setName("Pull policy");

        final String pullURuleName = "net.tirasa.pull.correlation.TirasaURule";
        final String pullGRuleName = "net.tirasa.pull.correlation.TirasaGRule";

        Implementation impl1 = entityFactory.newEntity(Implementation.class);
        impl1.setKey(pullURuleName);
        impl1.setEngine(ImplementationEngine.JAVA);
        impl1.setType(IdMImplementationType.PULL_CORRELATION_RULE);
        impl1.setBody(PullCorrelationRule.class.getName());
        impl1 = implementationDAO.save(impl1);

        PullCorrelationRuleEntity rule1 = entityFactory.newEntity(PullCorrelationRuleEntity.class);
        rule1.setAnyType(anyTypeDAO.findUser());
        rule1.setPullPolicy(pullPolicy);
        rule1.setImplementation(impl1);
        pullPolicy.add(rule1);

        Implementation impl2 = entityFactory.newEntity(Implementation.class);
        impl2.setKey(pullGRuleName);
        impl2.setEngine(ImplementationEngine.JAVA);
        impl2.setType(IdMImplementationType.PULL_CORRELATION_RULE);
        impl2.setBody(PullCorrelationRule.class.getName());
        impl2 = implementationDAO.save(impl2);

        PullCorrelationRuleEntity rule2 = entityFactory.newEntity(PullCorrelationRuleEntity.class);
        rule2.setAnyType(anyTypeDAO.findGroup());
        rule2.setPullPolicy(pullPolicy);
        rule2.setImplementation(impl2);
        pullPolicy.add(rule2);

        pullPolicy = policyDAO.save(pullPolicy);

        assertNotNull(pullPolicy);
        assertEquals(pullURuleName,
                pullPolicy.getCorrelationRule(anyTypeDAO.findUser()).get().getImplementation().getKey());
        assertEquals(pullGRuleName,
                pullPolicy.getCorrelationRule(anyTypeDAO.findGroup()).get().getImplementation().getKey());
    }

    @Test
    public void createPush() {
        PushPolicy pushPolicy = entityFactory.newEntity(PushPolicy.class);
        pushPolicy.setName("Push policy");
        pushPolicy.setConflictResolutionAction(ConflictResolutionAction.IGNORE);

        final String pushURuleName = "net.tirasa.push.correlation.TirasaURule";
        final String pushGRuleName = "net.tirasa.push.correlation.TirasaGRule";

        Implementation impl1 = entityFactory.newEntity(Implementation.class);
        impl1.setKey(pushURuleName);
        impl1.setEngine(ImplementationEngine.JAVA);
        impl1.setType(IdMImplementationType.PUSH_CORRELATION_RULE);
        impl1.setBody(PushCorrelationRule.class.getName());
        impl1 = implementationDAO.save(impl1);

        PushCorrelationRuleEntity rule1 = entityFactory.newEntity(PushCorrelationRuleEntity.class);
        rule1.setAnyType(anyTypeDAO.findUser());
        rule1.setPushPolicy(pushPolicy);
        rule1.setImplementation(impl1);
        pushPolicy.add(rule1);

        Implementation impl2 = entityFactory.newEntity(Implementation.class);
        impl2.setKey(pushGRuleName);
        impl2.setEngine(ImplementationEngine.JAVA);
        impl2.setType(IdMImplementationType.PUSH_CORRELATION_RULE);
        impl2.setBody(PushCorrelationRule.class.getName());
        impl2 = implementationDAO.save(impl2);

        PushCorrelationRuleEntity rule2 = entityFactory.newEntity(PushCorrelationRuleEntity.class);
        rule2.setAnyType(anyTypeDAO.findGroup());
        rule2.setPushPolicy(pushPolicy);
        rule2.setImplementation(impl2);
        pushPolicy.add(rule2);

        pushPolicy = policyDAO.save(pushPolicy);

        assertNotNull(pushPolicy);
        assertEquals(pushURuleName,
                pushPolicy.getCorrelationRule(anyTypeDAO.findUser()).get().getImplementation().getKey());
        assertEquals(pushGRuleName,
                pushPolicy.getCorrelationRule(anyTypeDAO.findGroup()).get().getImplementation().getKey());
    }

    @Test
    public void createAccess() {
        int beforeCount = policyDAO.findAll().size();

        AccessPolicy accessPolicy = entityFactory.newEntity(AccessPolicy.class);
        accessPolicy.setName("AttrReleasePolicyAllowEverything");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.getRequiredAttrs().add(new Attr.Builder("cn").value("syncope").build());
        accessPolicy.setConf(conf);

        accessPolicy = policyDAO.save(accessPolicy);

        assertNotNull(accessPolicy);
        assertNotNull(accessPolicy.getKey());

        int afterCount = policyDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createAuth() {
        int beforeCount = policyDAO.findAll().size();

        AuthPolicy authPolicy = entityFactory.newEntity(AuthPolicy.class);
        authPolicy.setName("AuthPolicyTest");

        DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
        authPolicyConf.getAuthModules().addAll(List.of("LdapAuthentication1", "DatabaseAuthentication2"));
        authPolicyConf.setTryAll(true);
        authPolicy.setConf(authPolicyConf);

        authPolicy = policyDAO.save(authPolicy);

        assertNotNull(authPolicy);
        assertNotNull(authPolicy.getKey());

        int afterCount = policyDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createAttrRelease() {
        int beforeCount = policyDAO.findAll().size();

        AttrReleasePolicy attrReleasePolicy = entityFactory.newEntity(AttrReleasePolicy.class);
        attrReleasePolicy.setName("AttrReleasePolicyAllowEverything");
        attrReleasePolicy.setStatus(Boolean.TRUE);

        DefaultAttrReleasePolicyConf attrReleasePolicyConf = new DefaultAttrReleasePolicyConf();
        attrReleasePolicyConf.getAllowedAttrs().add("*");
        attrReleasePolicyConf.getIncludeOnlyAttrs().add("cn");
        attrReleasePolicy.setConf(attrReleasePolicyConf);

        attrReleasePolicy = policyDAO.save(attrReleasePolicy);

        assertNotNull(attrReleasePolicy);
        assertNotNull(attrReleasePolicy.getKey());
        assertNotNull(attrReleasePolicy.getStatus());
        assertNotNull(((DefaultAttrReleasePolicyConf) attrReleasePolicy.getConf()).getAllowedAttrs());

        int afterCount = policyDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
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
        rule.setType(IdRepoImplementationType.PASSWORD_RULE);
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

        AccessPolicy accessPolicy = policyDAO.find("419935c7-deb3-40b3-8a9a-683037e523a2");
        assertNotNull(accessPolicy);
        policyDAO.delete(accessPolicy);
        accessPolicy = policyDAO.find("419935c7-deb3-40b3-8a9a-683037e523a2");
        assertNull(accessPolicy);

        AuthPolicy authPolicy = policyDAO.find("b912a0d4-a890-416f-9ab8-84ab077eb028");
        assertNotNull(authPolicy);
        policyDAO.delete(authPolicy);
        authPolicy = policyDAO.find("b912a0d4-a890-416f-9ab8-84ab077eb028");
        assertNull(authPolicy);

        AttrReleasePolicy attrReleasepolicy = policyDAO.find("319935c7-deb3-40b3-8a9a-683037e523a2");
        assertNotNull(attrReleasepolicy);
        policyDAO.delete(attrReleasepolicy);
        attrReleasepolicy = policyDAO.find("319935c7-deb3-40b3-8a9a-683037e523a2");
        assertNull(attrReleasepolicy);
    }
}
