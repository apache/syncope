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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
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
import java.util.Date;
import java.util.UUID;

abstract class AbstractTest {

    protected static final String TENANT_ID = "test-tenant-id";

    protected static final String APP_ID = "test-app-id";

    protected static String createSignedJWT(final JWK jwk) throws JOSEException {
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

    protected static JWK generateJWKRSA() throws NoSuchAlgorithmException {
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

    protected static JWK generateJWKEC() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
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
}
