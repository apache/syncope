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
package org.apache.syncope.core.connid;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.policy.PolicyPattern;
import org.apache.syncope.core.util.InvalidPasswordPolicySpecException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generate random passwords according to given policies.
 *
 * @see PasswordPolicy
 */
@Component
public class PasswordGenerator {

    private static final String[] SPECIAL_CHARS = {"", "!", "£", "%", "&", "(", ")", "?", "#", "_", "$"};

    @Autowired
    private PolicyDAO policyDAO;

    public String generate(final List<PasswordPolicySpec> ppSpecs)
            throws InvalidPasswordPolicySpecException {

        PasswordPolicySpec policySpec = merge(ppSpecs);

        check(policySpec);

        return generate(policySpec);
    }

    public String generate(final SyncopeUser user)
            throws InvalidPasswordPolicySpecException {

        List<PasswordPolicySpec> ppSpecs = new ArrayList<PasswordPolicySpec>();

        PasswordPolicy globalPP = policyDAO.getGlobalPasswordPolicy();
        if (globalPP != null && globalPP.getSpecification() != null) {
            ppSpecs.add(globalPP.<PasswordPolicySpec>getSpecification());
        }

        for (SyncopeRole role : user.getRoles()) {
            if (role.getPasswordPolicy() != null && role.getPasswordPolicy().getSpecification() != null) {
                ppSpecs.add(role.getPasswordPolicy().<PasswordPolicySpec>getSpecification());
            }
        }

        for (ExternalResource resource : user.getResources()) {
            if (resource.getPasswordPolicy() != null && resource.getPasswordPolicy().getSpecification() != null) {
                ppSpecs.add(resource.getPasswordPolicy().<PasswordPolicySpec>getSpecification());
            }
        }

        PasswordPolicySpec policySpec = merge(ppSpecs);
        check(policySpec);
        return generate(policySpec);
    }

    private PasswordPolicySpec merge(final List<PasswordPolicySpec> ppSpecs) {
        PasswordPolicySpec fpps = new PasswordPolicySpec();
        fpps.setMinLength(0);
        fpps.setMaxLength(1000);

        for (PasswordPolicySpec policySpec : ppSpecs) {
            if (policySpec.getMinLength() > fpps.getMinLength()) {
                fpps.setMinLength(policySpec.getMinLength());
            }

            if ((policySpec.getMaxLength() != 0) && ((policySpec.getMaxLength() < fpps.getMaxLength()))) {
                fpps.setMaxLength(policySpec.getMaxLength());
            }
            fpps.setPrefixesNotPermitted(policySpec.getPrefixesNotPermitted());
            fpps.setSuffixesNotPermitted(policySpec.getSuffixesNotPermitted());

            if (!fpps.isNonAlphanumericRequired()) {
                fpps.setNonAlphanumericRequired(policySpec.isNonAlphanumericRequired());
            }

            if (!fpps.isAlphanumericRequired()) {
                fpps.setAlphanumericRequired(policySpec.isAlphanumericRequired());
            }
            if (!fpps.isDigitRequired()) {
                fpps.setDigitRequired(policySpec.isDigitRequired());
            }

            if (!fpps.isLowercaseRequired()) {
                fpps.setLowercaseRequired(policySpec.isLowercaseRequired());
            }
            if (!fpps.isUppercaseRequired()) {
                fpps.setUppercaseRequired(policySpec.isUppercaseRequired());
            }
            if (!fpps.isMustStartWithDigit()) {
                fpps.setMustStartWithDigit(policySpec.isMustStartWithDigit());
            }
            if (!fpps.isMustntStartWithDigit()) {
                fpps.setMustntStartWithDigit(policySpec.isMustntStartWithDigit());
            }
            if (!fpps.isMustEndWithDigit()) {
                fpps.setMustEndWithDigit(policySpec.isMustEndWithDigit());
            }
            if (fpps.isMustntEndWithDigit()) {
                fpps.setMustntEndWithDigit(policySpec.isMustntEndWithDigit());
            }
            if (!fpps.isMustStartWithAlpha()) {
                fpps.setMustStartWithAlpha(policySpec.isMustStartWithAlpha());
            }
            if (!fpps.isMustntStartWithAlpha()) {
                fpps.setMustntStartWithAlpha(policySpec.isMustntStartWithAlpha());
            }
            if (!fpps.isMustStartWithNonAlpha()) {
                fpps.setMustStartWithNonAlpha(policySpec.isMustStartWithNonAlpha());
            }
            if (!fpps.isMustntStartWithNonAlpha()) {
                fpps.setMustntStartWithNonAlpha(policySpec.isMustntStartWithNonAlpha());
            }
            if (!fpps.isMustEndWithNonAlpha()) {
                fpps.setMustEndWithNonAlpha(policySpec.isMustEndWithNonAlpha());
            }
            if (!fpps.isMustntEndWithNonAlpha()) {
                fpps.setMustntEndWithNonAlpha(policySpec.isMustntEndWithNonAlpha());
            }
            if (!fpps.isMustEndWithAlpha()) {
                fpps.setMustEndWithAlpha(policySpec.isMustEndWithAlpha());
            }
            if (!fpps.isMustntEndWithAlpha()) {
                fpps.setMustntEndWithAlpha(policySpec.isMustntEndWithAlpha());
            }
        }
        return fpps;
    }

