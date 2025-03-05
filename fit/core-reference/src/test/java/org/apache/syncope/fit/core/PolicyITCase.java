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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.InboundPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.policy.PushPolicyTO;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.DummyInboundCorrelationRule;
import org.apache.syncope.fit.core.reference.DummyPushCorrelationRule;
import org.junit.jupiter.api.Test;

public class PolicyITCase extends AbstractITCase {

    private InboundPolicyTO buildInboundPolicyTO() throws IOException {
        ImplementationTO corrRule = null;
        try {
            corrRule = IMPLEMENTATION_SERVICE.read(IdMImplementationType.INBOUND_CORRELATION_RULE, "TestPullRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPullRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(IdMImplementationType.INBOUND_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPullRule.groovy"), StandardCharsets.UTF_8));
                Response response = IMPLEMENTATION_SERVICE.create(corrRule);
                corrRule = IMPLEMENTATION_SERVICE.read(
                        corrRule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        InboundPolicyTO policy = new InboundPolicyTO();
        policy.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());
        policy.setName("Pull policy");

        return policy;
    }

    private PushPolicyTO buildPushPolicyTO() throws IOException {
        ImplementationTO corrRule = null;
        try {
            corrRule = IMPLEMENTATION_SERVICE.read(IdMImplementationType.PUSH_CORRELATION_RULE, "TestPushRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPushRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(IdMImplementationType.PUSH_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPushRule.groovy"), StandardCharsets.UTF_8));
                Response response = IMPLEMENTATION_SERVICE.create(corrRule);
                corrRule = IMPLEMENTATION_SERVICE.read(
                        corrRule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        PushPolicyTO policy = new PushPolicyTO();
        policy.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());
        policy.setName("Push policy");

        return policy;
    }

    @Test
    public void listByType() {
        List<PropagationPolicyTO> propagationPolicies = POLICY_SERVICE.list(PolicyType.PROPAGATION);
        assertNotNull(propagationPolicies);
        assertFalse(propagationPolicies.isEmpty());

        List<InboundPolicyTO> pullPolicies = POLICY_SERVICE.list(PolicyType.INBOUND);
        assertNotNull(pullPolicies);
        assertFalse(pullPolicies.isEmpty());
    }

    @Test
    public void getAccountPolicy() {
        AccountPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.ACCOUNT, "06e2ed52-6966-44aa-a177-a0ca7434201f");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().isEmpty());
        assertTrue(policyTO.getUsedByRealms().contains("/odd"));
    }

    @Test
    public void getPasswordPolicy() {
        PasswordPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.PASSWORD, "986d1236-3ac5-4a19-810c-5ab21d79cba1");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_NOPROPAGATION));
        assertTrue(policyTO.getUsedByRealms().containsAll(List.of("/", "/odd", "/even")));
    }

    @Test
    public void getPropagationPolicy() {
        PropagationPolicyTO policyTO =
                POLICY_SERVICE.read(PolicyType.PROPAGATION, "89d322db-9878-420c-b49c-67be13df9a12");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_DBSCRIPTED));
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getInboundPolicy() {
        InboundPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.INBOUND, "66691e96-285f-4464-bc19-e68384ea4c85");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAuthPolicy() {
        AuthPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.AUTH, "659b9906-4b6e-4bc0-aca0-6809dff346d4");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAccessPolicy() {
        AccessPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.ACCESS, "419935c7-deb3-40b3-8a9a-683037e523a2");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAttributeReleasePolicy() {
        AttrReleasePolicyTO policyTO =
                POLICY_SERVICE.read(PolicyType.ATTR_RELEASE, "319935c7-deb3-40b3-8a9a-683037e523a2");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void create() throws IOException {
        PropagationPolicyTO propagationPolicyTO = new PropagationPolicyTO();
        propagationPolicyTO.setName("propagation policy name");
        propagationPolicyTO.setBackOffStrategy(BackOffStrategy.EXPONENTIAL);
        propagationPolicyTO = createPolicy(PolicyType.PROPAGATION, propagationPolicyTO);
        assertNotNull(propagationPolicyTO);
        assertEquals(3, propagationPolicyTO.getMaxAttempts());
        assertEquals(BackOffStrategy.EXPONENTIAL.getDefaultBackOffParams(), propagationPolicyTO.getBackOffParams());

        InboundPolicyTO inboundPolicyTO = createPolicy(PolicyType.INBOUND, buildInboundPolicyTO());
        assertNotNull(inboundPolicyTO);
        assertEquals("TestPullRule", inboundPolicyTO.getCorrelationRules().get(AnyTypeKind.USER.name()));

        PushPolicyTO pushPolicyTO = createPolicy(PolicyType.PUSH, buildPushPolicyTO());
        assertNotNull(pushPolicyTO);
        assertEquals("TestPushRule", pushPolicyTO.getCorrelationRules().get(AnyTypeKind.USER.name()));

        AuthPolicyTO authPolicyTO = createPolicy(PolicyType.AUTH, buildAuthPolicyTO("LdapAuthentication1"));
        assertNotNull(authPolicyTO);
        assertEquals("Test Authentication policy", authPolicyTO.getName());

        AccessPolicyTO accessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());
        assertNotNull(accessPolicyTO);
        assertEquals("Test Access policy", accessPolicyTO.getName());

        AttrReleasePolicyTO attrReleasePolicyTO = createPolicy(PolicyType.ATTR_RELEASE, buildAttrReleasePolicyTO());
        assertNotNull(attrReleasePolicyTO);
        assertEquals("Test Attribute Release policy", attrReleasePolicyTO.getName());

        TicketExpirationPolicyTO ticketExpirationPolicyTO =
                createPolicy(PolicyType.TICKET_EXPIRATION, buildTicketExpirationPolicyTO());
        assertNotNull(ticketExpirationPolicyTO);
        assertEquals("Test Ticket Expiration policy", ticketExpirationPolicyTO.getName());
    }

    @Test
    public void updatePasswordPolicy() {
        PasswordPolicyTO globalPolicy = POLICY_SERVICE.read(
                PolicyType.PASSWORD, "ce93fcda-dc3a-4369-a7b0-a6108c261c85");

        PasswordPolicyTO policy = SerializationUtils.clone(globalPolicy);
        policy.setName("A simple password policy");

        // create a new password policy using the former as a template
        policy = createPolicy(PolicyType.PASSWORD, policy);
        assertNotNull(policy);
        assertNotEquals("ce93fcda-dc3a-4369-a7b0-a6108c261c85", policy.getKey());

        ImplementationTO rule = IMPLEMENTATION_SERVICE.read(
                IdRepoImplementationType.PASSWORD_RULE, policy.getRules().getFirst());
        assertNotNull(rule);

        DefaultPasswordRuleConf ruleConf = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        ruleConf.setMaxLength(22);
        rule.setBody(POJOHelper.serialize(ruleConf));

        // update new password policy
        POLICY_SERVICE.update(PolicyType.PASSWORD, policy);
        policy = POLICY_SERVICE.read(PolicyType.PASSWORD, policy.getKey());
        assertNotNull(policy);

        ruleConf = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        assertEquals(22, ruleConf.getMaxLength());
        assertEquals(8, ruleConf.getMinLength());
    }

    @Test
    public void updateAuthPolicy() {
        AuthPolicyTO newAuthPolicyTO = buildAuthPolicyTO("LdapAuthentication1");
        assertNotNull(newAuthPolicyTO);
        newAuthPolicyTO = createPolicy(PolicyType.AUTH, newAuthPolicyTO);

        DefaultAuthPolicyConf authPolicyConf = (DefaultAuthPolicyConf) newAuthPolicyTO.getConf();
        authPolicyConf.getAuthModules().add("LdapAuthentication");

        // update new authentication policy
        POLICY_SERVICE.update(PolicyType.AUTH, newAuthPolicyTO);
        newAuthPolicyTO = POLICY_SERVICE.read(PolicyType.AUTH, newAuthPolicyTO.getKey());
        assertNotNull(newAuthPolicyTO);

        authPolicyConf = (DefaultAuthPolicyConf) newAuthPolicyTO.getConf();
        assertNotNull(authPolicyConf);
        assertEquals(2, authPolicyConf.getAuthModules().size());
        assertTrue(authPolicyConf.getAuthModules().contains("LdapAuthentication"));
    }

    @Test
    public void updateAccessPolicy() {
        AccessPolicyTO newAccessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());
        assertNotNull(newAccessPolicyTO);

        DefaultAccessPolicyConf accessPolicyConf = (DefaultAccessPolicyConf) newAccessPolicyTO.getConf();
        accessPolicyConf.getRequiredAttrs().put("ou", "test");
        accessPolicyConf.getRequiredAttrs().remove("cn");
        accessPolicyConf.getRequiredAttrs().put("cn", "admin,Admin");

        // update new authentication policy
        POLICY_SERVICE.update(PolicyType.ACCESS, newAccessPolicyTO);
        newAccessPolicyTO = POLICY_SERVICE.read(PolicyType.ACCESS, newAccessPolicyTO.getKey());
        assertNotNull(newAccessPolicyTO);

        accessPolicyConf = (DefaultAccessPolicyConf) newAccessPolicyTO.getConf();
        assertEquals(2, accessPolicyConf.getRequiredAttrs().size());
        assertTrue(accessPolicyConf.getRequiredAttrs().containsKey("cn"));
        assertTrue(accessPolicyConf.getRequiredAttrs().containsKey("ou"));
    }

    @Test
    public void updateAttrReleasePolicy() {
        AttrReleasePolicyTO newPolicyTO = createPolicy(PolicyType.ATTR_RELEASE, buildAttrReleasePolicyTO());
        assertNotNull(newPolicyTO);

        DefaultAttrReleasePolicyConf policyConf = (DefaultAttrReleasePolicyConf) newPolicyTO.getConf();
        policyConf.getAllowedAttrs().add("postalCode");

        // update new policy
        POLICY_SERVICE.update(PolicyType.ATTR_RELEASE, newPolicyTO);
        newPolicyTO = POLICY_SERVICE.read(PolicyType.ATTR_RELEASE, newPolicyTO.getKey());
        assertNotNull(newPolicyTO);

        policyConf = (DefaultAttrReleasePolicyConf) newPolicyTO.getConf();
        assertEquals(3, policyConf.getAllowedAttrs().size());
        assertTrue(policyConf.getAllowedAttrs().contains("cn"));
        assertTrue(policyConf.getAllowedAttrs().contains("postalCode"));
        assertTrue(policyConf.getAllowedAttrs().contains("givenName"));
        assertTrue(policyConf.getIncludeOnlyAttrs().contains("cn"));
    }

    @Test
    public void updateTicketExpirationPolicy() {
        TicketExpirationPolicyTO newPolicyTO =
                createPolicy(PolicyType.TICKET_EXPIRATION, buildTicketExpirationPolicyTO());
        assertNotNull(newPolicyTO);

        DefaultTicketExpirationPolicyConf policyConf = (DefaultTicketExpirationPolicyConf) newPolicyTO.getConf();
        policyConf.getStConf().setNumberOfUses(2);

        // update new policy
        POLICY_SERVICE.update(PolicyType.TICKET_EXPIRATION, newPolicyTO);
        newPolicyTO = POLICY_SERVICE.read(PolicyType.TICKET_EXPIRATION, newPolicyTO.getKey());
        assertNotNull(newPolicyTO);

        policyConf = (DefaultTicketExpirationPolicyConf) newPolicyTO.getConf();
        assertEquals(2, policyConf.getStConf().getNumberOfUses());
    }

    @Test
    public void delete() throws IOException {
        InboundPolicyTO policy = buildInboundPolicyTO();

        InboundPolicyTO policyTO = createPolicy(PolicyType.INBOUND, policy);
        assertNotNull(policyTO);

        POLICY_SERVICE.delete(PolicyType.INBOUND, policyTO.getKey());

        try {
            POLICY_SERVICE.read(PolicyType.INBOUND, policyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        AuthPolicyTO authPolicy = buildAuthPolicyTO("LdapAuthentication1");

        AuthPolicyTO authPolicyTO = createPolicy(PolicyType.AUTH, authPolicy);
        assertNotNull(authPolicyTO);

        POLICY_SERVICE.delete(PolicyType.AUTH, authPolicyTO.getKey());

        try {
            POLICY_SERVICE.read(PolicyType.AUTH, authPolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        AccessPolicyTO accessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());
        assertNotNull(accessPolicyTO);

        POLICY_SERVICE.delete(PolicyType.ACCESS, accessPolicyTO.getKey());

        try {
            POLICY_SERVICE.read(PolicyType.ACCESS, accessPolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        AttrReleasePolicyTO attrReleasePolicyTO = createPolicy(PolicyType.ATTR_RELEASE, buildAttrReleasePolicyTO());
        assertNotNull(attrReleasePolicyTO);

        POLICY_SERVICE.delete(PolicyType.ATTR_RELEASE, attrReleasePolicyTO.getKey());

        try {
            POLICY_SERVICE.read(PolicyType.ATTR_RELEASE, attrReleasePolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        TicketExpirationPolicyTO ticketExpirationPolicyTO =
                createPolicy(PolicyType.TICKET_EXPIRATION, buildTicketExpirationPolicyTO());
        assertNotNull(ticketExpirationPolicyTO);

        POLICY_SERVICE.delete(PolicyType.TICKET_EXPIRATION, ticketExpirationPolicyTO.getKey());

        try {
            POLICY_SERVICE.read(PolicyType.TICKET_EXPIRATION, ticketExpirationPolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getInboundCorrelationRuleJavaClasses() {
        Set<String> classes = ANONYMOUS_CLIENT.platform().
                getJavaImplInfo(IdMImplementationType.INBOUND_CORRELATION_RULE).get().getClasses();
        assertEquals(1, classes.size());
        assertEquals(DummyInboundCorrelationRule.class.getName(), classes.iterator().next());
    }

    @Test
    public void getPushCorrelationRuleJavaClasses() {
        Set<String> classes = ANONYMOUS_CLIENT.platform().
                getJavaImplInfo(IdMImplementationType.PUSH_CORRELATION_RULE).get().getClasses();
        assertEquals(1, classes.size());
        assertEquals(DummyPushCorrelationRule.class.getName(), classes.iterator().next());
    }

    @Test
    public void issueSYNCOPE553() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setName("SYNCOPE553");

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = IMPLEMENTATION_SERVICE.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(PolicyType.ACCOUNT, policy);
        assertNotNull(policy);
    }

    @Test
    public void issueSYNCOPE682() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setName("SYNCOPE682");

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = IMPLEMENTATION_SERVICE.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(PolicyType.ACCOUNT, policy);
        assertTrue(policy.getPassthroughResources().isEmpty());

        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        String existingAP = ldap.getAccountPolicy();
        try {
            ldap.setAccountPolicy(policy.getKey());
            RESOURCE_SERVICE.update(ldap);

            ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
            assertEquals(policy.getKey(), ldap.getAccountPolicy());

            policy = POLICY_SERVICE.read(PolicyType.ACCOUNT, policy.getKey());
            assertEquals(List.of(RESOURCE_NAME_LDAP), policy.getPassthroughResources());
        } finally {
            ldap.setAccountPolicy(existingAP);
            RESOURCE_SERVICE.update(ldap);
        }
    }
}
