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
package org.apache.syncope.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.policy.PolicyPattern;
import org.apache.syncope.types.PasswordPolicySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordGenerator.class);

    private static final String[] SPECIAL_CHAR = {"!", "£", "%", "&", "(", ")", "?", "#", "_", "$"};

    @Autowired
    private PolicyDAO policyDAO;

    public String generatePasswordFromPwdSpec(final List<PasswordPolicySpec> passwordPolicySpecs)
            throws IncompatiblePolicyException {

        PasswordPolicySpec policySpec = mergePolicySpecs(passwordPolicySpecs);

        evaluateFinalPolicySpec(policySpec);
        return generatePassword(policySpec);
    }

    public String generateUserPassword(final SyncopeUser user)
            throws IncompatiblePolicyException {

        List<PasswordPolicySpec> userPasswordPolicies = new ArrayList<PasswordPolicySpec>();
        PasswordPolicySpec passwordPolicySpec = policyDAO.getGlobalPasswordPolicy().getSpecification();

        userPasswordPolicies.add(passwordPolicySpec);

        PasswordPolicySpec rolePasswordPolicySpec;
        if ((user.getRoles() != null) || (!user.getRoles().isEmpty())) {
            for (Iterator<SyncopeRole> rolesIterator = user.getRoles().iterator(); rolesIterator.hasNext();) {
                SyncopeRole syncopeRole = rolesIterator.next();
                rolePasswordPolicySpec = syncopeRole.getPasswordPolicy().getSpecification();
                userPasswordPolicies.add(rolePasswordPolicySpec);
            }
        }

        PasswordPolicySpec resourcePasswordPolicySpec;

        if ((user.getResources() != null) || (!user.getResources().isEmpty())) {
            for (Iterator<ExternalResource> resourcesIterator = user.getResources().iterator();
                    resourcesIterator.hasNext();) {
                ExternalResource externalResource = resourcesIterator.next();
                if (externalResource.getPasswordPolicy() != null) {
                    resourcePasswordPolicySpec = externalResource.getPasswordPolicy().getSpecification();
                    userPasswordPolicies.add(resourcePasswordPolicySpec);
                }
            }
        }

        PasswordPolicySpec policySpec = mergePolicySpecs(userPasswordPolicies);

        evaluateFinalPolicySpec(policySpec);
        return generatePassword(policySpec);
    }

    private PasswordPolicySpec mergePolicySpecs(final List<PasswordPolicySpec> userPasswordPolicies) {
        PasswordPolicySpec fpps = new PasswordPolicySpec();
        fpps.setMinLength(0);
        fpps.setMaxLength(1000);

        for (Iterator<PasswordPolicySpec> it = userPasswordPolicies.iterator(); it.hasNext();) {
            PasswordPolicySpec policySpec = it.next();
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

    private void evaluateFinalPolicySpec(final PasswordPolicySpec policySpec)
            throws IncompatiblePolicyException {
        
        if (policySpec.getMinLength() == 0) {
            LOG.error("Minimum lenght given is zero");
            throw new IncompatiblePolicyException("Minimum lenght given is zero");
        }
        if (policySpec.isMustEndWithAlpha() && policySpec.isMustntEndWithAlpha()) {
            LOG.error("Incompatible password policy specification: mustEndWithAlpha and"
                    + "mustntEndWithAlpha are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustEndWithAlpha and"
                    + "mustntEndWithAlpha are true");
        }
        if (policySpec.isMustEndWithAlpha() && policySpec.isMustEndWithDigit()) {
            LOG.error("Incompatible password policy specification: mustEndWithAlpha and"
                    + "mustEndWithDigit are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustEndWithAlpha and"
                    + "mustEndWithDigit are true");
        }
        if (policySpec.isMustEndWithDigit() && policySpec.isMustntEndWithDigit()) {
            LOG.error("Incompatible password policy specification: mustEndWithDigit and"
                    + "mustntEndWithDigit are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustEndWithDigit and"
                    + "mustntEndWithDigit are true");
        }
        if (policySpec.isMustEndWithNonAlpha() && policySpec.isMustntEndWithNonAlpha()) {
            LOG.error("Incompatible password policy specification: mustEndWithNonAlpha and"
                    + "mustntEndWithNonAlpha are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustEndWithNonAlpha and"
                    + "mustntEndWithNonAlpha are true");
        }
        if (policySpec.isMustStartWithAlpha() && policySpec.isMustntStartWithAlpha()) {
            LOG.error("Incompatible password policy specification: mustStartWithAlpha and"
                    + "mustntStartWithAlpha are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustStartWithAlpha and"
                    + "mustntStartWithAlpha are true");
        }
        if (policySpec.isMustStartWithAlpha() && policySpec.isMustStartWithDigit()) {
            LOG.error("Incompatible password policy specification: mustStartWithAlpha and"
                    + "mustStartWithDigit are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustStartWithAlpha and"
                    + "mustStartWithDigit are true");
        }
        if (policySpec.isMustStartWithDigit() && policySpec.isMustntStartWithDigit()) {
            LOG.error("Incompatible password policy specification: mustStartWithDigit and"
                    + "mustntStartWithDigit are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustStartWithDigit and"
                    + " mustntStartWithDigit are true");
        }
        if (policySpec.isMustStartWithNonAlpha() && policySpec.isMustntStartWithNonAlpha()) {
            LOG.error("Incompatible password policy specification: mustStartWithNonAlpha"
                    + "and mustntStartWithNonAlpha are true");
            throw new IncompatiblePolicyException("Incompatible password policy specification: mustStartWithNonAlpha"
                    + "and mustntStartWithNonAlpha are true");
        }
        if (policySpec.getMinLength() > policySpec.getMaxLength()) {
            LOG.error("Minimun length given (" + policySpec.getMinLength() + ") is higher than"
                    + "maximum allowed (" + policySpec.getMaxLength() + ")");
            throw new IncompatiblePolicyException("Minimun length given (" + policySpec.getMinLength() + ")"
                    + "is higher than maximum allowed (" + policySpec.getMaxLength() + ")");
        }
    }

    private String generatePassword(final PasswordPolicySpec policySpec) {
        StringBuilder generatedPassword = new StringBuilder(policySpec.getMinLength());

        if (policySpec.isDigitRequired() || policySpec.isAlphanumericRequired()) {
            generatedPassword.append(RandomStringUtils.randomAlphanumeric(policySpec.getMinLength()));
        }

        if (policySpec.isDigitRequired() && !PolicyPattern.DIGIT.matcher(generatedPassword.toString()).matches()) {
            int where = randomNumber(policySpec.getMinLength());
            generatedPassword.deleteCharAt(where);
            generatedPassword.insert(where, RandomStringUtils.randomNumeric(1));
        }

        if (!policySpec.isAlphanumericRequired() && !policySpec.isDigitRequired()) {
            generatedPassword.append(RandomStringUtils.randomAlphabetic(policySpec.getMinLength()));
        }

        if (policySpec.isUppercaseRequired() && !PolicyPattern.ALPHA_UPPERCASE
                .matcher(generatedPassword.toString()).matches()) {
            int where = randomNumber(policySpec.getMinLength());
            generatedPassword.deleteCharAt(where);
            generatedPassword.insert(where, RandomStringUtils.randomAlphabetic(1));
        }

        if (policySpec.isLowercaseRequired() && !PolicyPattern.ALPHA_LOWERCASE.
                matcher(generatedPassword.toString()).matches()) {
            int where = randomNumber(policySpec.getMinLength());
            generatedPassword.deleteCharAt(where);
            generatedPassword.insert(where, RandomStringUtils.randomAlphabetic(1).toLowerCase());
        }

        if (policySpec.isNonAlphanumericRequired()) {
            int where = randomNumber(policySpec.getMinLength());
            generatedPassword.deleteCharAt(where);
            generatedPassword.insert(where, SPECIAL_CHAR[randomNumber(SPECIAL_CHAR.length - 1)]);
        }

        checkEndChar(generatedPassword, policySpec);

        checkstartChar(generatedPassword, policySpec);

        for (Iterator<String> it = policySpec.getPrefixesNotPermitted().iterator(); it.hasNext();) {
            String prefix = it.next();
            if (generatedPassword.toString().startsWith(prefix)) {
                checkstartChar(generatedPassword, policySpec);
            }
        }

        for (Iterator<String> it = policySpec.getSuffixesNotPermitted().iterator(); it.hasNext();) {
            String suffix = it.next();
            if (generatedPassword.toString().endsWith(suffix)) {
                checkEndChar(generatedPassword, policySpec);
            }
        }
        return generatedPassword.toString();
    }

    private int randomNumber(final int range) {
        int randomNumber = (int) (Math.random() * (range - 1));
        return randomNumber == 0 ? 1 : randomNumber;
    }

    private void checkstartChar(final StringBuilder generatedPassword, final PasswordPolicySpec policySpec) {
        if (policySpec.isMustStartWithAlpha()) {
            generatedPassword.deleteCharAt(0);
            generatedPassword.insert(0, RandomStringUtils.randomAlphabetic(1));
        }
        if (policySpec.isMustStartWithNonAlpha() || policySpec.isMustStartWithDigit()) {
            generatedPassword.deleteCharAt(0);
            generatedPassword.insert(0, RandomStringUtils.randomNumeric(1));
        }
        if (policySpec.isMustntStartWithAlpha()) {
            generatedPassword.deleteCharAt(0);
            generatedPassword.insert(0, RandomStringUtils.randomNumeric(1));
        }
        if (policySpec.isMustntStartWithDigit()) {
            generatedPassword.deleteCharAt(0);
            generatedPassword.insert(0, RandomStringUtils.randomAlphabetic(1));
        }
        if (policySpec.isMustntStartWithNonAlpha()) {
            generatedPassword.deleteCharAt(0);
            generatedPassword.insert(0, RandomStringUtils.randomAlphanumeric(1));
        }
    }

    private void checkEndChar(final StringBuilder generatedPassword, final PasswordPolicySpec policySpec) {
        if (policySpec.isMustEndWithAlpha()) {
            generatedPassword.deleteCharAt(policySpec.getMinLength() - 1);
            generatedPassword.insert(policySpec.getMinLength() - 1, RandomStringUtils.randomAlphabetic(1));
        }
        if (policySpec.isMustEndWithNonAlpha() || policySpec.isMustEndWithDigit()) {
            generatedPassword.deleteCharAt(policySpec.getMinLength() - 1);
            generatedPassword.insert(policySpec.getMinLength() - 1, RandomStringUtils.randomNumeric(1));
        }

        if (policySpec.isMustntEndWithAlpha()) {
            generatedPassword.deleteCharAt(policySpec.getMinLength() - 1);
            generatedPassword.insert(policySpec.getMinLength() - 1, RandomStringUtils.randomNumeric(1));
        }
        if (policySpec.isMustntEndWithDigit()) {
            generatedPassword.deleteCharAt(policySpec.getMinLength() - 1);
            generatedPassword.insert(policySpec.getMinLength() - 1, RandomStringUtils.randomAlphabetic(1));
        }
        if (policySpec.isMustntEndWithNonAlpha()) {
            generatedPassword.deleteCharAt(policySpec.getMinLength() - 1);
            generatedPassword.insert(policySpec.getMinLength() - 1, RandomStringUtils.randomAlphabetic(1));
        }
    }
}
