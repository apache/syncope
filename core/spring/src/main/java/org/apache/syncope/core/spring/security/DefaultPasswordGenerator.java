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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.utils.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.provisioning.api.utils.policy.PolicyPattern;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generate random passwords according to given policies.
 * When no minimum and / or maximum length are specified, default values are set.
 *
 * <strong>WARNING</strong>: This class only takes {@link DefaultPasswordRuleConf} into account.
 */
public class DefaultPasswordGenerator implements PasswordGenerator {

    private static final char[] SPECIAL_CHARS = { '!', '£', '%', '&', '(', ')', '?', '#', '$' };

    private static final int VERY_MIN_LENGTH = 0;

    private static final int VERY_MAX_LENGTH = 64;

    private static final int MIN_LENGTH_IF_ZERO = 8;

    @Transactional(readOnly = true)
    @Override
    public String generate(final ExternalResource resource) throws InvalidPasswordRuleConf {
        List<PasswordRuleConf> ruleConfs = new ArrayList<>();

        if (resource.getPasswordPolicy() != null) {
            ruleConfs.addAll(resource.getPasswordPolicy().getRuleConfs());
        }

        return generate(ruleConfs);
    }

    @Override
    public String generate(final List<PasswordRuleConf> ruleConfs) throws InvalidPasswordRuleConf {
        List<DefaultPasswordRuleConf> defaultRuleConfs = new ArrayList<>();
        ruleConfs.stream().
                filter(ruleConf -> (ruleConf instanceof DefaultPasswordRuleConf)).
                forEachOrdered(ruleConf -> {
                    defaultRuleConfs.add((DefaultPasswordRuleConf) ruleConf);
                });

        DefaultPasswordRuleConf ruleConf = merge(defaultRuleConfs);
        check(ruleConf);
        return generate(ruleConf);
    }

    private DefaultPasswordRuleConf merge(final List<DefaultPasswordRuleConf> defaultRuleConfs) {
        DefaultPasswordRuleConf result = new DefaultPasswordRuleConf();
        result.setMinLength(VERY_MIN_LENGTH);
        result.setMaxLength(VERY_MAX_LENGTH);

        defaultRuleConfs.forEach(ruleConf -> {
            if (ruleConf.getMinLength() > result.getMinLength()) {
                result.setMinLength(ruleConf.getMinLength());
            }

            if ((ruleConf.getMaxLength() != 0) && ((ruleConf.getMaxLength() < result.getMaxLength()))) {
                result.setMaxLength(ruleConf.getMaxLength());
            }
            result.getPrefixesNotPermitted().addAll(ruleConf.getPrefixesNotPermitted());
            result.getSuffixesNotPermitted().addAll(ruleConf.getSuffixesNotPermitted());

            if (!result.isNonAlphanumericRequired()) {
                result.setNonAlphanumericRequired(ruleConf.isNonAlphanumericRequired());
            }

            if (!result.isAlphanumericRequired()) {
                result.setAlphanumericRequired(ruleConf.isAlphanumericRequired());
            }
            if (!result.isDigitRequired()) {
                result.setDigitRequired(ruleConf.isDigitRequired());
            }

            if (!result.isLowercaseRequired()) {
                result.setLowercaseRequired(ruleConf.isLowercaseRequired());
            }
            if (!result.isUppercaseRequired()) {
                result.setUppercaseRequired(ruleConf.isUppercaseRequired());
            }
            if (!result.isMustStartWithDigit()) {
                result.setMustStartWithDigit(ruleConf.isMustStartWithDigit());
            }
            if (!result.isMustntStartWithDigit()) {
                result.setMustntStartWithDigit(ruleConf.isMustntStartWithDigit());
            }
            if (!result.isMustEndWithDigit()) {
                result.setMustEndWithDigit(ruleConf.isMustEndWithDigit());
            }
            if (result.isMustntEndWithDigit()) {
                result.setMustntEndWithDigit(ruleConf.isMustntEndWithDigit());
            }
            if (!result.isMustStartWithAlpha()) {
                result.setMustStartWithAlpha(ruleConf.isMustStartWithAlpha());
            }
            if (!result.isMustntStartWithAlpha()) {
                result.setMustntStartWithAlpha(ruleConf.isMustntStartWithAlpha());
            }
            if (!result.isMustStartWithNonAlpha()) {
                result.setMustStartWithNonAlpha(ruleConf.isMustStartWithNonAlpha());
            }
            if (!result.isMustntStartWithNonAlpha()) {
                result.setMustntStartWithNonAlpha(ruleConf.isMustntStartWithNonAlpha());
            }
            if (!result.isMustEndWithNonAlpha()) {
                result.setMustEndWithNonAlpha(ruleConf.isMustEndWithNonAlpha());
            }
            if (!result.isMustntEndWithNonAlpha()) {
                result.setMustntEndWithNonAlpha(ruleConf.isMustntEndWithNonAlpha());
            }
            if (!result.isMustEndWithAlpha()) {
                result.setMustEndWithAlpha(ruleConf.isMustEndWithAlpha());
            }
            if (!result.isMustntEndWithAlpha()) {
                result.setMustntEndWithAlpha(ruleConf.isMustntEndWithAlpha());
            }
            if (!result.isUsernameAllowed()) {
                result.setUsernameAllowed(ruleConf.isUsernameAllowed());
            }
        });

        if (result.getMinLength() == 0) {
            result.setMinLength(
                    result.getMaxLength() < MIN_LENGTH_IF_ZERO ? result.getMaxLength() : MIN_LENGTH_IF_ZERO);
        }

        return result;
    }

