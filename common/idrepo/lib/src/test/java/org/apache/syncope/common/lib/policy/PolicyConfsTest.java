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
package org.apache.syncope.common.lib.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyConfsTest {

    @Test
    void defaultPasswordRuleConf() {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();

        assertEquals(DefaultPasswordRuleConf.class.getName(), conf.getName());

        conf.setName("conf");
        conf.setMaxLength(20);
        conf.setMinLength(8);
        conf.setAlphabetical(2);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(1);
        conf.setRepeatSame(2);
        conf.setUsernameAllowed(false);
        conf.setNotPermittedCaseSensitive(true);
        conf.setNotPermittedAsSubstrings(true);
        conf.setNotPermittedBackwards(true);
        conf.getSpecialChars().add('!');
        conf.getIllegalChars().add('#');
        conf.getWordsNotPermitted().add("secret");
        conf.getSchemasNotPermitted().add("password");

        assertEquals("conf", conf.getName());
        assertEquals(20, conf.getMaxLength());
        assertEquals(8, conf.getMinLength());
        assertEquals(2, conf.getAlphabetical());
        assertEquals(1, conf.getUppercase());
        assertEquals(1, conf.getLowercase());
        assertEquals(1, conf.getDigit());
        assertEquals(1, conf.getSpecial());
        assertEquals(2, conf.getRepeatSame());
        assertFalse(conf.isUsernameAllowed());
        assertTrue(conf.isNotPermittedCaseSensitive());
        assertTrue(conf.isNotPermittedAsSubstrings());
        assertTrue(conf.isNotPermittedBackwards());
        assertEquals(1, conf.getSpecialChars().size());
        assertEquals(1, conf.getIllegalChars().size());
        assertEquals(1, conf.getWordsNotPermitted().size());
        assertEquals(1, conf.getSchemasNotPermitted().size());
    }

    @Test
    void abstractPasswordRuleConf() {
        AbstractPasswordRuleConf conf = new AbstractPasswordRuleConf("name") {
        };

        assertEquals("name", conf.getName());
    }

    @Test
    void defaultAccountRuleConf() {
        DefaultAccountRuleConf conf = new DefaultAccountRuleConf();

        assertEquals(DefaultAccountRuleConf.class.getName(), conf.getName());

        conf.setName("conf");
        conf.setMaxLength(20);
        conf.setMinLength(8);
        conf.setPattern("[a-z]+");
        conf.setAllLowerCase(true);
        conf.setAllUpperCase(true);
        conf.getWordsNotPermitted().add("secret");
        conf.getSchemasNotPermitted().add("username");
        conf.getPrefixesNotPermitted().add("prefix");
        conf.getSuffixesNotPermitted().add("suffix");

        assertEquals("conf", conf.getName());
        assertEquals(20, conf.getMaxLength());
        assertEquals(8, conf.getMinLength());
        assertEquals("[a-z]+", conf.getPattern());
        assertTrue(conf.isAllLowerCase());
        assertTrue(conf.isAllUpperCase());
        assertEquals(1, conf.getWordsNotPermitted().size());
        assertEquals(1, conf.getSchemasNotPermitted().size());
        assertEquals(1, conf.getPrefixesNotPermitted().size());
        assertEquals(1, conf.getSuffixesNotPermitted().size());
    }

    @Test
    void abstractAccountRuleConf() {
        AbstractAccountRuleConf conf = new AbstractAccountRuleConf("name") {
        };

        assertEquals("name", conf.getName());
    }

    @Test
    void haveIBeenPwnedPasswordRuleConf() {
        HaveIBeenPwnedPasswordRuleConf conf = new HaveIBeenPwnedPasswordRuleConf();

        assertEquals(HaveIBeenPwnedPasswordRuleConf.class.getName(), conf.getName());
    }

    @Test
    void passwordPolicyTO() {
        PasswordPolicyTO policy = new PasswordPolicyTO();
        policy.setName("password");
        policy.setAllowNullPassword(true);
        policy.setHistoryLength(5);
        policy.getRules().add(DefaultPasswordRuleConf.class.getName());

        assertEquals("password", policy.getName());
        assertEquals(PasswordPolicyTO.class.getName(), policy.getDiscriminator());
        assertTrue(policy.isAllowNullPassword());
        assertEquals(5, policy.getHistoryLength());
        assertEquals(1, policy.getRules().size());
    }

    @Test
    void accountPolicyTO() {
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setName("account");
        policy.setPropagateSuspension(true);
        policy.setMaxAuthenticationAttempts(3);
        policy.getRules().add(DefaultAccountRuleConf.class.getName());
        policy.getPassthroughResources().add("resource");

        assertEquals("account", policy.getName());
        assertEquals(AccountPolicyTO.class.getName(), policy.getDiscriminator());
        assertTrue(policy.isPropagateSuspension());
        assertEquals(3, policy.getMaxAuthenticationAttempts());
        assertEquals(1, policy.getRules().size());
        assertEquals(1, policy.getPassthroughResources().size());
    }
}
