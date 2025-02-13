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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DefaultEncryptorTest {

    private static final String PASSWORD_VALUE = "password";

    private static Encryptor ENCRYPTOR;

    @BeforeAll
    public static void setUp() {
        ApplicationContextProvider.getBeanFactory().registerSingleton("securityProperties", new SecurityProperties());
        ENCRYPTOR = new DefaultEncryptorManager().getInstance();
    }

    @Test
    public void encoder() throws Exception {
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
    public void decodeDefaultAESKey() throws Exception {
        String decPassword = ENCRYPTOR.decode("9Pav+xl+UyHt02H9ZBytiA==", CipherAlgorithm.AES);
        assertEquals(PASSWORD_VALUE, decPassword);
    }

    @Test
    public void smallKey() throws Exception {
        DefaultEncryptor smallKeyEncryptor = new DefaultEncryptor("123");
        String encPassword = smallKeyEncryptor.encode(PASSWORD_VALUE, CipherAlgorithm.AES);
        String decPassword = smallKeyEncryptor.decode(encPassword, CipherAlgorithm.AES);
        assertEquals(PASSWORD_VALUE, decPassword);
    }

    @Test
    public void saltedHash() throws Exception {
        String encPassword = ENCRYPTOR.encode(PASSWORD_VALUE, CipherAlgorithm.SSHA256);
        assertNotNull(encPassword);

        assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.SSHA256, encPassword));
    }

    @Test
    public void verifySaltedFromExternal() throws Exception {
        // generated via https://github.com/peppelinux/pySSHA-slapd with command:
        // python3 pySSHA/ssha.py -p password -enc sha256 -s 666ac543 \
        //  | sed 's/{.*}//' | xargs echo -n | base64 -d | xxd -p | tr -d $'\n'  | xargs echo
        String encPassword = "b098017d584647e3fa1f3e0eb437648aefa84093c15e0d3efb752a4183cfdcf3666ac543";
        assertTrue(ENCRYPTOR.verify(PASSWORD_VALUE, CipherAlgorithm.SSHA256, encPassword));
    }
}
