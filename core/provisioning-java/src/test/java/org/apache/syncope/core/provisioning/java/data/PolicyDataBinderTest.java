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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.InboundPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.policy.PushPolicyTO;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PolicyDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private PolicyDataBinder binder;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    void getPasswordPolicyTO() {
        PasswordPolicy policy = policyDAO.findById(
                "ce93fcda-dc3a-4369-a7b0-a6108c261c85", PasswordPolicy.class).orElseThrow();

        PasswordPolicyTO passwordPolicyTO = binder.getPolicyTO(policy);
        assertEquals(policy.getKey(), passwordPolicyTO.getKey());
        assertEquals("a password policy", passwordPolicyTO.getName());
        assertTrue(passwordPolicyTO.isAllowNullPassword());
        assertEquals(1, passwordPolicyTO.getHistoryLength());
        assertEquals(List.of("DefaultPasswordRuleConf1"), passwordPolicyTO.getRules());
        assertTrue(passwordPolicyTO.getUsedByRealms().contains("/even/two"));
    }

    @Test
    void getAccountPolicyTO() {
        AccountPolicy policy = policyDAO.findById(
                "20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", AccountPolicy.class).orElseThrow();

        AccountPolicyTO accountPolicyTO = binder.getPolicyTO(policy);
        assertEquals("an account policy", accountPolicyTO.getName());
        assertEquals(0, accountPolicyTO.getMaxAuthenticationAttempts());
        assertEquals(List.of("DefaultAccountRuleConf1"), accountPolicyTO.getRules());
        assertTrue(accountPolicyTO.getUsedByRealms().contains("/even/two"));
    }

    @Test
    void getAuthAndAccessPolicyTO() {
        AuthPolicy authPolicy = policyDAO.findById(
                "b912a0d4-a890-416f-9ab8-84ab077eb028", AuthPolicy.class).orElseThrow();
        AuthPolicyTO authPolicyTO = binder.getPolicyTO(authPolicy);
        assertEquals("DefaultAuthPolicy", authPolicyTO.getName());
        assertNotNull(authPolicyTO.getConf());

        AccessPolicyTO accessPolicyTO = binder.getPolicyTO(policyDAO.findById(
                "419935c7-deb3-40b3-8a9a-683037e523a2").orElseThrow());
        assertEquals("DefaultAccessPolicy", accessPolicyTO.getName());
        assertNotNull(accessPolicyTO.getConf());
    }

    @Test
    void getAttrReleaseAndInboundPolicyTO() {
        AttrReleasePolicy attrReleasePolicy = policyDAO.findById(
                "319935c7-deb3-40b3-8a9a-683037e523a2", AttrReleasePolicy.class).orElseThrow();
        AttrReleasePolicyTO attrReleasePolicyTO = binder.getPolicyTO(attrReleasePolicy);
        assertEquals("AllowedAttrReleasePolicy", attrReleasePolicyTO.getName());
        assertEquals(0, attrReleasePolicyTO.getOrder());
        assertNotNull(attrReleasePolicyTO.getConf());

        InboundPolicy inboundPolicy = policyDAO.findById(
                "880f8553-069b-4aed-9930-2cd53873f544", InboundPolicy.class).orElseThrow();
        InboundPolicyTO inboundPolicyTO = binder.getPolicyTO(inboundPolicy);
        assertEquals("another pull policy", inboundPolicyTO.getName());
        assertEquals(ConflictResolutionAction.ALL, inboundPolicyTO.getConflictResolutionAction());
        assertEquals("TestInboundCorrelationRule", inboundPolicyTO.getCorrelationRules().get(AnyTypeKind.USER.name()));
    }

    @Test
    void createUpdatePasswordPolicy() {
        PasswordPolicyTO passwordPolicyTO = new PasswordPolicyTO();
        passwordPolicyTO.setName("test password policy");
        passwordPolicyTO.setAllowNullPassword(true);
        passwordPolicyTO.setHistoryLength(3);
        passwordPolicyTO.getRules().add("DefaultPasswordRuleConf1");
        passwordPolicyTO.getRules().add("not-existing");

        Policy policy = policyDAO.save(binder.create(passwordPolicyTO));

        PasswordPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test password policy", created.getName());
        assertTrue(created.isAllowNullPassword());
        assertEquals(3, created.getHistoryLength());
        assertEquals(List.of("DefaultPasswordRuleConf1"), created.getRules());

        created.setHistoryLength(5);
        created.getRules().clear();
        created.getRules().add("DefaultPasswordRuleConf2");
        binder.update(policy, created);

        PasswordPolicyTO updated = binder.getPolicyTO(policy);
        assertEquals(5, updated.getHistoryLength());
        assertEquals(List.of("DefaultPasswordRuleConf2"), updated.getRules());
    }

    @Test
    void invalidInboundCorrelationRule() {
        InboundPolicyTO inboundPolicyTO = new InboundPolicyTO();
        inboundPolicyTO.setName("test inbound policy");
        inboundPolicyTO.getCorrelationRules().put(AnyTypeKind.USER.name(), "not-existing");

        assertThrows(NotFoundException.class, () -> binder.create(inboundPolicyTO));
    }

    @Test
    void getPropagationAndPushPolicyTO() {
        PropagationPolicy propagationPolicy = policyDAO.findById(
                "89d322db-9878-420c-b49c-67be13df9a12", PropagationPolicy.class).orElseThrow();
        PropagationPolicyTO propagationPolicyTO = binder.getPolicyTO(propagationPolicy);
        assertEquals("sample propagation policy", propagationPolicyTO.getName());
        assertTrue(propagationPolicyTO.isFetchAroundProvisioning());
        assertEquals(BackOffStrategy.FIXED, propagationPolicyTO.getBackOffStrategy());
        assertEquals("10000", propagationPolicyTO.getBackOffParams());
        assertEquals(5, propagationPolicyTO.getMaxAttempts());
        assertTrue(propagationPolicyTO.getUsedByResources().contains("resource-db-scripted"));

        PushPolicy pushPolicy = policyDAO.findById(
                "fb6530e5-892d-4f47-a46b-180c5b6c5c83", PushPolicy.class).orElseThrow();
        PushPolicyTO pushPolicyTO = binder.getPolicyTO(pushPolicy);
        assertEquals("a push policy", pushPolicyTO.getName());
        assertEquals(ConflictResolutionAction.IGNORE, pushPolicyTO.getConflictResolutionAction());
        assertEquals(Map.of(AnyTypeKind.USER.name(), "TestPushCorrelationRule"), pushPolicyTO.getCorrelationRules());
    }

    @Test
    void createUpdateAccountPolicy() {
        AccountPolicyTO accountPolicyTO = new AccountPolicyTO();
        accountPolicyTO.setName("test account policy");
        accountPolicyTO.setMaxAuthenticationAttempts(4);
        accountPolicyTO.setPropagateSuspension(true);
        accountPolicyTO.getRules().add("DefaultAccountRuleConf1");
        accountPolicyTO.getRules().add("not-existing");

        Policy policy = policyDAO.save(binder.create(accountPolicyTO));

        AccountPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test account policy", created.getName());
        assertEquals(4, created.getMaxAuthenticationAttempts());
        assertTrue(created.isPropagateSuspension());
        assertEquals(List.of("DefaultAccountRuleConf1"), created.getRules());

        created.setMaxAuthenticationAttempts(7);
        created.setPropagateSuspension(false);
        created.getRules().clear();
        created.getRules().add("DefaultAccountRuleConf2");
        binder.update(policy, created);

        AccountPolicyTO updated = binder.getPolicyTO(policy);
        assertEquals(7, updated.getMaxAuthenticationAttempts());
        assertFalse(updated.isPropagateSuspension());
        assertEquals(List.of("DefaultAccountRuleConf2"), updated.getRules());
    }

    @Test
    void createUpdatePropagationPolicy() {
        PropagationPolicyTO propagationPolicyTO = new PropagationPolicyTO();
        propagationPolicyTO.setName("test propagation policy");
        propagationPolicyTO.setFetchAroundProvisioning(false);
        propagationPolicyTO.setUpdateDelta(true);
        propagationPolicyTO.setBackOffStrategy(BackOffStrategy.EXPONENTIAL);
        propagationPolicyTO.setBackOffParams("200;40000;3");
        propagationPolicyTO.setMaxAttempts(6);

        Policy policy = policyDAO.save(binder.create(propagationPolicyTO));

        PropagationPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test propagation policy", created.getName());
        assertFalse(created.isFetchAroundProvisioning());
        assertTrue(created.isUpdateDelta());
        assertEquals(BackOffStrategy.EXPONENTIAL, created.getBackOffStrategy());
        assertEquals("200;40000;3", created.getBackOffParams());
        assertEquals(6, created.getMaxAttempts());

        created.setFetchAroundProvisioning(true);
        created.setUpdateDelta(false);
        created.setBackOffStrategy(BackOffStrategy.RANDOM);
        created.setMaxAttempts(2);
        binder.update(policy, created);

        PropagationPolicyTO updated = binder.getPolicyTO(policy);
        assertTrue(updated.isFetchAroundProvisioning());
        assertFalse(updated.isUpdateDelta());
        assertEquals(BackOffStrategy.RANDOM, updated.getBackOffStrategy());
        assertEquals(2, updated.getMaxAttempts());
    }

    @Test
    void createUpdatePushPolicy() {
        PushPolicyTO pushPolicyTO = new PushPolicyTO();
        pushPolicyTO.setName("test push policy");
        pushPolicyTO.setConflictResolutionAction(ConflictResolutionAction.ALL);
        pushPolicyTO.getCorrelationRules().put(AnyTypeKind.USER.name(), "TestPushCorrelationRule");
        pushPolicyTO.getCorrelationRules().put("not-existing", "TestPushCorrelationRule");

        Policy policy = policyDAO.save(binder.create(pushPolicyTO));

        PushPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test push policy", created.getName());
        assertEquals(ConflictResolutionAction.ALL, created.getConflictResolutionAction());
        assertEquals(Map.of(AnyTypeKind.USER.name(), "TestPushCorrelationRule"), created.getCorrelationRules());

        created.setConflictResolutionAction(ConflictResolutionAction.IGNORE);
        created.getCorrelationRules().clear();
        binder.update(policy, created);

        PushPolicyTO updated = binder.getPolicyTO(policy);
        assertEquals(ConflictResolutionAction.IGNORE, updated.getConflictResolutionAction());
        assertTrue(updated.getCorrelationRules().isEmpty());

        PushPolicyTO invalid = new PushPolicyTO();
        invalid.setName("invalid push policy");
        invalid.getCorrelationRules().put(AnyTypeKind.USER.name(), "not-existing");
        assertThrows(NotFoundException.class, () -> binder.create(invalid));
    }

    @Test
    void createUpdateAuthPolicy() {
        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.setTryAll(true);
        conf.setBypassEnabled(true);
        conf.getAuthModules().add("DefaultLDAPAuthModule");
        conf.getAuthModules().add("not-existing");

        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        authPolicyTO.setName("test auth policy");
        authPolicyTO.setConf(conf);

        Policy policy = policyDAO.save(binder.create(authPolicyTO));

        AuthPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test auth policy", created.getName());
        DefaultAuthPolicyConf createdConf = (DefaultAuthPolicyConf) created.getConf();
        assertTrue(createdConf.isTryAll());
        assertTrue(createdConf.isBypassEnabled());
        assertEquals(List.of("DefaultLDAPAuthModule", "not-existing"), createdConf.getAuthModules());

        createdConf.setTryAll(false);
        createdConf.getAuthModules().clear();
        binder.update(policy, created);

        DefaultAuthPolicyConf updatedConf = (DefaultAuthPolicyConf) binder.<AuthPolicyTO>getPolicyTO(policy).getConf();
        assertFalse(updatedConf.isTryAll());
        assertTrue(updatedConf.getAuthModules().isEmpty());
    }

    @Test
    void createUpdateAccessPolicy() {
        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setOrder(2);
        conf.setEnabled(true);
        conf.setSsoEnabled(false);
        conf.setCaseInsensitive(true);
        conf.setRequireAllAttributes(false);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setName("test access policy");
        accessPolicyTO.setConf(conf);

        Policy policy = policyDAO.save(binder.create(accessPolicyTO));

        AccessPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test access policy", created.getName());
        DefaultAccessPolicyConf createdConf = (DefaultAccessPolicyConf) created.getConf();
        assertEquals(2, createdConf.getOrder());
        assertTrue(createdConf.isEnabled());
        assertFalse(createdConf.isSsoEnabled());
        assertTrue(createdConf.isCaseInsensitive());
        assertFalse(createdConf.isRequireAllAttributes());

        createdConf.setSsoEnabled(true);
        createdConf.setOrder(1);
        binder.update(policy, created);

        DefaultAccessPolicyConf updatedConf =
                (DefaultAccessPolicyConf) binder.<AccessPolicyTO>getPolicyTO(policy).getConf();
        assertTrue(updatedConf.isSsoEnabled());
        assertEquals(1, updatedConf.getOrder());
    }

    @Test
    void createUpdateAttrReleasePolicy() {
        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getAllowedAttrs().add("cn");
        conf.getAllowedAttrs().add("givenName");
        conf.getExcludedAttrs().add("secret");
        conf.setPrincipalIdAttr("uid");

        AttrReleasePolicyTO attrReleasePolicyTO = new AttrReleasePolicyTO();
        attrReleasePolicyTO.setName("test attr release policy");
        attrReleasePolicyTO.setOrder(3);
        attrReleasePolicyTO.setStatus(false);
        attrReleasePolicyTO.setConf(conf);

        Policy policy = policyDAO.save(binder.create(attrReleasePolicyTO));

        AttrReleasePolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test attr release policy", created.getName());
        assertEquals(3, created.getOrder());
        assertFalse(created.getStatus());
        DefaultAttrReleasePolicyConf createdConf = (DefaultAttrReleasePolicyConf) created.getConf();
        assertEquals(List.of("cn", "givenName"), createdConf.getAllowedAttrs());
        assertEquals(List.of("secret"), createdConf.getExcludedAttrs());
        assertEquals("uid", createdConf.getPrincipalIdAttr());

        created.setOrder(5);
        created.setStatus(true);
        createdConf.getAllowedAttrs().clear();
        binder.update(policy, created);

        AttrReleasePolicyTO updated = binder.getPolicyTO(policy);
        assertEquals(5, updated.getOrder());
        assertTrue(updated.getStatus());
        assertTrue(((DefaultAttrReleasePolicyConf) updated.getConf()).getAllowedAttrs().isEmpty());
    }

    @Test
    void createUpdateTicketExpirationPolicy() {
        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(600);
        conf.setTgtConf(tgtConf);
        DefaultTicketExpirationPolicyConf.STConf stConf = new DefaultTicketExpirationPolicyConf.STConf();
        stConf.setMaxTimeToLiveInSeconds(300);
        stConf.setNumberOfUses(2);
        conf.setStConf(stConf);

        TicketExpirationPolicyTO ticketExpirationPolicyTO = new TicketExpirationPolicyTO();
        ticketExpirationPolicyTO.setName("test ticket expiration policy");
        ticketExpirationPolicyTO.setConf(conf);

        Policy policy = policyDAO.save(binder.create(ticketExpirationPolicyTO));

        TicketExpirationPolicyTO created = binder.getPolicyTO(policy);
        assertNotNull(created.getKey());
        assertEquals("test ticket expiration policy", created.getName());
        DefaultTicketExpirationPolicyConf createdConf =
                (DefaultTicketExpirationPolicyConf) created.getConf();
        assertEquals(600, createdConf.getTgtConf().getMaxTimeToLiveInSeconds());
        assertEquals(300, createdConf.getStConf().getMaxTimeToLiveInSeconds());
        assertEquals(2, createdConf.getStConf().getNumberOfUses());

        createdConf.getTgtConf().setMaxTimeToLiveInSeconds(900);
        binder.update(policy, created);

        DefaultTicketExpirationPolicyConf updatedConf = (DefaultTicketExpirationPolicyConf) binder.
                <TicketExpirationPolicyTO>getPolicyTO(policy).getConf();
        assertEquals(900, updatedConf.getTgtConf().getMaxTimeToLiveInSeconds());
    }
}
