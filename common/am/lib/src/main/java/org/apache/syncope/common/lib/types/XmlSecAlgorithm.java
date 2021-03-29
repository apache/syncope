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

public enum XmlSecAlgorithm {

    /**
     * Triple DES EDE (192 bit key) in CBC mode
     */
    TRIPLEDES("http://www.w3.org/2001/04/xmlenc#tripledes-cbc"),

    /**
     * AES 128 Cipher
     */
    AES_128("http://www.w3.org/2001/04/xmlenc#aes128-cbc"),

    /**
     * AES 256 Cipher
     */
    AES_256("http://www.w3.org/2001/04/xmlenc#aes256-cbc"),

    /**
     * AES 192 Cipher
     */
    AES_192("http://www.w3.org/2001/04/xmlenc#aes192-cbc"),

    /**
     * AES 128 GCM Cipher
     */
    AES_128_GCM("http://www.w3.org/2009/xmlenc11#aes128-gcm"),

    /**
     * AES 192 GCM Cipher
     */
    AES_192_GCM("http://www.w3.org/2009/xmlenc11#aes192-gcm"),

    /**
     * AES 256 GCM Cipher
     */
    AES_256_GCM("http://www.w3.org/2009/xmlenc11#aes256-gcm"),

    /**
     * SEED 128 Cipher
     */
    SEED_128("http://www.w3.org/2007/05/xmldsig-more#seed128-cbc"),

    /**
     * CAMELLIA 128 Cipher
     */
    CAMELLIA_128("http://www.w3.org/2001/04/xmldsig-more#camellia128-cbc"),

    /**
     * CAMELLIA 192 Cipher
     */
    CAMELLIA_192("http://www.w3.org/2001/04/xmldsig-more#camellia192-cbc"),

    /**
     * CAMELLIA 256 Cipher
     */
    CAMELLIA_256("http://www.w3.org/2001/04/xmldsig-more#camellia256-cbc"),

    /**
     * RSA 1.5 Cipher
     */
    RSA_v1dot5("http://www.w3.org/2001/04/xmlenc#rsa-1_5"),

    /**
     * RSA OAEP Cipher
     */
    RSA_OAEP("http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"),

    /**
     * RSA OAEP Cipher
     */
    RSA_OAEP_11("http://www.w3.org/2009/xmlenc11#rsa-oaep"),

    /**
     * DIFFIE_HELLMAN Cipher
     */
    DIFFIE_HELLMAN("http://www.w3.org/2001/04/xmlenc#dh"),

    /**
     * Triple DES EDE (192 bit key) in CBC mode KEYWRAP
     */
    TRIPLEDES_KeyWrap("http://www.w3.org/2001/04/xmlenc#kw-tripledes"),

    /**
     * AES 128 Cipher KeyWrap
     */
    AES_128_KeyWrap("http://www.w3.org/2001/04/xmlenc#kw-aes128"),

    /**
     * AES 256 Cipher KeyWrap
     */
    AES_256_KeyWrap("http://www.w3.org/2001/04/xmlenc#kw-aes256"),

    /**
     * AES 192 Cipher KeyWrap
     */
    AES_192_KeyWrap("http://www.w3.org/2001/04/xmlenc#kw-aes192"),

    /**
     * CAMELLIA 128 Cipher KeyWrap
     */
    CAMELLIA_128_KeyWrap("http://www.w3.org/2001/04/xmldsig-more#kw-camellia128"),

    /**
     * CAMELLIA 192 Cipher KeyWrap
     */
    CAMELLIA_192_KeyWrap("http://www.w3.org/2001/04/xmldsig-more#kw-camellia192"),

    /**
     * CAMELLIA 256 Cipher KeyWrap
     */
    CAMELLIA_256_KeyWrap("http://www.w3.org/2001/04/xmldsig-more#kw-camellia256"),

    /**
     * SEED 128 Cipher KeyWrap
     */
    SEED_128_KeyWrap("http://www.w3.org/2007/05/xmldsig-more#kw-seed128"),

    /**
     * SHA1 Cipher
     */
    SHA1("http://www.w3.org/2000/09/xmldsig#sha1"),

    /**
     * SHA256 Cipher
     */
    SHA256("http://www.w3.org/2001/04/xmlenc#sha256"),

    /**
     * SHA512 Cipher
     */
    SHA512("http://www.w3.org/2001/04/xmlenc#sha512"),

    /**
     * RIPEMD Cipher
     */
    RIPEMD_160("http://www.w3.org/2001/04/xmlenc#ripemd160");

    private final String algorithm;

    XmlSecAlgorithm(final String uri) {
        this.algorithm = uri;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
