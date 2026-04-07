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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRuleConfClass;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.passay.DefaultPasswordValidator;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.ValidationResult;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.passay.resolver.PropertiesMessageResolver;
import org.passay.rule.DictionaryRule;
import org.passay.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@PasswordRuleConfClass(DefaultPasswordRuleConf.class)
public class DefaultPasswordRule implements PasswordRule {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultPasswordRule.class);

    protected final PropertiesMessageResolver messageResolver;

    @Autowired
    protected EncryptorManager encryptorManager;

    protected DefaultPasswordRuleConf conf;

    public DefaultPasswordRule() {
        Properties passay = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/passay.properties")) {
            passay.load(in);
            messageResolver = new PropertiesMessageResolver(passay);
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize Passay", e);
        }
    }

    @Override
    public PasswordRuleConf getConf() {
        return conf;
    }

    @Override
    public void setConf(final PasswordRuleConf conf) {
        if (conf instanceof DefaultPasswordRuleConf defaultPasswordRuleConf) {
            this.conf = defaultPasswordRuleConf;
        } else {
            throw new IllegalArgumentException(
                    DefaultPasswordRuleConf.class.getName() + " expected, got " + conf.getClass().getName());
        }
    }

    protected void enforce(final String username, final String clearPassword, final Collection<String> notPermitted) {
        List<Rule> rules = PasswordGenerator.conf2Rules(conf);
        if (!notPermitted.isEmpty()) {
            rules.add(new DictionaryRule(new WordListDictionary(new ArrayWordList(
                    notPermitted.stream().distinct().sorted(Comparator.naturalOrder()).toArray(String[]::new), true)),
                    true));
        }

        PasswordValidator passwordValidator = new DefaultPasswordValidator(messageResolver, rules);
        ValidationResult result = passwordValidator.validate(
                username == null ? new PasswordData(clearPassword) : new PasswordData(username, clearPassword));
        if (!result.isValid()) {
            throw new PasswordPolicyException(result.getMessages().stream().collect(Collectors.joining(",")));
        }
    }

    @Override
    public void enforce(final String username, final String clearPassword) {
        if (clearPassword == null) {
            return;
        }

        enforce(username, clearPassword, Set.of());
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final User user, final String clearPassword) {
        if (clearPassword == null) {
            return;
        }

        List<String> notPermitted = conf.getSchemasNotPermitted().stream().
                map(schema -> user.getPlainAttr(schema).
                map(PlainAttr::getValuesAsStrings).orElse(null)).
                filter(values -> !CollectionUtils.isEmpty(values)).
                flatMap(Collection::stream).
                toList();

        enforce(user.getUsername(), clearPassword, notPermitted);
    }

    @Transactional(readOnly = true)
    @Override
    public void enforce(final LinkedAccount account) {
        String clearPassword = null;
        if (account.getPassword() != null && account.canDecodeSecrets()) {
            try {
                clearPassword = encryptorManager.getInstance().decode(
                        account.getPassword(), account.getCipherAlgorithm());
            } catch (Exception e) {
                LOG.error("Could not decode password for {}", account, e);
            }
        }
        if (clearPassword == null) {
            return;
        }

        List<String> notPermitted = conf.getSchemasNotPermitted().stream().
                map(schema -> account.getPlainAttr(schema).
                map(PlainAttr::getValuesAsStrings).orElse(null)).
                filter(values -> !CollectionUtils.isEmpty(values)).
                flatMap(Collection::stream).
                toList();

        enforce(account.getUsername(), clearPassword, notPermitted);
    }
}
