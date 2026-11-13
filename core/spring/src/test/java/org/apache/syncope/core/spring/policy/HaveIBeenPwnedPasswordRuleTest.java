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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.HaveIBeenPwnedPasswordRuleConf;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class HaveIBeenPwnedPasswordRuleTest {

    private static final String SHA1_PASSWORD = "5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private Encryptor encryptor;

    private HaveIBeenPwnedPasswordRule rule;

    @BeforeEach
    void setUp() {
        encryptor = mock(Encryptor.class);

        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        rule = new HaveIBeenPwnedPasswordRule();
        rule.setConf(new HaveIBeenPwnedPasswordRuleConf());
        ReflectionTestUtils.setField(rule, "encryptorManager", encryptorManager);
    }

    private MockedConstruction<RestTemplate> pwnedService(final String body) {
        return mockConstruction(RestTemplate.class, (mock, context) -> when(mock.exchange(
                any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).
                thenReturn(new ResponseEntity<>(body, HttpStatus.OK)));
    }

    @Test
    void setConfRequiresExpectedType() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> rule.setConf(new DefaultPasswordRuleConf()));
        assertEquals(
                HaveIBeenPwnedPasswordRuleConf.class.getName() + " expected, got "
                + DefaultPasswordRuleConf.class.getName(),
                ex.getMessage());
    }

    @Test
    void pwnedPasswordIsRejected() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        try (MockedConstruction<RestTemplate> construction = pwnedService(
                "1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345\r\n"
                + "0000000000000000000000000000000000A:1")) {

            PasswordPolicyException ex =
                    assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "password"));
            assertEquals("Password pwned", ex.getMessage());
            assertEquals(1, construction.constructed().size());
        }
    }

    @Test
    void notPwnedPasswordIsAccepted() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        try (MockedConstruction<RestTemplate> construction = pwnedService(
                "0000000000000000000000000000000000A:1")) {

            assertDoesNotThrow(() -> rule.enforce("bob", "password"));
            assertEquals(1, construction.constructed().size());
        }
    }

    @Test
    void emptyResponseBodyIsAccepted() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        try (MockedConstruction<RestTemplate> construction = pwnedService("")) {
            assertDoesNotThrow(() -> rule.enforce("bob", "password"));
        }
    }

    @Test
    void httpErrorIsIgnored() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        try (MockedConstruction<RestTemplate> construction = mockConstruction(
                RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).
                        thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)))) {

            assertDoesNotThrow(() -> rule.enforce("bob", "password"));
        }
    }

    @Test
    void encodingFailureIsIgnored() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).
                thenThrow(new NoSuchAlgorithmException("boom"));

        try (MockedConstruction<RestTemplate> construction = pwnedService("")) {
            assertDoesNotThrow(() -> rule.enforce("bob", "password"));
            assertTrue(construction.constructed().isEmpty());
        }
    }

    @Test
    void nullClearPasswordIsIgnored() {
        assertDoesNotThrow(() -> rule.enforce("bob", null));

        User user = mock(User.class);
        assertDoesNotThrow(() -> rule.enforce(user, null));
    }

    @Test
    void linkedAccountWithoutPasswordIsIgnored() {
        LinkedAccount account = mock(LinkedAccount.class);

        assertDoesNotThrow(() -> rule.enforce(account));
    }

    @Test
    void linkedAccountWithDecodablePasswordIsChecked() throws Exception {
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).thenReturn("password");
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);

        try (MockedConstruction<RestTemplate> construction = pwnedService(
                "1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345")) {

            assertThrows(PasswordPolicyException.class, () -> rule.enforce(account));
        }
    }

    @Test
    void linkedAccountWithNonDecodablePasswordIsSkipped() throws Exception {
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(false);

        try (MockedConstruction<RestTemplate> construction = pwnedService("")) {
            assertDoesNotThrow(() -> rule.enforce(account));
            assertTrue(construction.constructed().isEmpty());
        }
    }

    @Test
    void linkedAccountDecodingFailureIsIgnored() throws Exception {
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).
                thenThrow(new NoSuchAlgorithmException("boom"));

        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);

        try (MockedConstruction<RestTemplate> construction = pwnedService("")) {
            assertDoesNotThrow(() -> rule.enforce(account));
            assertTrue(construction.constructed().isEmpty());
        }
    }
}
