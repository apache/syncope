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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.PolicyPattern;
import org.junit.Test;

public class PasswordGeneratorTest {

    private final DefaultPasswordGenerator passwordGenerator = new DefaultPasswordGenerator();

    private DefaultPasswordRuleConf createBaseDefaultPasswordRuleConf() {
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
        DefaultPasswordRuleConf pwdRuleConf = createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setMustStartWithDigit(true);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithDigit(true);

        List<PasswordRuleConf> ruleConfs = new ArrayList<>();
        ruleConfs.add(pwdRuleConf);
        ruleConfs.add(pwdRuleConf2);
        String generatedPassword = passwordGenerator.generate(ruleConfs);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isDigit(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void startWithDigitAndWithAlpha() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf = createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setMustStartWithDigit(true);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithAlpha(true);

        List<PasswordRuleConf> pwdRuleConfs = new ArrayList<>();
        pwdRuleConfs.add(pwdRuleConf);
        pwdRuleConfs.add(pwdRuleConf2);
        String generatedPassword = passwordGenerator.generate(pwdRuleConfs);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void passwordWithNonAlpha() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf = createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setNonAlphanumericRequired(true);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf2.setMustEndWithAlpha(true);

        List<PasswordRuleConf> pwdRuleConfs = new ArrayList<>();
        pwdRuleConfs.add(pwdRuleConf);
        pwdRuleConfs.add(pwdRuleConf2);
        String generatedPassword = passwordGenerator.generate(pwdRuleConfs);
        assertTrue(PolicyPattern.NON_ALPHANUMERIC.matcher(generatedPassword).matches());
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test(expected = InvalidPasswordRuleConf.class)
    public void incopatiblePolicies() throws InvalidPasswordRuleConf {
        DefaultPasswordRuleConf pwdRuleConf = createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setMinLength(12);

        DefaultPasswordRuleConf pwdRuleConf2 = createBaseDefaultPasswordRuleConf();
        pwdRuleConf.setMaxLength(10);

        List<PasswordRuleConf> pwdRuleConfs = new ArrayList<>();
        pwdRuleConfs.add(pwdRuleConf);
        pwdRuleConfs.add(pwdRuleConf2);
        passwordGenerator.generate(pwdRuleConfs);
    }

    @Test
    public void issueSYNCOPE678() {
        String password = null;
        try {
            password = passwordGenerator.generate(Collections.<PasswordRuleConf>emptyList());
        } catch (InvalidPasswordRuleConf e) {
            fail(e.getMessage());
        }
        assertNotNull(password);

        DefaultPasswordRuleConf ppSpec = createBaseDefaultPasswordRuleConf();
        ppSpec.setMinLength(0);
        password = null;
        try {
            password = passwordGenerator.generate(Collections.<PasswordRuleConf>singletonList(ppSpec));
        } catch (InvalidPasswordRuleConf e) {
            fail(e.getMessage());
        }
        assertNotNull(password);
    }
}
