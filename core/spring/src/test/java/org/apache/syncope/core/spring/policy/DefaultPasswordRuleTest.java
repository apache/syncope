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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DefaultPasswordRuleTest {

    private Encryptor encryptor;

    private DefaultPasswordRuleConf conf;

    private DefaultPasswordRule rule;

    @BeforeEach
    void setUp() {
        encryptor = mock(Encryptor.class);
        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        conf = new DefaultPasswordRuleConf();
        conf.setUsernameAllowed(true);

        rule = new DefaultPasswordRule();
        rule.setConf(conf);
        ReflectionTestUtils.setField(rule, "encryptorManager", encryptorManager);
    }

    @Test
    void setConfRequiresExpectedType() {
        PasswordRuleConf other = mock(PasswordRuleConf.class);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rule.setConf(other));

        assertTrue(ex.getMessage().startsWith(
                DefaultPasswordRuleConf.class.getName() + " expected, got "));
    }

    @Test
    void getConf() {
        assertSame(conf, rule.getConf());
    }

    @Test
    void nullPasswordIsIgnored() {
        assertDoesNotThrow(() -> rule.enforce("bob", null));

        User user = mock(User.class);
        assertDoesNotThrow(() -> rule.enforce(user, null));
        verify(user, never()).getUsername();
    }

    @Test
    void minimumLength() {
        conf.setMinLength(10);

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "short"));
        assertDoesNotThrow(() -> rule.enforce("bob", "longenough1"));
    }

    @Test
    void maximumLength() {
        conf.setMaxLength(5);

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "toolong"));
        assertDoesNotThrow(() -> rule.enforce("bob", "short"));
    }

    @Test
    void requiredCharacterClasses() {
        conf.setAlphabetical(1);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(1);
        conf.getSpecialChars().add('!');

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "abcdefgh"));
        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "ABCDEFGH1!"));
        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "Abcdefgh!"));
        assertDoesNotThrow(() -> rule.enforce("bob", "Abcdefg1!"));
    }

    @Test
    void illegalCharacters() {
        conf.getIllegalChars().add('$');

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "abc$def"));
        assertDoesNotThrow(() -> rule.enforce("bob", "abcdef"));
    }

    @Test
    void usernameNotAllowed() {
        conf.setUsernameAllowed(false);

        PasswordPolicyException ex =
                assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "bob"));
        assertTrue(ex.getMessage().contains("bob"));
        assertDoesNotThrow(() -> rule.enforce("bob", "alice"));
    }

    @Test
    void wordsNotPermitted() {
        conf.getWordsNotPermitted().add("password");

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "password"));
    }

    @Test
    void wordsNotPermittedAsSubstrings() {
        conf.getWordsNotPermitted().add("secret");
        conf.setNotPermittedAsSubstrings(true);

        assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "mysecretvalue"));
    }

    @Test
    void userSchemaValuesAreNotPermitted() {
        conf.getSchemasNotPermitted().add("mail");

        PlainAttr mail = mock(PlainAttr.class);
        when(mail.getValuesAsStrings()).thenReturn(List.of("admin@example.com"));

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("bob");
        when(user.getPlainAttr("mail")).thenReturn(Optional.of(mail));

        assertThrows(PasswordPolicyException.class, () -> rule.enforce(user, "admin@example.com"));
        assertDoesNotThrow(() -> rule.enforce(user, "other-value"));
    }

    @Test
    void linkedAccountPasswordIsDecodedAndEnforced() throws Exception {
        conf.setUsernameAllowed(false);
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getUsername()).thenReturn("bob");
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).thenReturn("bob");

        assertThrows(PasswordPolicyException.class, () -> rule.enforce(account));
    }

    @Test
    void linkedAccountWithoutPasswordIsIgnored() {
        LinkedAccount account = mock(LinkedAccount.class);

        assertDoesNotThrow(() -> rule.enforce(account));
        verify(account, never()).getUsername();
    }

    @Test
    void linkedAccountWithNonDecodablePasswordIsIgnored() {
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(false);

        assertDoesNotThrow(() -> rule.enforce(account));
        verify(account, never()).getUsername();
    }

    @Test
    void linkedAccountDecodingFailureIsIgnored() throws Exception {
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).
                thenThrow(new NoSuchAlgorithmException("boom"));

        assertDoesNotThrow(() -> rule.enforce(account));
        verify(account, never()).getUsername();
    }

}
