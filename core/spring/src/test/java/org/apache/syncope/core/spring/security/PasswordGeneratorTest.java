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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.apache.syncope.core.spring.implementation.ImplementationManagerTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
public class PasswordGeneratorTest {

    private final DefaultPasswordGenerator passwordGenerator = new DefaultPasswordGenerator();

    @Test
    public void digit() {
        DefaultPasswordRuleConf pwdRuleConf = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setDigit(1);
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf));

        String generatedPassword = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));

        assertTrue(generatedPassword.chars().anyMatch(Character::isDigit));
    }

    @Test
    public void alphabetical() {
        DefaultPasswordRuleConf pwdRuleConf = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setAlphabetical(1);
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf));

        String generatedPassword = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));

        assertTrue(generatedPassword.chars().anyMatch(Character::isAlphabetic));
    }

    @Test
    public void lowercase() {
        DefaultPasswordRuleConf pwdRuleConf = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setLowercase(1);
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf));

        String generatedPassword = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));

        assertTrue(generatedPassword.chars().anyMatch(Character::isLowerCase));
    }

    @Test
    public void uppercase() {
        DefaultPasswordRuleConf pwdRuleConf = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setUppercase(1);
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf));

        String generatedPassword = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));

        assertTrue(generatedPassword.chars().anyMatch(Character::isUpperCase));
    }

    @Test
    public void special() {
        DefaultPasswordRuleConf pwdRuleConf = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setSpecial(1);
        pwdRuleConf.getSpecialChars().add('@');
        pwdRuleConf.getSpecialChars().add('!');
        pwdRuleConf.getSpecialChars().add('%');
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf));

        String generatedPassword = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));

        assertTrue(generatedPassword.chars().anyMatch(c -> '@' == c || '!' == c || '%' == c));
    }

    @Test
    public void issueSYNCOPE678() {
        String password = passwordGenerator.generate(List.of());
        assertNotNull(password);

        DefaultPasswordRuleConf pwdRuleConf1 = ImplementationManagerTest.createBaseDefaultPasswordRuleConf();
        pwdRuleConf1.setMinLength(0);
        TestImplementation passwordRule = new TestImplementation();
        passwordRule.setBody(POJOHelper.serialize(pwdRuleConf1));

        password = passwordGenerator.generate(List.of(new TestPasswordPolicy(passwordRule)));
        assertNotNull(password);
    }
}
