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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultInboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPushCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PolicyTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void findAll() {
        assertEquals(17, policyDAO.count());
        assertEquals(17, policyDAO.findAll().size());

        assertEquals(1, policyDAO.findAll(AccessPolicy.class).size());
        assertEquals(2, policyDAO.findAll(AccountPolicy.class).size());
        assertEquals(2, policyDAO.findAll(AttrReleasePolicy.class).size());
        assertEquals(2, policyDAO.findAll(AuthPolicy.class).size());
        assertEquals(3, policyDAO.findAll(PasswordPolicy.class).size());
        assertEquals(2, policyDAO.findAll(PropagationPolicy.class).size());
        assertEquals(4, policyDAO.findAll(InboundPolicy.class).size());
        assertEquals(1, policyDAO.findAll(PushPolicy.class).size());
    }

    @Test
    public void findByKey() {
        PropagationPolicy propagationPolicy = policyDAO.findById(
                "89d322db-9878-420c-b49c-67be13df9a12", PropagationPolicy.class).orElseThrow();
        assertEquals(BackOffStrategy.FIXED, propagationPolicy.getBackOffStrategy());
        assertEquals("10000", propagationPolicy.getBackOffParams());
        assertEquals(5, propagationPolicy.getMaxAttempts());

        InboundPolicy inboundPolicy = policyDAO.findById("880f8553-069b-4aed-9930-2cd53873f544", InboundPolicy.class).
                orElseThrow();

        InboundCorrelationRuleEntity pullCR = inboundPolicy.getCorrelationRule(AnyTypeKind.USER.name()).orElseThrow();
        DefaultInboundCorrelationRuleConf pullCRConf =
                POJOHelper.deserialize(pullCR.getImplementation().getBody(), DefaultInboundCorrelationRuleConf.class);
        assertNotNull(pullCRConf);
        assertEquals(2, pullCRConf.getSchemas().size());
        assertTrue(pullCRConf.getSchemas().contains("username"));
        assertTrue(pullCRConf.getSchemas().contains("firstname"));

        PushPolicy pushPolicy = policyDAO.findById(
                "fb6530e5-892d-4f47-a46b-180c5b6c5c83", PushPolicy.class).orElseThrow();

        PushCorrelationRuleEntity pushCR = pushPolicy.getCorrelationRule(AnyTypeKind.USER.name()).orElseThrow();
        DefaultPushCorrelationRuleConf pushCRConf =
                POJOHelper.deserialize(pushCR.getImplementation().getBody(), DefaultPushCorrelationRuleConf.class);
        assertNotNull(pushCRConf);
        assertEquals(1, pushCRConf.getSchemas().size());
        assertTrue(pushCRConf.getSchemas().contains("surname"));

        assertTrue(policyDAO.findById("617735c7-deb3-40b3-8a9a-683037e523a2", AccessPolicy.class).isEmpty());
        assertTrue(policyDAO.findById("419935c7-deb3-40b3-8a9a-683037e523a2", AccessPolicy.class).isPresent());
        assertTrue(policyDAO.findById(UUID.randomUUID().toString(), AccessPolicy.class).isEmpty());

        assertTrue(policyDAO.findById("b912a0d4-a890-416f-9ab8-84ab077eb028", AuthPolicy.class).isPresent());
        assertTrue(policyDAO.findById("659b9906-4b6e-4bc0-aca0-6809dff346d4", AuthPolicy.class).isPresent());
        assertTrue(policyDAO.findById(UUID.randomUUID().toString(), AuthPolicy.class).isEmpty());

        assertTrue(policyDAO.findById("019935c7-deb3-40b3-8a9a-683037e523a2", AttrReleasePolicy.class).isEmpty());
        assertTrue(policyDAO.findById("319935c7-deb3-40b3-8a9a-683037e523a2", AttrReleasePolicy.class).isPresent());
        assertTrue(policyDAO.findById(UUID.randomUUID().toString(), AttrReleasePolicy.class).isEmpty());
    }

    @Test
    public void createPropagation() {
        long beforeCount = policyDAO.count();

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

        long afterCount = policyDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createPull() {
        InboundPolicy inboundPolicy = entityFactory.newEntity(InboundPolicy.class);
        inboundPolicy.setConflictResolutionAction(ConflictResolutionAction.IGNORE);
        inboundPolicy.setName("Pull policy");

        final String pullURuleName = "net.tirasa.pull.correlation.TirasaURule";
        final String pullGRuleName = "net.tirasa.pull.correlation.TirasaGRule";

        Implementation impl1 = entityFactory.newEntity(Implementation.class);
        impl1.setKey(pullURuleName);
        impl1.setEngine(ImplementationEngine.JAVA);
        impl1.setType(IdMImplementationType.INBOUND_CORRELATION_RULE);
        impl1.setBody(InboundCorrelationRule.class.getName());
        impl1 = implementationDAO.save(impl1);

        InboundCorrelationRuleEntity rule1 = entityFactory.newEntity(InboundCorrelationRuleEntity.class);
        rule1.setAnyType(anyTypeDAO.getUser());
        rule1.setInboundPolicy(inboundPolicy);
        rule1.setImplementation(impl1);
        inboundPolicy.add(rule1);

        Implementation impl2 = entityFactory.newEntity(Implementation.class);
        impl2.setKey(pullGRuleName);
        impl2.setEngine(ImplementationEngine.JAVA);
        impl2.setType(IdMImplementationType.INBOUND_CORRELATION_RULE);
        impl2.setBody(InboundCorrelationRule.class.getName());
        impl2 = implementationDAO.save(impl2);

        InboundCorrelationRuleEntity rule2 = entityFactory.newEntity(InboundCorrelationRuleEntity.class);
        rule2.setAnyType(anyTypeDAO.getGroup());
        rule2.setInboundPolicy(inboundPolicy);
        rule2.setImplementation(impl2);
        inboundPolicy.add(rule2);

        inboundPolicy = policyDAO.save(inboundPolicy);

        assertNotNull(inboundPolicy);
        assertEquals(pullURuleName,
                inboundPolicy.getCorrelationRule(AnyTypeKind.USER.name()).get().getImplementation().getKey());
        assertEquals(pullGRuleName,
                inboundPolicy.getCorrelationRule(AnyTypeKind.GROUP.name()).get().getImplementation().getKey());
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
        rule1.setAnyType(anyTypeDAO.getUser());
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
        rule2.setAnyType(anyTypeDAO.getGroup());
        rule2.setPushPolicy(pushPolicy);
        rule2.setImplementation(impl2);
        pushPolicy.add(rule2);

        pushPolicy = policyDAO.save(pushPolicy);

        assertNotNull(pushPolicy);
        assertEquals(pushURuleName,
                pushPolicy.getCorrelationRule(AnyTypeKind.USER.name()).get().getImplementation().getKey());
        assertEquals(pushGRuleName,
                pushPolicy.getCorrelationRule(AnyTypeKind.GROUP.name()).get().getImplementation().getKey());
    }

    @Test
    public void createAccess() {
        long beforeCount = policyDAO.count();

        AccessPolicy accessPolicy = entityFactory.newEntity(AccessPolicy.class);
        accessPolicy.setName("AttrReleasePolicyAllowEverything");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.getRequiredAttrs().put("cn", "syncope");
        accessPolicy.setConf(conf);

        accessPolicy = policyDAO.save(accessPolicy);

        assertNotNull(accessPolicy);
        assertNotNull(accessPolicy.getKey());

        long afterCount = policyDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createAuth() {
        long beforeCount = policyDAO.count();

        AuthPolicy authPolicy = entityFactory.newEntity(AuthPolicy.class);
        authPolicy.setName("AuthPolicyTest");

        DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
        authPolicyConf.getAuthModules().addAll(List.of("LdapAuthentication1", "DatabaseAuthentication2"));
        authPolicyConf.setTryAll(true);
        authPolicy.setConf(authPolicyConf);

        authPolicy = policyDAO.save(authPolicy);

        assertNotNull(authPolicy);
        assertNotNull(authPolicy.getKey());

        long afterCount = policyDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createAttrRelease() {
        long beforeCount = policyDAO.count();

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

        long afterCount = policyDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void createTicketExpiration() {
        long beforeCount = policyDAO.count();

        TicketExpirationPolicy ticketExpirationPolicy = entityFactory.newEntity(TicketExpirationPolicy.class);
        ticketExpirationPolicy.setName("TicketExpirationPolicyTest");

        DefaultTicketExpirationPolicyConf ticketExpirationPolicyConf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(110);
        ticketExpirationPolicyConf.setTgtConf(tgtConf);
        DefaultTicketExpirationPolicyConf.STConf stConf = new DefaultTicketExpirationPolicyConf.STConf();
        stConf.setMaxTimeToLiveInSeconds(0);
        stConf.setNumberOfUses(1);
        ticketExpirationPolicyConf.setStConf(stConf);
        ticketExpirationPolicy.setConf(ticketExpirationPolicyConf);

        ticketExpirationPolicy = policyDAO.save(ticketExpirationPolicy);

        assertNotNull(ticketExpirationPolicy);
        assertNotNull(ticketExpirationPolicy.getKey());
        assertNotNull(((DefaultTicketExpirationPolicyConf) ticketExpirationPolicy.getConf()).getTgtConf());
        assertNotNull(((DefaultTicketExpirationPolicyConf) ticketExpirationPolicy.getConf()).getStConf());

        long afterCount = policyDAO.count();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void update() {
        PasswordPolicy policy = policyDAO.findById(
                "ce93fcda-dc3a-4369-a7b0-a6108c261c85", PasswordPolicy.class).orElseThrow();
        assertEquals(1, policy.getRules().size());

        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setMaxLength(8);
        ruleConf.setMinLength(6);

        Implementation rule = entityFactory.newEntity(Implementation.class);
        rule.setKey("PasswordRule" + UUID.randomUUID());
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
        Policy policy = policyDAO.findById("66691e96-285f-4464-bc19-e68384ea4c85").orElseThrow();
        policyDAO.delete(policy);
        assertTrue(policyDAO.findById("66691e96-285f-4464-bc19-e68384ea4c85").isEmpty());

        AccessPolicy accessPolicy = policyDAO.findById(
                "419935c7-deb3-40b3-8a9a-683037e523a2", AccessPolicy.class).orElseThrow();
        policyDAO.delete(accessPolicy);
        assertTrue(policyDAO.findById("419935c7-deb3-40b3-8a9a-683037e523a2").isEmpty());
        assertTrue(policyDAO.findById("419935c7-deb3-40b3-8a9a-683037e523a2", AccessPolicy.class).isEmpty());

        AuthPolicy authPolicy = policyDAO.findById(
                "b912a0d4-a890-416f-9ab8-84ab077eb028", AuthPolicy.class).orElseThrow();
        policyDAO.delete(authPolicy);
        assertTrue(policyDAO.findById("b912a0d4-a890-416f-9ab8-84ab077eb028").isEmpty());
        assertTrue(policyDAO.findById("b912a0d4-a890-416f-9ab8-84ab077eb028", AuthPolicy.class).isEmpty());

        AttrReleasePolicy attrReleasepolicy = policyDAO.findById(
                "319935c7-deb3-40b3-8a9a-683037e523a2", AttrReleasePolicy.class).orElseThrow();
        policyDAO.delete(attrReleasepolicy);
        assertTrue(policyDAO.findById("319935c7-deb3-40b3-8a9a-683037e523a2").isEmpty());
        assertTrue(policyDAO.findById("319935c7-deb3-40b3-8a9a-683037e523a2", AttrReleasePolicy.class).isEmpty());
    }
}
