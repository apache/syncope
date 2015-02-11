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
package org.apache.syncope.core.misc.security;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.core.misc.policy.InvalidPasswordPolicySpecException;
import org.apache.syncope.core.misc.policy.PolicyPattern;
import org.junit.Test;

public class PasswordGeneratorTest {

    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Test
    public void startEndWithDigit()
            throws InvalidPasswordPolicySpecException {

        PasswordPolicySpec passwordPolicySpec = createBasePasswordPolicySpec();
        passwordPolicySpec.setMustStartWithDigit(true);

        PasswordPolicySpec passwordPolicySpec2 = createBasePasswordPolicySpec();
        passwordPolicySpec.setMustEndWithDigit(true);
        List<PasswordPolicySpec> passwordPolicySpecs = new ArrayList<>();
        passwordPolicySpecs.add(passwordPolicySpec);
        passwordPolicySpecs.add(passwordPolicySpec2);
        String generatedPassword = passwordGenerator.generate(passwordPolicySpecs);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isDigit(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void startWithDigitAndWithAlpha()
            throws InvalidPasswordPolicySpecException {

        PasswordPolicySpec passwordPolicySpec = createBasePasswordPolicySpec();
        passwordPolicySpec.setMustStartWithDigit(true);

        PasswordPolicySpec passwordPolicySpec2 = createBasePasswordPolicySpec();
        passwordPolicySpec.setMustEndWithAlpha(true);
        List<PasswordPolicySpec> passwordPolicySpecs = new ArrayList<>();
        passwordPolicySpecs.add(passwordPolicySpec);
        passwordPolicySpecs.add(passwordPolicySpec2);
        String generatedPassword = passwordGenerator.generate(passwordPolicySpecs);
        assertTrue(Character.isDigit(generatedPassword.charAt(0)));
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test
    public void passwordWithNonAlpha()
            throws InvalidPasswordPolicySpecException {

        PasswordPolicySpec passwordPolicySpec = createBasePasswordPolicySpec();
        passwordPolicySpec.setNonAlphanumericRequired(true);

        PasswordPolicySpec passwordPolicySpec2 = createBasePasswordPolicySpec();
        passwordPolicySpec.setMustEndWithAlpha(true);
        List<PasswordPolicySpec> passwordPolicySpecs = new ArrayList<>();
        passwordPolicySpecs.add(passwordPolicySpec);
        passwordPolicySpecs.add(passwordPolicySpec2);
        String generatedPassword = passwordGenerator.generate(passwordPolicySpecs);
        assertTrue(PolicyPattern.NON_ALPHANUMERIC.matcher(generatedPassword).matches());
        assertTrue(Character.isLetter(generatedPassword.charAt(generatedPassword.length() - 1)));
    }

    @Test(expected = InvalidPasswordPolicySpecException.class)
    public void incopatiblePolicies()
            throws InvalidPasswordPolicySpecException {

        PasswordPolicySpec passwordPolicySpec = createBasePasswordPolicySpec();
        passwordPolicySpec.setMinLength(12);

        PasswordPolicySpec passwordPolicySpec2 = createBasePasswordPolicySpec();
        passwordPolicySpec.setMaxLength(10);

        List<PasswordPolicySpec> passwordPolicySpecs = new ArrayList<>();
        passwordPolicySpecs.add(passwordPolicySpec);
        passwordPolicySpecs.add(passwordPolicySpec2);
        passwordGenerator.generate(passwordPolicySpecs);
    }

    private PasswordPolicySpec createBasePasswordPolicySpec() {
        PasswordPolicySpec basePasswordPolicySpec = new PasswordPolicySpec();
        basePasswordPolicySpec.setAlphanumericRequired(false);
        basePasswordPolicySpec.setDigitRequired(false);
        basePasswordPolicySpec.setLowercaseRequired(false);
        basePasswordPolicySpec.setMaxLength(1000);
        basePasswordPolicySpec.setMinLength(8);
        basePasswordPolicySpec.setMustEndWithAlpha(false);
        basePasswordPolicySpec.setMustEndWithDigit(false);
        basePasswordPolicySpec.setMustEndWithNonAlpha(false);
        basePasswordPolicySpec.setMustStartWithAlpha(false);
        basePasswordPolicySpec.setMustStartWithDigit(false);
        basePasswordPolicySpec.setMustStartWithNonAlpha(false);
        basePasswordPolicySpec.setMustntEndWithAlpha(false);
        basePasswordPolicySpec.setMustntEndWithDigit(false);
        basePasswordPolicySpec.setMustntEndWithNonAlpha(false);
        basePasswordPolicySpec.setMustntStartWithAlpha(false);
        basePasswordPolicySpec.setMustntStartWithDigit(false);
        basePasswordPolicySpec.setMustntStartWithNonAlpha(false);
        basePasswordPolicySpec.setNonAlphanumericRequired(false);
        basePasswordPolicySpec.setUppercaseRequired(false);
        return basePasswordPolicySpec;
    }
}
