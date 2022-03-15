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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.policy.PushPolicyTO;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.DummyPullCorrelationRule;
import org.apache.syncope.fit.core.reference.DummyPushCorrelationRule;
import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.types.BackOffStrategy;

public class PolicyITCase extends AbstractITCase {

    private PullPolicyTO buildPullPolicyTO() throws IOException {
        ImplementationTO corrRule = null;
        try {
            corrRule = implementationService.read(IdMImplementationType.PULL_CORRELATION_RULE, "TestPullRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPullRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(IdMImplementationType.PULL_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPullRule.groovy"), StandardCharsets.UTF_8));
                Response response = implementationService.create(corrRule);
                corrRule = implementationService.read(
                        corrRule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        PullPolicyTO policy = new PullPolicyTO();
        policy.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());
        policy.setName("Pull policy");

        return policy;
    }

    private PushPolicyTO buildPushPolicyTO() throws IOException {
        ImplementationTO corrRule = null;
        try {
            corrRule = implementationService.read(IdMImplementationType.PUSH_CORRELATION_RULE, "TestPushRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPushRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(IdMImplementationType.PUSH_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPushRule.groovy"), StandardCharsets.UTF_8));
                Response response = implementationService.create(corrRule);
                corrRule = implementationService.read(
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
        List<PropagationPolicyTO> propagationPolicies = policyService.list(PolicyType.PROPAGATION);
        assertNotNull(propagationPolicies);
        assertFalse(propagationPolicies.isEmpty());

        List<PullPolicyTO> pullPolicies = policyService.list(PolicyType.PULL);
        assertNotNull(pullPolicies);
        assertFalse(pullPolicies.isEmpty());
    }

    @Test
    public void getAccountPolicy() {
        AccountPolicyTO policyTO = policyService.read(PolicyType.ACCOUNT, "06e2ed52-6966-44aa-a177-a0ca7434201f");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().isEmpty());
        assertTrue(policyTO.getUsedByRealms().contains("/odd"));
    }

    @Test
    public void getPasswordPolicy() {
        PasswordPolicyTO policyTO = policyService.read(PolicyType.PASSWORD, "986d1236-3ac5-4a19-810c-5ab21d79cba1");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_NOPROPAGATION));
        assertTrue(policyTO.getUsedByRealms().containsAll(List.of("/", "/odd", "/even")));
    }

    @Test
    public void getPropagationPolicy() {
        PropagationPolicyTO policyTO =
                policyService.read(PolicyType.PROPAGATION, "89d322db-9878-420c-b49c-67be13df9a12");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_DBSCRIPTED));
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getPullPolicy() {
        PullPolicyTO policyTO = policyService.read(PolicyType.PULL, "66691e96-285f-4464-bc19-e68384ea4c85");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAuthPolicy() {
        AuthPolicyTO policyTO = policyService.read(PolicyType.AUTH, "659b9906-4b6e-4bc0-aca0-6809dff346d4");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAccessPolicy() {
        AccessPolicyTO policyTO = policyService.read(PolicyType.ACCESS, "419935c7-deb3-40b3-8a9a-683037e523a2");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void getAttributeReleasePolicy() {
        AttrReleasePolicyTO policyTO =
                policyService.read(PolicyType.ATTR_RELEASE, "319935c7-deb3-40b3-8a9a-683037e523a2");

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

        PullPolicyTO pullPolicyTO = createPolicy(PolicyType.PULL, buildPullPolicyTO());
        assertNotNull(pullPolicyTO);
        assertEquals("TestPullRule", pullPolicyTO.getCorrelationRules().get(AnyTypeKind.USER.name()));

        PushPolicyTO pushPolicyTO = createPolicy(PolicyType.PUSH, buildPushPolicyTO());
        assertNotNull(pushPolicyTO);
        assertEquals("TestPushRule", pushPolicyTO.getCorrelationRules().get(AnyTypeKind.USER.name()));

        AuthPolicyTO authPolicyTO = createPolicy(PolicyType.AUTH, buildAuthPolicyTO("LdapAuthentication1"));
        assertNotNull(authPolicyTO);
        assertEquals("Test Authentication policy", authPolicyTO.getName());

        AccessPolicyTO accessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());
        assertNotNull(accessPolicyTO);
        assertEquals("Test Access policy", accessPolicyTO.getName());
    }

    @Test
    public void updatePasswordPolicy() {
        PasswordPolicyTO globalPolicy = policyService.read(PolicyType.PASSWORD, "ce93fcda-dc3a-4369-a7b0-a6108c261c85");

        PasswordPolicyTO policy = SerializationUtils.clone(globalPolicy);
        policy.setName("A simple password policy");

        // create a new password policy using the former as a template
        policy = createPolicy(PolicyType.PASSWORD, policy);
        assertNotNull(policy);
        assertNotEquals("ce93fcda-dc3a-4369-a7b0-a6108c261c85", policy.getKey());

        ImplementationTO rule = implementationService.read(
                IdRepoImplementationType.PASSWORD_RULE, policy.getRules().get(0));
        assertNotNull(rule);

        DefaultPasswordRuleConf ruleConf = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        ruleConf.setMaxLength(22);
        rule.setBody(POJOHelper.serialize(ruleConf));

        // update new password policy
        policyService.update(PolicyType.PASSWORD, policy);
        policy = policyService.read(PolicyType.PASSWORD, policy.getKey());
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
        policyService.update(PolicyType.AUTH, newAuthPolicyTO);
        newAuthPolicyTO = policyService.read(PolicyType.AUTH, newAuthPolicyTO.getKey());
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
        accessPolicyConf.getRequiredAttrs().add(new Attr.Builder("ou").value("test").build());
        accessPolicyConf.getRequiredAttrs().removeIf(attr -> "cn".equals(attr.getSchema()));
        accessPolicyConf.getRequiredAttrs().add(new Attr.Builder("cn").values("admin", "Admin").build());

        // update new authentication policy
        policyService.update(PolicyType.ACCESS, newAccessPolicyTO);
        newAccessPolicyTO = policyService.read(PolicyType.ACCESS, newAccessPolicyTO.getKey());
        assertNotNull(newAccessPolicyTO);

        accessPolicyConf = (DefaultAccessPolicyConf) newAccessPolicyTO.getConf();
        assertEquals(2, accessPolicyConf.getRequiredAttrs().size());
        assertTrue(accessPolicyConf.getRequiredAttrs().stream().anyMatch(attr -> "cn".equals(attr.getSchema())));
        assertTrue(accessPolicyConf.getRequiredAttrs().stream().anyMatch(attr -> "ou".equals(attr.getSchema())));
    }

    @Test
    public void updateAttrReleasePolicy() {
        AttrReleasePolicyTO newPolicyTO = createPolicy(PolicyType.ATTR_RELEASE, buildAttrReleasePolicyTO());
        assertNotNull(newPolicyTO);

        DefaultAttrReleasePolicyConf policyConf = (DefaultAttrReleasePolicyConf) newPolicyTO.getConf();
        policyConf.getAllowedAttrs().add("postalCode");

        // update new policy
        policyService.update(PolicyType.ATTR_RELEASE, newPolicyTO);
        newPolicyTO = policyService.read(PolicyType.ATTR_RELEASE, newPolicyTO.getKey());
        assertNotNull(newPolicyTO);

        policyConf = (DefaultAttrReleasePolicyConf) newPolicyTO.getConf();
        assertEquals(3, policyConf.getAllowedAttrs().size());
        assertTrue(policyConf.getAllowedAttrs().contains("cn"));
        assertTrue(policyConf.getAllowedAttrs().contains("postalCode"));
        assertTrue(policyConf.getAllowedAttrs().contains("givenName"));
        assertTrue(policyConf.getIncludeOnlyAttrs().contains("cn"));
    }

    @Test
    public void delete() throws IOException {
        PullPolicyTO policy = buildPullPolicyTO();

        PullPolicyTO policyTO = createPolicy(PolicyType.PULL, policy);
        assertNotNull(policyTO);

        policyService.delete(PolicyType.PULL, policyTO.getKey());

        try {
            policyService.read(PolicyType.PULL, policyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        AuthPolicyTO authPolicy = buildAuthPolicyTO("LdapAuthentication1");

        AuthPolicyTO authPolicyTO = createPolicy(PolicyType.AUTH, authPolicy);
        assertNotNull(authPolicyTO);

        policyService.delete(PolicyType.AUTH, authPolicyTO.getKey());

        try {
            policyService.read(PolicyType.AUTH, authPolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }

        AccessPolicyTO accessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());
        assertNotNull(accessPolicyTO);

        policyService.delete(PolicyType.ACCESS, accessPolicyTO.getKey());

        try {
            policyService.read(PolicyType.ACCESS, accessPolicyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getPullCorrelationRuleJavaClasses() {
        Set<String> classes = adminClient.platform().
                getJavaImplInfo(IdMImplementationType.PULL_CORRELATION_RULE).get().getClasses();
        assertEquals(1, classes.size());
        assertEquals(DummyPullCorrelationRule.class.getName(), classes.iterator().next());
    }

    @Test
    public void getPushCorrelationRuleJavaClasses() {
        Set<String> classes = adminClient.platform().
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
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(PolicyType.ACCOUNT, policy);
        assertNotNull(policy);
    }

    @Test
    public void issueSYNCOPE682() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setName("SYNCOPE682");
        policy.getPassthroughResources().add(RESOURCE_NAME_LDAP);

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(PolicyType.ACCOUNT, policy);
        assertNotNull(policy);
    }
}