    private void check(final PasswordPolicySpec policySpec)
            throws InvalidPasswordPolicySpecException {

        if (policySpec.getMinLength() == 0) {
            throw new InvalidPasswordPolicySpecException("Minimum length is zero");
        }
        if (policySpec.isMustEndWithAlpha() && policySpec.isMustntEndWithAlpha()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustEndWithAlpha and mustntEndWithAlpha are both true");
        }
        if (policySpec.isMustEndWithAlpha() && policySpec.isMustEndWithDigit()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustEndWithAlpha and mustEndWithDigit are both true");
        }
        if (policySpec.isMustEndWithDigit() && policySpec.isMustntEndWithDigit()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustEndWithDigit and mustntEndWithDigit are both true");
        }
        if (policySpec.isMustEndWithNonAlpha() && policySpec.isMustntEndWithNonAlpha()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustEndWithNonAlpha and mustntEndWithNonAlpha are both true");
        }
        if (policySpec.isMustStartWithAlpha() && policySpec.isMustntStartWithAlpha()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustStartWithAlpha and mustntStartWithAlpha are both true");
        }
        if (policySpec.isMustStartWithAlpha() && policySpec.isMustStartWithDigit()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustStartWithAlpha and mustStartWithDigit are both true");
        }
        if (policySpec.isMustStartWithDigit() && policySpec.isMustntStartWithDigit()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustStartWithDigit and mustntStartWithDigit are both true");
        }
        if (policySpec.isMustStartWithNonAlpha() && policySpec.isMustntStartWithNonAlpha()) {
            throw new InvalidPasswordPolicySpecException(
                    "mustStartWithNonAlpha and mustntStartWithNonAlpha are both true");
        }
        if (policySpec.getMinLength() > policySpec.getMaxLength()) {
            throw new InvalidPasswordPolicySpecException("Minimun length (" + policySpec.getMinLength() + ")"
                    + "is greater than maximum length (" + policySpec.getMaxLength() + ")");
        }
    }

    private String generate(final PasswordPolicySpec policySpec) {
        String[] generatedPassword = new String[policySpec.getMinLength()];

        for (int i = 0; i < generatedPassword.length; i++) {
            generatedPassword[i] = "";
        }

        checkStartChar(generatedPassword, policySpec);

        checkEndChar(generatedPassword, policySpec);

        checkRequired(generatedPassword, policySpec);

        //filled empty chars
        for (int firstEmptyChar = firstEmptyChar(generatedPassword);
                firstEmptyChar < generatedPassword.length - 1; firstEmptyChar++) {
            generatedPassword[firstEmptyChar] = RandomStringUtils.randomAlphabetic(1);
        }

        checkPrefixAndSuffix(generatedPassword, policySpec);

        return StringUtils.join(generatedPassword);
    }

