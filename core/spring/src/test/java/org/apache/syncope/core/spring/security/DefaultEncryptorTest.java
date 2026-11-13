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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.List;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;

class DefaultEncryptorTest {

    private static final String PASSWORD_VALUE = "password";

    private static Encryptor ENCRYPTOR;

    @BeforeAll
    public static void setUp() throws IOException {
        SecurityProperties props = new SecurityProperties();
        props.setAesSecretKey(SpringTestConfiguration.AES_SECRET_KEY);

        DefaultListableBeanFactory beanFactory = ApplicationContextProvider.getBeanFactory();
        if (beanFactory == null) {
            StaticApplicationContext context = new StaticApplicationContext();
            context.refresh();
            ApplicationContextProvider.setApplicationContext(context);
            beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
            ApplicationContextProvider.setBeanFactory(beanFactory);
        }
        if (!beanFactory.containsSingleton("securityProperties")) {
            beanFactory.registerSingleton("securityProperties", props);
        }

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAesSecretKey(SpringTestConfiguration.AES_SECRET_KEY);

        ENCRYPTOR = new DefaultEncryptorManager(
                new DefaultCredentialChecker("", "", "", "", false), securityProperties).getInstance();
    }

    @Test
    void encoder() throws Exception {
        for (CipherAlgorithm cipherAlgorithm : CipherAlgorithm.values()) {
            String encPassword = ENCRYPTOR.encode(PASSWORD_VALUE, cipherAlgorithm);

            assertNotNull(encPassword);
            assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, cipherAlgorithm, encPassword));
            assertFalse(ENCRYPTOR.verify(PASSWORD_VALUE + "diff", cipherAlgorithm, encPassword));

