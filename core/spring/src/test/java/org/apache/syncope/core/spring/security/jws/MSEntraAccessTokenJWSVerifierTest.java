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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import javax.cache.Caching;
import javax.cache.expiry.Duration;
import org.junit.jupiter.api.Test;

public class MSEntraAccessTokenJWSVerifierTest extends AbstractTest {

    private static class TestMSEntraAccessTokenJWSVerifier extends MSEntraJWSVerifierCacheLoader {

        private final String jwksUri;

        private final String oidc;

        private final String jwks;

        TestMSEntraAccessTokenJWSVerifier(final String jwksUri, final String oidc, final String jwks) {
            super(null, null);

            this.jwksUri = jwksUri;
            this.oidc = oidc;
            this.jwks = jwks;
        }

        @Override
        protected String fetchDocument(final String url) {
            return url.equals(jwksUri) ? jwks : oidc;
        }
    }

    @Test
    void supportedJWSAlgorithmsEmpty() {
        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": []}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        assertTrue(v.supportedJWSAlgorithms().isEmpty());
    }

    @Test
    void supportedJWSAlgorithmsRSA() throws Exception {
        JWK jwk = generateJWKRSA();
        String[] chunks = createSignedJWT(jwk).split("\\.");

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        assertTrue(v.verify(
                JWSHeader.parse(new Base64URL(chunks[0])),
                (chunks[0] + "." + chunks[1]).getBytes(),
                new Base64URL(chunks[2])));
        assertTrue(v.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));
        assertDoesNotThrow(v::getJCAContext);
    }

    @Test
    void supportedJWSAlgorithmsRSAJWSAlgorithm() throws Exception {
        JWK jwk = generateJWKRSA();

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        assertTrue(v.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));
    }

    @Test
    void supportedJWSAlgorithmsRSAJCAContext() throws NoSuchAlgorithmException, JOSEException {
        JWK jwk = generateJWKRSA();

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        assertDoesNotThrow(v::getJCAContext);
    }

    @Test
    void supportedJWSAlgorithmsEC() throws Exception {
        JWK jwk = generateJWKEC();
        String[] chunks = createSignedJWT(jwk).split("\\.");

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        assertTrue(v.verify(
                JWSHeader.parse(new Base64URL(chunks[0])),
                (chunks[0] + "." + chunks[1]).getBytes(),
                new Base64URL(chunks[2])
        ));
        assertTrue(v.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));
        assertDoesNotThrow(v::getJCAContext);
    }

    @Test
    void supportedJWSAlgorithmsMixed() throws Exception {
        JWK jwkRSA = generateJWKRSA();
        JWK jwkEC = generateJWKEC();
        String[] chunksRSA = createSignedJWT(jwkRSA).split("\\.");
        String[] chunksEC = createSignedJWT(jwkEC).split("\\.");

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": ["
                + jwkRSA.toPublicJWK().toJSONString() + ","
                + jwkEC.toPublicJWK().toJSONString()
                + "]}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                Caching.getCachingProvider().getCacheManager(),
                new TestMSEntraAccessTokenJWSVerifier(jwksUri, oidc, jwks),
                Duration.ETERNAL);

        // Verify with RSA
        assertTrue(v.verify(
                JWSHeader.parse(new Base64URL(chunksRSA[0])),
                (chunksRSA[0] + "." + chunksRSA[1]).getBytes(),
                new Base64URL(chunksRSA[2])));

        // Verify with EC
        assertTrue(v.verify(
                JWSHeader.parse(new Base64URL(chunksEC[0])),
                (chunksEC[0] + "." + chunksEC[1]).getBytes(),
                new Base64URL(chunksEC[2])));

        Set.of((JWSAlgorithm) jwkRSA.getAlgorithm(), (JWSAlgorithm) jwkEC.getAlgorithm()).
                forEach(jwsAlgorithm -> assertTrue(v.supportedJWSAlgorithms().contains(jwsAlgorithm)));

        assertDoesNotThrow(v::getJCAContext);
    }
}
