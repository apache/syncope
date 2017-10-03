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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class PolicyITCase extends AbstractITCase {

    private PullPolicyTO buildPullPolicyTO() throws IOException {
        ImplementationTO corrRule = null;
        try {
            corrRule = implementationService.read("TestPullRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPullRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(ImplementationType.PULL_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPullRule.groovy"), StandardCharsets.UTF_8));
                Response response = implementationService.create(corrRule);
                corrRule = getObject(response.getLocation(), ImplementationService.class, ImplementationTO.class);
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        PullPolicySpec spec = new PullPolicySpec();
        spec.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());

        PullPolicyTO policy = new PullPolicyTO();
        policy.setSpecification(spec);
        policy.setDescription("Pull policy");

        return policy;
    }

    @Test
    public void listByType() {
        List<PullPolicyTO> policyTOs = policyService.list(PolicyType.PULL);

        assertNotNull(policyTOs);
        assertFalse(policyTOs.isEmpty());
    }

    @Test
    public void getAccountPolicy() {
        AccountPolicyTO policyTO = policyService.read("06e2ed52-6966-44aa-a177-a0ca7434201f");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().isEmpty());
        assertTrue(policyTO.getUsedByRealms().contains("/odd"));
    }

    @Test
    public void getPasswordPolicy() {
        PasswordPolicyTO policyTO = policyService.read("986d1236-3ac5-4a19-810c-5ab21d79cba1");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByResources().contains(RESOURCE_NAME_NOPROPAGATION));
        assertTrue(policyTO.getUsedByRealms().containsAll(Arrays.asList("/", "/odd", "/even")));
    }

    @Test
    public void getPullPolicy() {
        PullPolicyTO policyTO = policyService.read("66691e96-285f-4464-bc19-e68384ea4c85");

        assertNotNull(policyTO);
        assertTrue(policyTO.getUsedByRealms().isEmpty());
    }

    @Test
    public void createMissingDescription() {
        PullPolicyTO policy = new PullPolicyTO();
        policy.setSpecification(new PullPolicySpec());

        try {
            createPolicy(policy);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPolicy, e.getType());
        }
    }

    @Test
    public void create() throws IOException {
        PullPolicyTO policy = buildPullPolicyTO();

        PullPolicyTO policyTO = createPolicy(policy);

        assertNotNull(policyTO);
        assertEquals("TestPullRule", policyTO.getSpecification().getCorrelationRules().get(AnyTypeKind.USER.name()));
    }

    @Test
    public void update() {
        PasswordPolicyTO globalPolicy = policyService.read("ce93fcda-dc3a-4369-a7b0-a6108c261c85");

        PasswordPolicyTO policy = SerializationUtils.clone(globalPolicy);
        policy.setDescription("A simple password policy");

        // create a new password policy using the former as a template
        policy = createPolicy(policy);
        assertNotNull(policy);
        assertNotEquals("ce93fcda-dc3a-4369-a7b0-a6108c261c85", policy.getKey());

        ImplementationTO rule = implementationService.read(policy.getRules().get(0));
        assertNotNull(rule);

        DefaultPasswordRuleConf ruleConf = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        ruleConf.setMaxLength(22);
        rule.setBody(POJOHelper.serialize(ruleConf));

        // update new password policy
        policyService.update(policy);
        policy = policyService.read(policy.getKey());
        assertNotNull(policy);

        ruleConf = POJOHelper.deserialize(rule.getBody(), DefaultPasswordRuleConf.class);
        assertEquals(22, ruleConf.getMaxLength());
        assertEquals(8, ruleConf.getMinLength());
    }

    @Test
    public void delete() throws IOException {
        PullPolicyTO policy = buildPullPolicyTO();

        PullPolicyTO policyTO = createPolicy(policy);
        assertNotNull(policyTO);

        policyService.delete(policyTO.getKey());

        try {
            policyService.read(policyTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void getPullCorrelationRuleJavaClasses() {
        assertTrue(syncopeService.platform().
                getJavaImplInfo(ImplementationType.PULL_CORRELATION_RULE).get().getClasses().isEmpty());
    }

    @Test
    public void issueSYNCOPE553() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setDescription("SYNCOPE553");

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(ImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(policy);
        assertNotNull(policy);
    }

    @Test
    public void issueSYNCOPE682() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setDescription("SYNCOPE682");
        policy.getPassthroughResources().add(RESOURCE_NAME_LDAP);

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(ImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        policy.getRules().add(rule.getKey());

        policy = createPolicy(policy);
        assertNotNull(policy);
    }
}
