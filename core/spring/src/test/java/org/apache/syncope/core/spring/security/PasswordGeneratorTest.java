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
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.apache.syncope.core.spring.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.spring.policy.PolicyPattern;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
public class PasswordGeneratorTest {

    private final DefaultPasswordGenerator passwordGenerator = new DefaultPasswordGenerator();

    private static DefaultPasswordRuleConf createBaseDefaultPasswordRuleConf() {
        DefaultPasswordRuleConf baseDefaultPasswordRuleConf = new DefaultPasswordRuleConf();
        baseDefaultPasswordRuleConf.setAlphanumericRequired(false);
        baseDefaultPasswordRuleConf.setDigitRequired(false);
        baseDefaultPasswordRuleConf.setLowercaseRequired(false);
        baseDefaultPasswordRuleConf.setMaxLength(1000);
        baseDefaultPasswordRuleConf.setMinLength(8);
        baseDefaultPasswordRuleConf.setMustEndWithAlpha(false);
        baseDefaultPasswordRuleConf.setMustEndWithDigit(false);
        baseDefaultPasswordRuleConf.setMustEndWithNonAlpha(false);
        baseDefaultPasswordRuleConf.setMustStartWithAlpha(false);
        baseDefaultPasswordRuleConf.setMustStartWithDigit(false);
        baseDefaultPasswordRuleConf.setMustStartWithNonAlpha(false);
        baseDefaultPasswordRuleConf.setMustntEndWithAlpha(false);
        baseDefaultPasswordRuleConf.setMustntEndWithDigit(false);
        baseDefaultPasswordRuleConf.setMustntEndWithNonAlpha(false);
        baseDefaultPasswordRuleConf.setMustntStartWithAlpha(false);
        baseDefaultPasswordRuleConf.setMustntStartWithDigit(false);
        baseDefaultPasswordRuleConf.setMustntStartWithNonAlpha(false);
        baseDefaultPasswordRuleConf.setNonAlphanumericRequired(false);
        baseDefaultPasswordRuleConf.setUppercaseRequired(false);
        return baseDefaultPasswordRuleConf;
    }

    @Test
    public void startEndWithDigit() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf1 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf1.setMustStartWithDigit(true);
        TestImplementation passwordRule1 = new TestImplementation();
        passwordRule1.setBody(POJOHelper.serialize(pwdRuleConf1));
        TestPasswordPolicy policy1 = new TestPasswordPolicy();
        policy1.add(passwordRule1);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithDigit(true);
        TestImplementation passwordRule2 = new TestImplementation();
        passwordRule2.setBody(POJOHelper.serialize(pwdRuleConf2));
        TestPasswordPolicy policy2 = new TestPasswordPolicy();
        policy2.add(passwordRule2);

        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(policy1);
        policies.add(policy2);
        String generatedPassword = passwordGenerator.generate(policies);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isDigit(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void startWithDigitAndWithAlpha() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf1 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf1.setMustStartWithDigit(true);
        TestImplementation passwordRule1 = new TestImplementation();
        passwordRule1.setBody(POJOHelper.serialize(pwdRuleConf1));
        TestPasswordPolicy policy1 = new TestPasswordPolicy();
        policy1.add(passwordRule1);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithAlpha(true);
        TestImplementation passwordRule2 = new TestImplementation();
        passwordRule2.setBody(POJOHelper.serialize(pwdRuleConf2));
        TestPasswordPolicy policy2 = new TestPasswordPolicy();
        policy2.add(passwordRule2);

        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(policy1);
        policies.add(policy2);
        String generatedPassword = passwordGenerator.generate(policies);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void passwordWithNonAlpha() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf1 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf1.setNonAlphanumericRequired(true);
        TestImplementation passwordRule1 = new TestImplementation();
        passwordRule1.setBody(POJOHelper.serialize(pwdRuleConf1));
        TestPasswordPolicy policy1 = new TestPasswordPolicy();
        policy1.add(passwordRule1);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithAlpha(true);
        TestImplementation passwordRule2 = new TestImplementation();
        passwordRule2.setBody(POJOHelper.serialize(pwdRuleConf2));
        TestPasswordPolicy policy2 = new TestPasswordPolicy();
        policy2.add(passwordRule2);

        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(policy1);
        policies.add(policy2);
        String generatedPassword = passwordGenerator.generate(policies);
        assertTrue(PolicyPattern.NON_ALPHANUMERIC.matcher(generatedPassword).matches());
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void incopatiblePolicies() {
        assertThrows(InvalidPasswordRuleConf.class, () -> {
            DefaultPasswordRuleConf pwdRuleConf1 = createBaseDefaultPasswordRuleConf();
            pwdRuleConf1.setMinLength(12);
            TestImplementation passwordRule1 = new TestImplementation();
            passwordRule1.setBody(POJOHelper.serialize(pwdRuleConf1));
            TestPasswordPolicy policy1 = new TestPasswordPolicy();
            policy1.add(passwordRule1);

            DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
            pwdRuleConf2.setMaxLength(10);
            TestImplementation passwordRule2 = new TestImplementation();
            passwordRule2.setBody(POJOHelper.serialize(pwdRuleConf2));
            TestPasswordPolicy policy2 = new TestPasswordPolicy();
            policy2.add(passwordRule2);

            List<PasswordPolicy> policies = new ArrayList<>();
            policies.add(policy1);
            policies.add(policy2);
            passwordGenerator.generate(policies);
        });
    }

    @Test
    public void issueSYNCOPE678() {
        String password = null;
        try {
            password = passwordGenerator.generate(Collections.<PasswordPolicy>emptyList());
        } catch (InvalidPasswordRuleConf e) {
            fail(e::getMessage);
        }
        assertNotNull(password);

        DefaultPasswordRuleConf pwdRuleConf1 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf1.setMinLength(0);
        TestImplementation passwordRule1 = new TestImplementation();
        passwordRule1.setBody(POJOHelper.serialize(pwdRuleConf1));
        TestPasswordPolicy policy1 = new TestPasswordPolicy();

        password = null;
        try {
            password = passwordGenerator.generate(Collections.<PasswordPolicy>singletonList(policy1));
        } catch (InvalidPasswordRuleConf e) {
            fail(e::getMessage);
        }
        assertNotNull(password);
    }
}
