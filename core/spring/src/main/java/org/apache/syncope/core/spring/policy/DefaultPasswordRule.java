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
package org.apache.syncope.core.spring.policy;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRuleConfClass;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.IllegalCharacterRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.PropertiesMessageResolver;
import org.passay.RepeatCharactersRule;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.UsernameRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@PasswordRuleConfClass(DefaultPasswordRuleConf.class)
public class DefaultPasswordRule implements PasswordRule {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultPasswordRule.class);

    public static List<Rule> conf2Rules(final DefaultPasswordRuleConf conf) {
        List<Rule> rules = new ArrayList<>();

        LengthRule lengthRule = new LengthRule();
        if (conf.getMinLength() > 0) {
            lengthRule.setMinimumLength(conf.getMinLength());
        }
        if (conf.getMaxLength() > 0) {
            lengthRule.setMaximumLength(conf.getMaxLength());
        }
        rules.add(lengthRule);

        if (conf.getAlphabetical() > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.Alphabetical, conf.getAlphabetical()));
        }

        if (conf.getUppercase() > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.UpperCase, conf.getUppercase()));
        }

        if (conf.getLowercase() > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.LowerCase, conf.getLowercase()));
        }

        if (conf.getDigit() > 0) {
            rules.add(new CharacterRule(EnglishCharacterData.Digit, conf.getDigit()));
        }

        if (conf.getSpecial() > 0) {
            rules.add(new CharacterRule(new CharacterData() {

                @Override
                public String getErrorCode() {
                    return "INSUFFICIENT_SPECIAL";
                }

                @Override
                public String getCharacters() {
                    return new String(ArrayUtils.toPrimitive(conf.getSpecialChars().toArray(Character[]::new)));
                }
            }, conf.getSpecial()));
        }

        if (!conf.getIllegalChars().isEmpty()) {
            rules.add(new IllegalCharacterRule(
                    ArrayUtils.toPrimitive(conf.getIllegalChars().toArray(Character[]::new))));
        }

        if (conf.getRepeatSame() > 0) {
            rules.add(new RepeatCharactersRule(conf.getRepeatSame()));
        }

        if (!conf.isUsernameAllowed()) {
            rules.add(new UsernameRule(true, true));
        }

        return rules;
    }

    @Autowired
    protected EncryptorManager encryptorManager;

    protected DefaultPasswordRuleConf conf;

    protected PasswordValidator passwordValidator;

    @Override
    public PasswordRuleConf getConf() {
        return conf;
    }

    @Override
    public void setConf(final PasswordRuleConf conf) {
        if (conf instanceof DefaultPasswordRuleConf defaultPasswordRuleConf) {
            this.conf = defaultPasswordRuleConf;

            Properties passay = new Properties();
            try (InputStream in = getClass().getResourceAsStream("/passay.properties")) {
                passay.load(in);
                passwordValidator = new PasswordValidator(new PropertiesMessageResolver(passay), conf2Rules(this.conf));
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize Passay", e);
            }
        } else {
            throw new IllegalArgumentException(
                    DefaultPasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    protected void enforce(final String clear, final String username, final Set<String> wordsNotPermitted) {
        RuleResult result = passwordValidator.validate(
                username == null ? new PasswordData(clear) : new PasswordData(username, clear));
        if (!result.isValid()) {
            throw new PasswordPolicyException(String.join(",", passwordValidator.getMessages(result)));
        }

        // check words not permitted
        wordsNotPermitted.stream().
                filter(word -> StringUtils.containsIgnoreCase(clear, word)).findFirst().
                ifPresent(word -> {
                    throw new PasswordPolicyException("Used word(s) not permitted");
                });
    }

    @Override
    public void enforce(final String username, final String clearPassword) {
        if (clearPassword != null) {
            Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
            enforce(clearPassword, username, wordsNotPermitted);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user, final String clearPassword) {
        if (clearPassword != null) {
            Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
            wordsNotPermitted.addAll(
                    conf.getSchemasNotPermitted().stream().
                            map(schema -> user.getPlainAttr(schema).
                            map(PlainAttr::getValuesAsStrings).orElse(null)).
                            filter(Objects::nonNull).
                            filter(values -> !CollectionUtils.isEmpty(values)).
                            flatMap(Collection::stream).
                            collect(Collectors.toSet()));

            enforce(clearPassword, user.getUsername(), wordsNotPermitted);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        conf.getWordsNotPermitted().addAll(
                conf.getSchemasNotPermitted().stream().
                        map(schema -> account.getPlainAttr(schema).
                        map(PlainAttr::getValuesAsStrings).orElse(null)).
                        filter(Objects::nonNull).
                        filter(values -> !CollectionUtils.isEmpty(values)).
                        flatMap(Collection::stream).
                        toList());

        if (account.getPassword() != null) {
            String clear = null;
            if (account.canDecodeSecrets()) {
                try {
                    clear = encryptorManager.getInstance().decode(account.getPassword(), account.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.error("Could not decode password for {}", account, e);
                }
            }

            if (clear != null) {
                Set<String> wordsNotPermitted = new HashSet<>(conf.getWordsNotPermitted());
                wordsNotPermitted.addAll(
                        conf.getSchemasNotPermitted().stream().
                                map(schema -> account.getPlainAttr(schema).
                                map(PlainAttr::getValuesAsStrings).orElse(null)).
                                filter(Objects::nonNull).
                                filter(values -> !CollectionUtils.isEmpty(values)).
                                flatMap(Collection::stream).
                                collect(Collectors.toSet()));

                enforce(clear, account.getUsername(), wordsNotPermitted);
            }
        }
    }
}
