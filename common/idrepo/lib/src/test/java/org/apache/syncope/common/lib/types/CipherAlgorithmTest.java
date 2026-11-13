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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CipherAlgorithmTest {

    @Test
    void algorithm() {
        assertEquals("SHA-1", CipherAlgorithm.SHA.getAlgorithm());
        assertEquals("SHA-1", CipherAlgorithm.SHA1.getAlgorithm());
        assertEquals("SHA-256", CipherAlgorithm.SHA256.getAlgorithm());
        assertEquals("SHA-512", CipherAlgorithm.SHA512.getAlgorithm());
        assertEquals("AES", CipherAlgorithm.AES.getAlgorithm());
        assertEquals("S-MD5", CipherAlgorithm.SMD5.getAlgorithm());
        assertEquals("S-SHA-1", CipherAlgorithm.SSHA.getAlgorithm());
        assertEquals("S-SHA-256", CipherAlgorithm.SSHA256.getAlgorithm());
        assertEquals("S-SHA-512", CipherAlgorithm.SSHA512.getAlgorithm());
        assertEquals("BCRYPT", CipherAlgorithm.BCRYPT.getAlgorithm());
    }

    @Test
    void invertible() {
        assertFalse(CipherAlgorithm.SHA.isInvertible());
        assertTrue(CipherAlgorithm.AES.isInvertible());
        assertFalse(CipherAlgorithm.BCRYPT.isInvertible());
    }

    @Test
    void salted() {
        assertFalse(CipherAlgorithm.SHA.isSalted());
        assertFalse(CipherAlgorithm.AES.isSalted());
        assertTrue(CipherAlgorithm.SMD5.isSalted());
        assertTrue(CipherAlgorithm.SSHA.isSalted());
        assertTrue(CipherAlgorithm.SSHA1.isSalted());
        assertTrue(CipherAlgorithm.SSHA256.isSalted());
        assertTrue(CipherAlgorithm.SSHA512.isSalted());
    }
}
