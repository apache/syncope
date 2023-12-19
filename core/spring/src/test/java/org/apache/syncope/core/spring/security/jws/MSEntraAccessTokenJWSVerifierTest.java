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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MSEntraAccessTokenJWSVerifierTest {

    private static class SpyableMSEntraAccessTokenJWSVerifier extends MSEntraAccessTokenJWSVerifier {

        SpyableMSEntraAccessTokenJWSVerifier() {
            super(null, null, Duration.ofHours(24));
        }
    }

    private static final String TENANT_ID = "test-tenant-id";

    private static final String APP_ID = "test-app-id";

    private static String createSignedJWT(final JWK jwk) throws JOSEException {
        // Create JWT header
        JWSHeader header = new JWSHeader.Builder((JWSAlgorithm) jwk.getAlgorithm())
                .type(JOSEObjectType.JWT)
                .keyID(jwk.getKeyID())
                .build();

        // Create JWT payload
        JWTClaimsSet payload = new JWTClaimsSet.Builder()
                .issuer(TENANT_ID)
                .audience(APP_ID)
                .build();

        // Create signed JWT
        SignedJWT signedJWT = new SignedJWT(header, payload);

        JWSSigner signer = jwk.getAlgorithm() == JWSAlgorithm.RS256
                ? new RSASSASigner(jwk.toRSAKey())
                : new ECDSASigner(jwk.toECKey());

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static MSEntraAccessTokenJWSVerifier getSpyInstance(
            final String jwksUri, final String oidc, final String jwks) {

        MSEntraAccessTokenJWSVerifier v = spy(SpyableMSEntraAccessTokenJWSVerifier.class);
        doAnswer(m -> m.getArgument(0).equals(jwksUri) ? jwks : oidc).when(v).fetchDocument(anyString());
        return v;
    }

    private static JWK generateJWKRSA() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        // Convert to JWK format
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .build();
    }

    private static JWK generateJWKEC() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        // Generate EC key pair with P-256 curve
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(Curve.P_256.toECParameterSpec());
        KeyPair keyPair = gen.generateKeyPair();

        // Convert to JWK format
        return new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .algorithm(JWSAlgorithm.ES256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .build();
    }

    @Test
    void getOpenIDMetadataDocumentUrl() {
        // Tenant id and app id
        MSEntraAccessTokenJWSVerifier v1 = new MSEntraAccessTokenJWSVerifier(TENANT_ID, APP_ID, Duration.ofHours(24));
        assertEquals(String.format(
                "https://login.microsoftonline.com/%s/.well-known/openid-configuration?appid=%s", TENANT_ID, APP_ID),
                v1.getOpenIDMetadataDocumentUrl());

        // Tenant id, no app id
        MSEntraAccessTokenJWSVerifier v2 = new MSEntraAccessTokenJWSVerifier(TENANT_ID, null, Duration.ofHours(24));
        assertEquals(
                String.format("https://login.microsoftonline.com/%s/.well-known/openid-configuration", TENANT_ID),
                v2.getOpenIDMetadataDocumentUrl());

        // No tenant id, no app id
        MSEntraAccessTokenJWSVerifier v3 = new MSEntraAccessTokenJWSVerifier(null, null, Duration.ofHours(24));
        assertEquals(
                "https://login.microsoftonline.com/common/.well-known/openid-configuration",
                v3.getOpenIDMetadataDocumentUrl());
    }

    @Test
    void extractJwksUri() {
        String doc = "{\"jwks_uri\": \"https://login.microsoftonline.com/common/discovery/keys\"}";

        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(TENANT_ID, APP_ID, Duration.ofHours(24));
        assertEquals("https://login.microsoftonline.com/common/discovery/keys", v.extractJwksUri(doc));
    }

    @Test
    void parseJsonWebKeySetRSA() throws Exception {
        // Create JWK, JWKS and jwt string
        JWK jwk = generateJWKRSA();
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";
        String jwt = createSignedJWT(jwk);

        // Create JWSVerifier
        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                "unknown-tenant-id", null, Duration.ofHours(24));

        assertDoesNotThrow(() -> v.parseJsonWebKeySet(jwks));

        Map<String, JWSVerifier> verifiersMap = v.parseJsonWebKeySet(jwks);
        assertEquals(1, verifiersMap.size());
        JWSVerifier v1 = verifiersMap.get(jwk.getKeyID());
        assertNotNull(v1);
        assertTrue(v1.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));

        // Verify JWT
        String[] chunks = jwt.split("\\.");
        assertTrue(v1.verify(
                JWSHeader.parse(new Base64URL(chunks[0])),
                (chunks[0] + "." + chunks[1]).getBytes(),
                new Base64URL(chunks[2])));
    }

    @Test
    void parseJsonWebKeySetEC() throws Exception {
        // Create JWK, JWKS and jwt string
        JWK jwk = generateJWKEC();
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";
        String jwt = createSignedJWT(jwk);

        // Create JWSVerifier
        MSEntraAccessTokenJWSVerifier v = new MSEntraAccessTokenJWSVerifier(
                "unknown-tenant-id", null, Duration.ofHours(24));

        assertDoesNotThrow(() -> v.parseJsonWebKeySet(jwks));
        Map<String, JWSVerifier> verifiersMap = v.parseJsonWebKeySet(jwks);
        assertEquals(1, verifiersMap.size());
        JWSVerifier v1 = verifiersMap.get(jwk.getKeyID());
        assertNotNull(v1);
        assertTrue(v1.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));

        // Verify JWT
        String[] chunks = jwt.split("\\.");
        assertTrue(v1.verify(
                JWSHeader.parse(new Base64URL(chunks[0])),
                (chunks[0] + "." + chunks[1]).getBytes(),
                new Base64URL(chunks[2])));
    }

    @Test
    void supportedJWSAlgorithmsEmpty() {
        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": []}";

        MSEntraAccessTokenJWSVerifier v = getSpyInstance(jwksUri, oidc, jwks);

        assertTrue(v.supportedJWSAlgorithms().isEmpty());
    }

    @Test
    void supportedJWSAlgorithmsRSA() throws Exception {
        JWK jwk = generateJWKRSA();
        String[] chunks = createSignedJWT(jwk).split("\\.");

        String jwksUri = "https://example.com/keys";
        MSEntraAccessTokenJWSVerifier v = getSpyInstance(
                jwksUri,
                "{\"jwks_uri\": \"" + jwksUri + "\"}",
                "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}");

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
        MSEntraAccessTokenJWSVerifier v = getSpyInstance(
                jwksUri,
                "{\"jwks_uri\": \"" + jwksUri + "\"}",
                "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}");

        assertTrue(v.supportedJWSAlgorithms().contains((JWSAlgorithm) jwk.getAlgorithm()));
    }

    @Test
    void supportedJWSAlgorithmsRSAJCAContext() throws NoSuchAlgorithmException, JOSEException {
        JWK jwk = generateJWKRSA();

        String jwksUri = "https://example.com/keys";
        String oidc = "{\"jwks_uri\": \"" + jwksUri + "\"}";
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";

        MSEntraAccessTokenJWSVerifier v = getSpyInstance(jwksUri, oidc, jwks);

        assertDoesNotThrow(v::getJCAContext);
    }

    @Test
    void supportedJWSAlgorithmsEC() throws Exception {
        JWK jwk = generateJWKEC();
        String[] chunks = createSignedJWT(jwk).split("\\.");

        String jwksUri = "https://example.com/keys";
        MSEntraAccessTokenJWSVerifier v = getSpyInstance(
                jwksUri,
                "{\"jwks_uri\": \"" + jwksUri + "\"}",
                "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}");

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
        MSEntraAccessTokenJWSVerifier v = getSpyInstance(jwksUri,
                "{\"jwks_uri\": \"" + jwksUri + "\"}",
                "{\"keys\": ["
                + jwkRSA.toPublicJWK().toJSONString() + ","
                + jwkEC.toPublicJWK().toJSONString()
                + "]}");

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
