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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.misc.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.misc.policy.PolicyPattern;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.beans.factory.annotation.Autowired;

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

    private static final int MIN_LENGTH_IF_ZERO = 6;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Override
    public String generate(final User user) throws InvalidPasswordRuleConf {
        List<PasswordRuleConf> ruleConfs = new ArrayList<>();

        for (Realm ancestor : realmDAO.findAncestors(user.getRealm())) {
            if (ancestor.getPasswordPolicy() != null) {
                ruleConfs.addAll(ancestor.getPasswordPolicy().getRuleConfs());
            }
        }

        for (ExternalResource resource : userDAO.findAllResources(user)) {
            if (resource.getPasswordPolicy() != null) {
                ruleConfs.addAll(resource.getPasswordPolicy().getRuleConfs());
            }
        }

        return generate(ruleConfs);
    }

    @Override
    public String generate(final List<PasswordRuleConf> ruleConfs) throws InvalidPasswordRuleConf {
        List<DefaultPasswordRuleConf> defaultRuleConfs = new ArrayList<>();
        for (PasswordRuleConf ruleConf : ruleConfs) {
            if (ruleConf instanceof DefaultPasswordRuleConf) {
                defaultRuleConfs.add((DefaultPasswordRuleConf) ruleConf);
            }
        }

        DefaultPasswordRuleConf ruleConf = merge(defaultRuleConfs);
        check(ruleConf);
        return generate(ruleConf);
    }

    private DefaultPasswordRuleConf merge(final List<DefaultPasswordRuleConf> defaultRuleConfs) {
        DefaultPasswordRuleConf fpps = new DefaultPasswordRuleConf();
        fpps.setMinLength(VERY_MIN_LENGTH);
        fpps.setMaxLength(VERY_MAX_LENGTH);

        for (DefaultPasswordRuleConf ruleConf : defaultRuleConfs) {
            if (ruleConf.getMinLength() > fpps.getMinLength()) {
                fpps.setMinLength(ruleConf.getMinLength());
            }

            if ((ruleConf.getMaxLength() != 0) && ((ruleConf.getMaxLength() < fpps.getMaxLength()))) {
                fpps.setMaxLength(ruleConf.getMaxLength());
            }
            fpps.getPrefixesNotPermitted().addAll(ruleConf.getPrefixesNotPermitted());
            fpps.getSuffixesNotPermitted().addAll(ruleConf.getSuffixesNotPermitted());

            if (!fpps.isNonAlphanumericRequired()) {
                fpps.setNonAlphanumericRequired(ruleConf.isNonAlphanumericRequired());
            }

            if (!fpps.isAlphanumericRequired()) {
                fpps.setAlphanumericRequired(ruleConf.isAlphanumericRequired());
            }
            if (!fpps.isDigitRequired()) {
                fpps.setDigitRequired(ruleConf.isDigitRequired());
            }

            if (!fpps.isLowercaseRequired()) {
                fpps.setLowercaseRequired(ruleConf.isLowercaseRequired());
            }
            if (!fpps.isUppercaseRequired()) {
                fpps.setUppercaseRequired(ruleConf.isUppercaseRequired());
            }
            if (!fpps.isMustStartWithDigit()) {
                fpps.setMustStartWithDigit(ruleConf.isMustStartWithDigit());
            }
            if (!fpps.isMustntStartWithDigit()) {
                fpps.setMustntStartWithDigit(ruleConf.isMustntStartWithDigit());
            }
            if (!fpps.isMustEndWithDigit()) {
                fpps.setMustEndWithDigit(ruleConf.isMustEndWithDigit());
            }
            if (fpps.isMustntEndWithDigit()) {
                fpps.setMustntEndWithDigit(ruleConf.isMustntEndWithDigit());
            }
            if (!fpps.isMustStartWithAlpha()) {
                fpps.setMustStartWithAlpha(ruleConf.isMustStartWithAlpha());
            }
            if (!fpps.isMustntStartWithAlpha()) {
                fpps.setMustntStartWithAlpha(ruleConf.isMustntStartWithAlpha());
            }
            if (!fpps.isMustStartWithNonAlpha()) {
                fpps.setMustStartWithNonAlpha(ruleConf.isMustStartWithNonAlpha());
            }
            if (!fpps.isMustntStartWithNonAlpha()) {
                fpps.setMustntStartWithNonAlpha(ruleConf.isMustntStartWithNonAlpha());
            }
            if (!fpps.isMustEndWithNonAlpha()) {
                fpps.setMustEndWithNonAlpha(ruleConf.isMustEndWithNonAlpha());
            }
            if (!fpps.isMustntEndWithNonAlpha()) {
                fpps.setMustntEndWithNonAlpha(ruleConf.isMustntEndWithNonAlpha());
            }
            if (!fpps.isMustEndWithAlpha()) {
                fpps.setMustEndWithAlpha(ruleConf.isMustEndWithAlpha());
            }
            if (!fpps.isMustntEndWithAlpha()) {
                fpps.setMustntEndWithAlpha(ruleConf.isMustntEndWithAlpha());
            }
        }

        if (fpps.getMinLength() == 0) {
            fpps.setMinLength(fpps.getMaxLength() < MIN_LENGTH_IF_ZERO ? fpps.getMaxLength() : MIN_LENGTH_IF_ZERO);
        }

        return fpps;
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
