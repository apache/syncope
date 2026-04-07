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
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.passay.UnicodeString;
import org.passay.data.CharacterData;
import org.passay.data.EnglishCharacterData;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.passay.rule.CharacterRule;
import org.passay.rule.DictionaryRule;
import org.passay.rule.IllegalCharacterRule;
import org.passay.rule.LengthRule;
import org.passay.rule.RepeatCharactersRule;
import org.passay.rule.Rule;
import org.passay.rule.UsernameRule;

public interface PasswordGenerator {

    static List<Rule> conf2Rules(final DefaultPasswordRuleConf conf) {
        List<Rule> rules = new ArrayList<>();

        if (conf.getMinLength() > 0 || conf.getMaxLength() > 0) {
            rules.add(new LengthRule(
                    conf.getMinLength() > 0 ? conf.getMinLength() : 0,
                    conf.getMaxLength() > 0 ? conf.getMaxLength() : Integer.MAX_VALUE));
        }

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
                    new UnicodeString(ArrayUtils.toPrimitive(conf.getIllegalChars().toArray(Character[]::new)))));
        }

        if (conf.getRepeatSame() > 0) {
            rules.add(new RepeatCharactersRule(conf.getRepeatSame()));
        }

        if (!conf.isUsernameAllowed()) {
            rules.add(new UsernameRule(true, true));
        }

        if (!conf.getWordsNotPermitted().isEmpty()) {
            conf.getWordsNotPermitted().sort(Comparator.naturalOrder());
            rules.add(new DictionaryRule(new WordListDictionary(
                    new ArrayWordList(conf.getWordsNotPermitted().toArray(String[]::new), true)), true));
        }

        return rules;
    }

    String generate(ExternalResource resource, List<Realm> realms);

    String generate(List<PasswordPolicy> policies);
}