    private int randomNumber(final int range) {
        int randomNumber = (int) (Math.random() * (range - 1));
        return randomNumber == 0 ? 1 : randomNumber;
    }

    private void checkStartChar(final String[] generatedPassword, final PasswordPolicySpec policySpec) {
        if (policySpec.isMustStartWithAlpha()) {
            generatedPassword[0] = RandomStringUtils.randomAlphabetic(1);
        }
        if (policySpec.isMustStartWithNonAlpha() || policySpec.isMustStartWithDigit()) {
            generatedPassword[0] = RandomStringUtils.randomNumeric(1);
        }
        if (policySpec.isMustntStartWithAlpha()) {
            generatedPassword[0] = RandomStringUtils.randomNumeric(1);

        }
        if (policySpec.isMustntStartWithDigit()) {
            generatedPassword[0] = RandomStringUtils.randomAlphabetic(1);

        }
        if (policySpec.isMustntStartWithNonAlpha()) {
            generatedPassword[0] = RandomStringUtils.randomAlphabetic(1);

        }
    }

    private void checkEndChar(final String[] generatedPassword, final PasswordPolicySpec policySpec) {
        if (policySpec.isMustEndWithAlpha()) {
            generatedPassword[policySpec.getMinLength() - 1] = RandomStringUtils.randomAlphabetic(1);
        }
        if (policySpec.isMustEndWithNonAlpha() || policySpec.isMustEndWithDigit()) {
            generatedPassword[policySpec.getMinLength() - 1] = RandomStringUtils.randomNumeric(1);
        }

        if (policySpec.isMustntEndWithAlpha()) {
            generatedPassword[policySpec.getMinLength() - 1] = RandomStringUtils.randomNumeric(1);
        }
        if (policySpec.isMustntEndWithDigit()) {
            generatedPassword[policySpec.getMinLength() - 1] = RandomStringUtils.randomAlphabetic(1);
        }
        if (policySpec.isMustntEndWithNonAlpha()) {
            generatedPassword[policySpec.getMinLength() - 1] = RandomStringUtils.randomAlphabetic(1);

        }
    }

    private int firstEmptyChar(final String[] generatedPStrings) {
        int index = 0;
        while (!generatedPStrings[index].isEmpty()) {
            index++;
        }
        return index;
    }

    private void checkRequired(final String[] generatedPassword, final PasswordPolicySpec policySpec) {
        if (policySpec.isDigitRequired()
                && !PolicyPattern.DIGIT.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] = RandomStringUtils.randomNumeric(1);
        }

        if (policySpec.isUppercaseRequired()
                && !PolicyPattern.ALPHA_UPPERCASE.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] = RandomStringUtils.randomAlphabetic(1).toUpperCase();
        }

        if (policySpec.isLowercaseRequired()
                && !PolicyPattern.ALPHA_LOWERCASE.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] = RandomStringUtils.randomAlphabetic(1).toLowerCase();
        }

        if (policySpec.isNonAlphanumericRequired()
                && !PolicyPattern.NON_ALPHANUMERIC.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] =
                    SPECIAL_CHARS[randomNumber(SPECIAL_CHARS.length - 1)];
        }
    }

    private void checkPrefixAndSuffix(final String[] generatedPassword, final PasswordPolicySpec policySpec) {
        for (String prefix : policySpec.getPrefixesNotPermitted()) {
            if (StringUtils.join(generatedPassword).startsWith(prefix)) {
                checkStartChar(generatedPassword, policySpec);
            }
        }

        for (String suffix : policySpec.getSuffixesNotPermitted()) {
            if (StringUtils.join(generatedPassword).endsWith(suffix)) {
                checkEndChar(generatedPassword, policySpec);
            }
        }
    }
}
