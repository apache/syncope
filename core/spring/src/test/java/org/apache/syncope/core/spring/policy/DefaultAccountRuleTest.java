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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultAccountRuleTest {

    private DefaultAccountRuleConf conf;

    private DefaultAccountRule rule;

    @BeforeEach
    void setUp() {
        conf = new DefaultAccountRuleConf();
        rule = new DefaultAccountRule();
        rule.setConf(conf);
    }

    @Test
    void setConfRequiresExpectedType() {
        AccountRuleConf other = mock(AccountRuleConf.class);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rule.setConf(other));
        assertTrue(ex.getMessage().startsWith(
                DefaultAccountRuleConf.class.getName() + " expected, got "));
    }

    @Test
    void validUsername() {
        assertDoesNotThrow(() -> rule.enforce("bob.smith"));
    }

    @Test
    void minLength() {
        conf.setMinLength(5);

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("bob"));
        assertEquals("Username too short", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("bobby"));
    }

    @Test
    void maxLength() {
        conf.setMaxLength(4);

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("bobby"));
        assertEquals("Username too long", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("bob"));
    }

    @Test
    void wordsNotPermittedAreCaseInsensitive() {
        conf.getWordsNotPermitted().add("Admin");

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("superadmin"));
        assertEquals("Used word(s) not permitted", ex.getMessage());
    }

    @Test
    void allUpperCase() {
        conf.setAllUpperCase(true);

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("Bob"));
        assertEquals("No lowercase characters permitted", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("BOB"));
    }

    @Test
    void allLowerCase() {
        conf.setAllLowerCase(true);

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("Bob"));
        assertEquals("No uppercase characters permitted", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("bob"));
    }

    @Test
    void defaultPattern() {
        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("bob!"));
        assertEquals("Username does not match pattern", ex.getMessage());
    }

    @Test
    void customPattern() {
        conf.setPattern("[a-z]+");

        assertThrows(AccountPolicyException.class, () -> rule.enforce("bob1"));
        assertDoesNotThrow(() -> rule.enforce("bob"));
    }

    @Test
    void prefixesNotPermitted() {
        conf.getPrefixesNotPermitted().add("sys");

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("sysadmin"));
        assertEquals("Prefix not permitted", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("admin"));
    }

    @Test
    void suffixesNotPermitted() {
        conf.getSuffixesNotPermitted().add("bot");

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce("mailbot"));
        assertEquals("Suffix not permitted", ex.getMessage());
        assertDoesNotThrow(() -> rule.enforce("mail"));
    }

    @Test
    void enforceUserIncludesSchemaValues() {
        conf.getSchemasNotPermitted().add("nickname");

        PlainAttr nickname = mock(PlainAttr.class);
        when(nickname.getValuesAsStrings()).thenReturn(List.of("BOB"));

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("bob");
        when(user.getPlainAttr("nickname")).thenReturn(Optional.of(nickname));

        AccountPolicyException ex =
                assertThrows(AccountPolicyException.class, () -> rule.enforce(user));
        assertEquals("Used word(s) not permitted", ex.getMessage());
    }

    @Test
    void enforceUserIgnoresMissingOrEmptySchemaValues() {
        conf.getSchemasNotPermitted().add("missing");
        conf.getSchemasNotPermitted().add("empty");

        PlainAttr empty = mock(PlainAttr.class);
        when(empty.getValuesAsStrings()).thenReturn(List.of());

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("bob");
        when(user.getPlainAttr("missing")).thenReturn(Optional.empty());
        when(user.getPlainAttr("empty")).thenReturn(Optional.of(empty));

        assertDoesNotThrow(() -> rule.enforce(user));
    }

    @Test
    void enforceLinkedAccountWithBlankUsernameIsSkipped() {
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getUsername()).thenReturn(" ");

        assertDoesNotThrow(() -> rule.enforce(account));
    }

    @Test
    void enforceLinkedAccountIncludesSchemaValues() {
        conf.getSchemasNotPermitted().add("mail");

        PlainAttr mail = mock(PlainAttr.class);
        when(mail.getValuesAsStrings()).thenReturn(List.of("BOB"));

        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getUsername()).thenReturn("bob");
        when(account.getPlainAttr("mail")).thenReturn(Optional.of(mail));

        assertThrows(AccountPolicyException.class, () -> rule.enforce(account));
    }
}
