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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MSEntraJWSVerifierCacheLoaderTest extends AbstractTest {

    @Test
    void getOpenIDMetadataDocumentUrl() {
        // Tenant id and app id
        MSEntraJWSVerifierCacheLoader v1 = new MSEntraJWSVerifierCacheLoader(TENANT_ID, APP_ID);
        assertEquals(String.format(
                "https://login.microsoftonline.com/%s/.well-known/openid-configuration?appid=%s", TENANT_ID, APP_ID),
                v1.getOpenIDMetadataDocumentUrl());

        // Tenant id, no app id
        MSEntraJWSVerifierCacheLoader v2 = new MSEntraJWSVerifierCacheLoader(TENANT_ID, null);
        assertEquals(
                String.format("https://login.microsoftonline.com/%s/.well-known/openid-configuration", TENANT_ID),
                v2.getOpenIDMetadataDocumentUrl());

        // No tenant id, no app id
        MSEntraJWSVerifierCacheLoader v3 = new MSEntraJWSVerifierCacheLoader(null, null);
        assertEquals(
                "https://login.microsoftonline.com/common/.well-known/openid-configuration",
                v3.getOpenIDMetadataDocumentUrl());
    }

    @Test
    void extractJwksUri() {
        String doc = "{\"jwks_uri\": \"https://login.microsoftonline.com/common/discovery/keys\"}";

        MSEntraJWSVerifierCacheLoader v = new MSEntraJWSVerifierCacheLoader(TENANT_ID, APP_ID);
        assertEquals("https://login.microsoftonline.com/common/discovery/keys", v.extractJwksUri(doc));
    }

    @Test
    void parseJsonWebKeySetRSA() throws Exception {
        // Create JWK, JWKS and jwt string
        JWK jwk = generateJWKRSA();
        String jwks = "{\"keys\": [" + jwk.toPublicJWK().toJSONString() + "]}";
        String jwt = createSignedJWT(jwk);

        // Create JWSVerifier
        MSEntraJWSVerifierCacheLoader v = new MSEntraJWSVerifierCacheLoader("unknown-tenant-id", null);

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
        MSEntraJWSVerifierCacheLoader v = new MSEntraJWSVerifierCacheLoader("unknown-tenant-id", null);

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
}
