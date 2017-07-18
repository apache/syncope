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

import org.apache.syncope.core.spring.security.Encryptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.junit.Test;

/**
 * Test class to test all encryption algorithms.
 */
public class EncryptorTest {

    private final String password = "password";

    private final Encryptor encryptor = Encryptor.getInstance();

    /**
     * Verify all algorithms.
     */
    @Test
    public void testEncoder() throws Exception {
        for (CipherAlgorithm cipherAlgorithm : CipherAlgorithm.values()) {
            final String encPassword = encryptor.encode(password, cipherAlgorithm);

            assertNotNull(encPassword);
            assertTrue(encryptor.verify(password, cipherAlgorithm, encPassword));
            assertFalse(encryptor.verify("pass", cipherAlgorithm, encPassword));

            // check that same password encoded with BCRYPT or Salted versions results in different digest
            if (cipherAlgorithm.equals(CipherAlgorithm.BCRYPT) || cipherAlgorithm.getAlgorithm().startsWith("S-")) {
                final String encSamePassword = encryptor.encode(password, cipherAlgorithm);
                assertNotNull(encSamePassword);
                assertFalse(encSamePassword.equals(encPassword));
                assertTrue(encryptor.verify(password, cipherAlgorithm, encSamePassword));
            }
        }
    }

    @Test
    public void testDecodeDefaultAESKey() throws Exception {
        String decPassword = encryptor.decode("9Pav+xl+UyHt02H9ZBytiA==", CipherAlgorithm.AES);
        assertEquals(password, decPassword);
    }

    @Test
    public void testSmallKey() throws Exception {
        Encryptor smallKeyEncryptor = Encryptor.getInstance("123");
        String encPassword = smallKeyEncryptor.encode(password, CipherAlgorithm.AES);
        String decPassword = smallKeyEncryptor.decode(encPassword, CipherAlgorithm.AES);
        assertEquals(password, decPassword);
    }

    @Test
    public void testSaltedHash() throws Exception {
        String encPassword = encryptor.encode(password, CipherAlgorithm.SSHA256);
        // System.out.println("ENC: " + encPassword);
        assertNotNull(encPassword);

        assertTrue(encryptor.verify(password, CipherAlgorithm.SSHA256, encPassword));
    }
}
