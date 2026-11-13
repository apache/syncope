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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class XmlSecAlgorithmTest {

    @Test
    void algorithms() {
        assertEquals("http://www.w3.org/2001/04/xmlenc#tripledes-cbc", XmlSecAlgorithm.TRIPLEDES.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes128-cbc", XmlSecAlgorithm.AES_128.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes192-cbc", XmlSecAlgorithm.AES_192.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#aes256-cbc", XmlSecAlgorithm.AES_256.getAlgorithm());
        assertEquals("http://www.w3.org/2009/xmlenc11#aes128-gcm", XmlSecAlgorithm.AES_128_GCM.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-1_5", XmlSecAlgorithm.RSA_v1dot5.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p", XmlSecAlgorithm.RSA_OAEP.getAlgorithm());
        assertEquals("http://www.w3.org/2009/xmlenc11#rsa-oaep", XmlSecAlgorithm.RSA_OAEP_11.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#kw-aes128", XmlSecAlgorithm.AES_128_KeyWrap.getAlgorithm());
        assertEquals("http://www.w3.org/2000/09/xmldsig#sha1", XmlSecAlgorithm.SHA1.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#sha256", XmlSecAlgorithm.SHA256.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#sha512", XmlSecAlgorithm.SHA512.getAlgorithm());
        assertEquals("http://www.w3.org/2001/04/xmlenc#ripemd160", XmlSecAlgorithm.RIPEMD_160.getAlgorithm());
    }

    @Test
    void allAlgorithmsDefined() {
        Set<String> algorithms = new HashSet<>();

        for (XmlSecAlgorithm algorithm : XmlSecAlgorithm.values()) {
            assertTrue(algorithm.getAlgorithm().startsWith("http://"));
            algorithms.add(algorithm.getAlgorithm());
        }

        assertEquals(XmlSecAlgorithm.values().length, algorithms.size());
    }
}
