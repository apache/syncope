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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class DefaultCredentialCheckerTest {

    private static final String DEFAULT_AES_KEY = "1abcdefghilmnopqrstuvz2!";

    private static final String DEFAULT_JWS_KEY =
            "ZW7pRixehFuNUtnY5Se47IemgMryTzazPPJ9CGX5LTCmsOJpOgHAQEuPQeV9A28f";

    private static final String DEFAULT_ADMIN_PASSWORD =
            "DE088591C00CC98B36F5ADAAF7DA2B004CF7F2FE7BBB45B766B6409876E2F3DB13C7905C6AA59464";

    private static final String DEFAULT_ANONYMOUS_KEY = "anonymousKey";

    private static DefaultCredentialChecker withDefaultCredentials(final boolean productionMode) throws IOException {
        return new DefaultCredentialChecker(
                DEFAULT_AES_KEY,
                DEFAULT_JWS_KEY,
                DEFAULT_ADMIN_PASSWORD,
                DEFAULT_ANONYMOUS_KEY,
                productionMode);
    }

    @Test
    void defaultAesKeyRejectedInProductionMode() throws IOException {
        DefaultCredentialChecker checker = withDefaultCredentials(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, checker::checkIsDefaultAESKeyInUse);
        assertTrue(ex.getMessage().contains("default AES key"));
    }

    @Test
    void defaultJwsKeyRejectedInProductionMode() throws IOException {
        DefaultCredentialChecker checker = withDefaultCredentials(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, checker::checkIsDefaultJWSKeyInUse);
        assertTrue(ex.getMessage().contains("default JWKS key"));
    }

    @Test
    void defaultAdminPasswordRejectedInProductionMode() throws IOException {
        DefaultCredentialChecker checker = withDefaultCredentials(true);

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, checker::checkIsDefaultAdminPasswordInUse);
        assertTrue(ex.getMessage().contains("default adminPassword"));
    }

    @Test
    void defaultAnonymousKeyRejectedInProductionMode() throws IOException {
        DefaultCredentialChecker checker = withDefaultCredentials(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, checker::checkIsDefaultAnonymousKeyInUse);
        assertTrue(ex.getMessage().contains("default anonymousKey"));
    }

    @Test
    void defaultCredentialsAcceptedOutsideProductionMode() throws IOException {
        DefaultCredentialChecker checker = withDefaultCredentials(false);

        assertDoesNotThrow(checker::checkIsDefaultAESKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultJWSKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultAdminPasswordInUse);
        assertDoesNotThrow(checker::checkIsDefaultAnonymousKeyInUse);
    }

    @Test
    void customCredentialsAcceptedInProductionMode() throws IOException {
        DefaultCredentialChecker checker = new DefaultCredentialChecker(
                "custom-aes", "custom-jws", "custom-admin", "custom-anonymous", true);

        assertDoesNotThrow(checker::checkIsDefaultAESKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultJWSKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultAdminPasswordInUse);
        assertDoesNotThrow(checker::checkIsDefaultAnonymousKeyInUse);
    }

    @Test
    void emptyCredentialsAreNotConsideredDefault() throws IOException {
        DefaultCredentialChecker checker = new DefaultCredentialChecker("", "", "", "", true);

        assertDoesNotThrow(checker::checkIsDefaultAESKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultJWSKeyInUse);
        assertDoesNotThrow(checker::checkIsDefaultAdminPasswordInUse);
        assertDoesNotThrow(checker::checkIsDefaultAnonymousKeyInUse);
    }
}
