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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.util.List;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MfaLogicTest {

    private EncryptorManager encryptorManager;

    private Encryptor encryptor;

    private SecretGenerator secretGenerator;

    private QrGenerator qrGenerator;

    private RecoveryCodeGenerator recoveryCodeGenerator;

    private CodeVerifier codeVerifier;

    private SecurityProperties securityProperties;

    private MfaLogic logic;

    @BeforeEach
    void setUp() {
        encryptorManager = mock(EncryptorManager.class);
        encryptor = mock(Encryptor.class);
        secretGenerator = mock(SecretGenerator.class);
        qrGenerator = mock(QrGenerator.class);
        recoveryCodeGenerator = mock(RecoveryCodeGenerator.class);
        codeVerifier = mock(CodeVerifier.class);

        securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");

        when(encryptorManager.getInstance()).thenReturn(encryptor);

        logic = new MfaLogic(
                mock(UserDataBinder.class),
                mock(UserDAO.class),
                encryptorManager,
                mock(DomainOps.class),
                secretGenerator,
                qrGenerator,
                HashingAlgorithm.SHA1,
                recoveryCodeGenerator,
                codeVerifier,
                securityProperties);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void generate() throws Exception {
        when(secretGenerator.generate()).thenReturn("secret");
        when(qrGenerator.generate(any(QrData.class))).thenReturn(new byte[] { 1 });
        when(qrGenerator.getImageMimeType()).thenReturn("image/png");
        when(recoveryCodeGenerator.generateCodes(10)).thenReturn(new String[] { "code" });

        Mfa mfa = logic.generate("user");

        assertEquals("secret", mfa.secret());
        assertEquals(List.of("code"), mfa.recoveryCodes());
        assertTrue(mfa.dataUri().startsWith("data:image/png;base64,"));
    }

    @Test
    void generateForAdmin() throws Exception {
        when(secretGenerator.generate()).thenReturn("secret");
        when(qrGenerator.generate(any(QrData.class))).thenReturn(new byte[] { 1 });
        when(qrGenerator.getImageMimeType()).thenReturn("image/png");

        assertEquals(List.of(), logic.generate("admin").recoveryCodes());
    }

    @Test
    void checkWithoutValues() {
        assertThrows(SyncopeClientException.class, () -> logic.check(new MfaCheck(null, "otp")));
        assertThrows(SyncopeClientException.class, () -> logic.check(new MfaCheck("secret", null)));
    }

    @Test
    void check() {
        when(codeVerifier.isValidCode("secret", "otp")).thenReturn(true);

        assertTrue(logic.check(new MfaCheck("secret", "otp")));
    }

    @Test
    void enrollAdminWithExistingSecret() throws Exception {
        LogicTestAuth.authenticate("admin");
        securityProperties.setAdminMfaSecret("existing");
        when(encryptor.encode(any(), any())).thenReturn("encrypted");

        assertThrows(SyncopeClientException.class, () -> logic.enroll(new Mfa("secret", "uri", List.of())));
    }

    @Test
    void enrollAdmin() throws Exception {
        LogicTestAuth.authenticate("admin");
        when(encryptor.encode(any(), any())).thenReturn("encrypted");

        logic.enroll(new Mfa("secret", "uri", List.of()));

        assertEquals("encrypted", securityProperties.getAdminMfaSecret());
    }

    @Test
    void enrolledAdmin() {
        LogicTestAuth.authenticate("admin");

        assertFalse(logic.enrolled("admin"));

        securityProperties.setAdminMfaSecret("encrypted");
        assertTrue(logic.enrolled("admin"));
    }

    @Test
    void dismissAdminOnMaster() {
        LogicTestAuth.authenticate("admin");

        assertThrows(SyncopeClientException.class, () -> logic.dismiss());
    }
}