            // check that same password encoded with BCRYPT or Salted versions results in different digest
            if (cipherAlgorithm == CipherAlgorithm.BCRYPT || cipherAlgorithm.isSalted()) {
                String encSamePassword = ENCRYPTOR.encode(PASSWORD_VALUE, cipherAlgorithm);
                assertNotNull(encSamePassword);
                assertFalse(encSamePassword.equals(encPassword));
                assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, cipherAlgorithm, encSamePassword));
            }
        }
    }

    @Test
    void decodeDefaultAESKey() throws Exception {
        String decPassword = ENCRYPTOR.decode("9Pav+xl+UyHt02H9ZBytiA==", CipherAlgorithm.AES);
        assertEquals(PASSWORD_VALUE, decPassword);
    }

    @Test
    void smallKey() throws Exception {
        DefaultEncryptor prodModeEncryptor = new DefaultEncryptor("123", true, new SecurityProperties().getDigester());
        assertThrows(InvalidKeyException.class, () -> prodModeEncryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES));

        DefaultEncryptor smallKeyEncryptor = new DefaultEncryptor("123", false, new SecurityProperties().getDigester());
        String encPassword = smallKeyEncryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES);
        String decPassword = smallKeyEncryptor.decode(encPassword, CipherAlgorithm.AES);
        assertEquals(PASSWORD_VALUE, decPassword);
    }

    @Test
    void saltedHash() throws Exception {
        String encPassword = ENCRYPTOR.encode(PASSWORD_VALUE, CipherAlgorithm.SSHA256);
        assertNotNull(encPassword);

        assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.SSHA256, encPassword));
    }

    @Test
    void verifySaltedFromExternal() throws Exception {
        // generated via https://github.com/peppelinux/pySSHA-slapd with command:
        // python3 pySSHA/ssha.py -p password -enc sha256 -s 666ac543 \
        //  | sed 's/{.*}//' | xargs echo -n | base64 -d | xxd -p | tr -d $'\n'  | xargs echo
        String encPassword = "b098017d584647e3fa1f3e0eb437648aefa84093c15e0d3efb752a4183cfdcf3666ac543";
        assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.SSHA256, encPassword));
    }

    @Test
    void encodeNullValue() throws Exception {
        assertNull(ENCRYPTOR.encode(null, CipherAlgorithm.AES));
        assertNull(ENCRYPTOR.encode(null, CipherAlgorithm.SHA256));
        assertNull(ENCRYPTOR.encode(null, null));
    }

    @Test
    void encodeNullCipherAlgorithmUsesAES() throws Exception {
        String encPassword = ENCRYPTOR.encode(PASSWORD_VALUE, null);

        assertNotNull(encPassword);
        assertEquals(PASSWORD_VALUE, ENCRYPTOR.decode(encPassword, CipherAlgorithm.AES));
        assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, null, encPassword));
        assertFalse(ENCRYPTOR.verify(PASSWORD_VALUE + "diff", null, encPassword));
    }

    @Test
    void decodeNullEncodedAndNonAesAlgorithm() throws Exception {
        assertNull(ENCRYPTOR.decode(null, CipherAlgorithm.AES));
        assertNull(ENCRYPTOR.decode("whatever", CipherAlgorithm.SHA256));
        assertNull(ENCRYPTOR.decode(null, CipherAlgorithm.SHA256));
    }

    @Test
    void verifyNullValue() throws Exception {
        String encPassword = ENCRYPTOR.encode(PASSWORD_VALUE, CipherAlgorithm.AES);

        assertFalse(ENCRYPTOR.verify(null, CipherAlgorithm.AES, encPassword));
        assertFalse(ENCRYPTOR.verify(null, CipherAlgorithm.SHA256, encPassword));
        assertFalse(ENCRYPTOR.verify(null, CipherAlgorithm.BCRYPT, encPassword));
    }

    @Test
    void verifyMalformedEncodedValue() {
        assertFalse(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.BCRYPT, "not-a-bcrypt-hash"));
        assertFalse(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.AES, null));
    }

    @Test
    void aesNotConfigured() {
        DefaultEncryptor nullKeyEncryptor = new DefaultEncryptor(
                null, true, new SecurityProperties().getDigester());
        assertThrows(IllegalArgumentException.class,
                () -> nullKeyEncryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES));
        assertThrows(IllegalArgumentException.class,
                () -> nullKeyEncryptor.decode("AAAA", CipherAlgorithm.AES));
        assertFalse(nullKeyEncryptor.verify(PASSWORD_VALUE, CipherAlgorithm.AES, "AAAA"));

        DefaultEncryptor blankKeyEncryptor = new DefaultEncryptor(
                "   ", true, new SecurityProperties().getDigester());
        assertThrows(IllegalArgumentException.class,
                () -> blankKeyEncryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES));
        assertThrows(IllegalArgumentException.class,
                () -> blankKeyEncryptor.decode("AAAA", CipherAlgorithm.AES));
    }

    @Test
    void nonProductionAesKeyIsNormalized() throws Exception {
        List<String> keys = List.of(
                "1234567890",
                "1234567890123456",
                "12345678901234567890",
                "123456789012345678901234",
                "123456789012345678901234567890",
                "12345678901234567890123456789012",
                "1234567890123456789012345678901234567890");

        for (String key : keys) {
            DefaultEncryptor encryptor = new DefaultEncryptor(
                    key, false, new SecurityProperties().getDigester());

            String encPassword = encryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES);
            assertEquals(PASSWORD_VALUE, encryptor.decode(encPassword, CipherAlgorithm.AES));
        }
    }

    @Test
    void productionModeDoesNotNormalizeAesKey() throws Exception {
        DefaultEncryptor encryptor = new DefaultEncryptor(
                "1234567890123456", true, new SecurityProperties().getDigester());

        String encPassword = encryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES);
        assertEquals(PASSWORD_VALUE, encryptor.decode(encPassword, CipherAlgorithm.AES));
        assertThrows(InvalidKeyException.class,
                () -> new DefaultEncryptor("12345678901234567890", true, new SecurityProperties().getDigester()).
                        encode(PASSWORD_VALUE, CipherAlgorithm.AES));
    }
}
