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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

class HaveIBeenPwnedPasswordRuleTest {

    private static final String SHA1_PASSWORD = "5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private Encryptor encryptor;

    private HttpClient httpClient;

    private HaveIBeenPwnedPasswordRule rule;

    @BeforeEach
    void setUp() {
        encryptor = mock(Encryptor.class);
        httpClient = mock(HttpClient.class);

        EncryptorManager encryptorManager = mock(EncryptorManager.class);
        when(encryptorManager.getInstance()).thenReturn(encryptor);

        rule = new HaveIBeenPwnedPasswordRule();
        rule.setConf(new HaveIBeenPwnedPasswordRuleConf());
        ReflectionTestUtils.setField(rule, "encryptorManager", encryptorManager);
        ReflectionTestUtils.setField(rule, "httpClient", httpClient);
    }

    private static HttpResponse<String> response(final int status, final String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static HttpResponse.BodyHandler<String> anyBodyHandler() {
        return any();
    }

    private void pwnedService(final String body) throws IOException, InterruptedException {
        pwnedService(200, body);
    }

    private void pwnedService(final int status, final String body) throws IOException, InterruptedException {
        HttpResponse<String> response = response(status, body);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(response);
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
        pwnedService("1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345\r\n"
                + "0000000000000000000000000000000000A:1");

        PasswordPolicyException ex =
                assertThrows(PasswordPolicyException.class, () -> rule.enforce("bob", "password"));
        assertEquals("Password pwned", ex.getMessage());
        verify(httpClient).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void notPwnedPasswordIsAccepted() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);
        pwnedService("0000000000000000000000000000000000A:1");

        assertDoesNotThrow(() -> rule.enforce("bob", "password"));
        verify(httpClient).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void emptyResponseBodyIsAccepted() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);
        pwnedService("");

        assertDoesNotThrow(() -> rule.enforce("bob", "password"));
    }

    @Test
    void httpErrorIsIgnored() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);
        pwnedService(500, "1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345");

        assertDoesNotThrow(() -> rule.enforce("bob", "password"));
    }

    @Test
    void networkFailureIsIgnored() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenThrow(new IOException("boom"));

        assertDoesNotThrow(() -> rule.enforce("bob", "password"));
    }

    @Test
    void encodingFailureIsIgnored() throws Exception {
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).
                thenThrow(new NoSuchAlgorithmException("boom"));

        assertDoesNotThrow(() -> rule.enforce("bob", "password"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void nullClearPasswordIsIgnored() {
        assertDoesNotThrow(() -> rule.enforce("bob", null));

        User user = mock(User.class);
        assertDoesNotThrow(() -> rule.enforce(user, null));
        verifyNoInteractions(httpClient);
    }

    @Test
    void linkedAccountWithoutPasswordIsIgnored() {
        LinkedAccount account = mock(LinkedAccount.class);

        assertDoesNotThrow(() -> rule.enforce(account));
        verifyNoInteractions(httpClient);
    }

    @Test
    void linkedAccountWithDecodablePasswordIsChecked() throws Exception {
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).thenReturn("password");
        when(encryptor.encode("password", CipherAlgorithm.SHA1)).thenReturn(SHA1_PASSWORD);

        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);

        pwnedService("1E4C9B93F3F0682250B6CF8331B7EE68FD8:12345");

        assertThrows(PasswordPolicyException.class, () -> rule.enforce(account));
        verify(httpClient).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void linkedAccountWithNonDecodablePasswordIsSkipped() throws Exception {
        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(false);

        assertDoesNotThrow(() -> rule.enforce(account));
        verifyNoInteractions(httpClient);
    }

    @Test
    void linkedAccountDecodingFailureIsIgnored() throws Exception {
        when(encryptor.decode("encoded", CipherAlgorithm.SHA512)).
                thenThrow(new NoSuchAlgorithmException("boom"));

        LinkedAccount account = mock(LinkedAccount.class);
        when(account.getPassword()).thenReturn("encoded");
        when(account.canDecodeSecrets()).thenReturn(true);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.SHA512);

        assertDoesNotThrow(() -> rule.enforce(account));
        verifyNoInteractions(httpClient);
    }
}