    private void check(final DefaultPasswordRuleConf defaultPasswordRuleConf)
            throws InvalidPasswordRuleConf {

        if (defaultPasswordRuleConf.isMustEndWithAlpha() && defaultPasswordRuleConf.isMustntEndWithAlpha()) {
            throw new InvalidPasswordRuleConf(
                    "mustEndWithAlpha and mustntEndWithAlpha are both true");
        }
        if (defaultPasswordRuleConf.isMustEndWithAlpha() && defaultPasswordRuleConf.isMustEndWithDigit()) {
            throw new InvalidPasswordRuleConf(
                    "mustEndWithAlpha and mustEndWithDigit are both true");
        }
        if (defaultPasswordRuleConf.isMustEndWithDigit() && defaultPasswordRuleConf.isMustntEndWithDigit()) {
            throw new InvalidPasswordRuleConf(
                    "mustEndWithDigit and mustntEndWithDigit are both true");
        }
        if (defaultPasswordRuleConf.isMustEndWithNonAlpha() && defaultPasswordRuleConf.isMustntEndWithNonAlpha()) {
            throw new InvalidPasswordRuleConf(
                    "mustEndWithNonAlpha and mustntEndWithNonAlpha are both true");
        }
        if (defaultPasswordRuleConf.isMustStartWithAlpha() && defaultPasswordRuleConf.isMustntStartWithAlpha()) {
            throw new InvalidPasswordRuleConf(
                    "mustStartWithAlpha and mustntStartWithAlpha are both true");
        }
        if (defaultPasswordRuleConf.isMustStartWithAlpha() && defaultPasswordRuleConf.isMustStartWithDigit()) {
            throw new InvalidPasswordRuleConf(
                    "mustStartWithAlpha and mustStartWithDigit are both true");
        }
        if (defaultPasswordRuleConf.isMustStartWithDigit() && defaultPasswordRuleConf.isMustntStartWithDigit()) {
            throw new InvalidPasswordRuleConf(
                    "mustStartWithDigit and mustntStartWithDigit are both true");
        }
        if (defaultPasswordRuleConf.isMustStartWithNonAlpha() && defaultPasswordRuleConf.isMustntStartWithNonAlpha()) {
            throw new InvalidPasswordRuleConf(
                    "mustStartWithNonAlpha and mustntStartWithNonAlpha are both true");
        }
        if (defaultPasswordRuleConf.getMinLength() > defaultPasswordRuleConf.getMaxLength()) {
            throw new InvalidPasswordRuleConf(
                    "Minimun length (" + defaultPasswordRuleConf.getMinLength() + ")"
                    + "is greater than maximum length (" + defaultPasswordRuleConf.getMaxLength() + ")");
        }
    }

