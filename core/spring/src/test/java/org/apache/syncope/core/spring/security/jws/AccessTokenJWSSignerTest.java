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
package org.apache.syncope.core.spring.security.jws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AccessTokenJWSSignerTest {

    private static final String HMAC_KEY =
            "0123456789012345678901234567890123456789012345678901234567890123456789";

    private static String keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + ':' + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @Test
    void signWithHmac() throws Exception {
        AccessTokenJWSSigner signer = new AccessTokenJWSSigner(JWSAlgorithm.HS512, HMAC_KEY);

        assertEquals(JWSAlgorithm.HS512, signer.getJwsAlgorithm());
        assertTrue(signer.supportedJWSAlgorithms().contains(JWSAlgorithm.HS512));
        assertNotNull(signer.getJCAContext());

        Base64URL signature = signer.sign(
                new JWSHeader(JWSAlgorithm.HS512),
                "payload".getBytes(StandardCharsets.UTF_8));
        assertNotNull(signature);
        assertTrue(signature.toString().length() > 0);
    }

    @Test
    void signWithRsa() throws Exception {
        AccessTokenJWSSigner signer = new AccessTokenJWSSigner(JWSAlgorithm.RS256, keyPair());

        assertTrue(signer.supportedJWSAlgorithms().contains(JWSAlgorithm.RS256));
        assertNotNull(signer.sign(
                new JWSHeader(JWSAlgorithm.RS256),
                "payload".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rsaRequiresKeyPair() throws Exception {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new AccessTokenJWSSigner(JWSAlgorithm.RS256, HMAC_KEY));
        assertEquals("A key pair is required, in the 'private:public' format", ex.getMessage());
    }

    @Test
    void unsupportedAlgorithm() throws Exception {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new AccessTokenJWSSigner(JWSAlgorithm.ES256, HMAC_KEY));
        assertEquals("Unsupported JWS algorithm: ES256", ex.getMessage());
    }
}