    private String generate(final DefaultPasswordRuleConf ruleConf) {
        String[] generatedPassword = new String[ruleConf.getMinLength()];

        for (int i = 0; i < generatedPassword.length; i++) {
            generatedPassword[i] = StringUtils.EMPTY;
        }

        checkStartChar(generatedPassword, ruleConf);

        checkEndChar(generatedPassword, ruleConf);

        checkRequired(generatedPassword, ruleConf);

        for (int firstEmptyChar = firstEmptyChar(generatedPassword);
                firstEmptyChar < generatedPassword.length - 1; firstEmptyChar++) {

            generatedPassword[firstEmptyChar] = SecureRandomUtils.generateRandomLetter();
        }

        checkPrefixAndSuffix(generatedPassword, ruleConf);

        return StringUtils.join(generatedPassword);
    }

    private void checkStartChar(final String[] generatedPassword, final DefaultPasswordRuleConf ruleConf) {
        if (ruleConf.isMustStartWithAlpha()) {
            generatedPassword[0] = SecureRandomUtils.generateRandomLetter();
        }
        if (ruleConf.isMustStartWithNonAlpha() || ruleConf.isMustStartWithDigit()) {
            generatedPassword[0] = SecureRandomUtils.generateRandomNumber();
        }
        if (ruleConf.isMustntStartWithAlpha()) {
            generatedPassword[0] = SecureRandomUtils.generateRandomNumber();
        }
        if (ruleConf.isMustntStartWithDigit()) {
            generatedPassword[0] = SecureRandomUtils.generateRandomLetter();
        }
        if (ruleConf.isMustntStartWithNonAlpha()) {
            generatedPassword[0] = SecureRandomUtils.generateRandomLetter();
        }

        if (StringUtils.EMPTY.equals(generatedPassword[0])) {
            generatedPassword[0] = SecureRandomUtils.generateRandomLetter();
        }
    }

    private void checkEndChar(final String[] generatedPassword, final DefaultPasswordRuleConf ruleConf) {
        if (ruleConf.isMustEndWithAlpha()) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomLetter();
        }
        if (ruleConf.isMustEndWithNonAlpha() || ruleConf.isMustEndWithDigit()) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomNumber();
        }

        if (ruleConf.isMustntEndWithAlpha()) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomNumber();
        }
        if (ruleConf.isMustntEndWithDigit()) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomLetter();
        }
        if (ruleConf.isMustntEndWithNonAlpha()) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomLetter();
        }

        if (StringUtils.EMPTY.equals(generatedPassword[ruleConf.getMinLength() - 1])) {
            generatedPassword[ruleConf.getMinLength() - 1] = SecureRandomUtils.generateRandomLetter();
        }
    }

    private int firstEmptyChar(final String[] generatedPStrings) {
        int index = 0;
        while (!generatedPStrings[index].isEmpty()) {
            index++;
        }
        return index;
    }

    private void checkRequired(final String[] generatedPassword, final DefaultPasswordRuleConf ruleConf) {
        if (ruleConf.isDigitRequired()
                && !PolicyPattern.DIGIT.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] = SecureRandomUtils.generateRandomNumber();
        }

        if (ruleConf.isUppercaseRequired()
                && !PolicyPattern.ALPHA_UPPERCASE.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] =
                    SecureRandomUtils.generateRandomLetter().toUpperCase();
        }

        if (ruleConf.isLowercaseRequired()
                && !PolicyPattern.ALPHA_LOWERCASE.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] =
                    SecureRandomUtils.generateRandomLetter().toLowerCase();
        }

        if (ruleConf.isNonAlphanumericRequired()
                && !PolicyPattern.NON_ALPHANUMERIC.matcher(StringUtils.join(generatedPassword)).matches()) {

            generatedPassword[firstEmptyChar(generatedPassword)] =
                    SecureRandomUtils.generateRandomSpecialCharacter(SPECIAL_CHARS);
        }
    }

    private void checkPrefixAndSuffix(final String[] generatedPassword, final DefaultPasswordRuleConf ruleConf) {
        for (String prefix : ruleConf.getPrefixesNotPermitted()) {
            if (StringUtils.join(generatedPassword).startsWith(prefix)) {
                checkStartChar(generatedPassword, ruleConf);
            }
        }

        for (String suffix : ruleConf.getSuffixesNotPermitted()) {
            if (StringUtils.join(generatedPassword).endsWith(suffix)) {
                checkEndChar(generatedPassword, ruleConf);
            }
        }
    }

}
