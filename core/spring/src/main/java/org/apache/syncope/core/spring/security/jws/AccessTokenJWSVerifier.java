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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class AccessTokenJWSVerifier implements JWSVerifier {

    private final JWSVerifier delegate;

    public AccessTokenJWSVerifier(final JWSAlgorithm jwsAlgorithm, final String jwsKey)
            throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {

        if (JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)) {
            if (jwsKey.indexOf(':') == -1) {
                throw new IllegalArgumentException("A key pair is required, in the 'private:public' format");
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(
                    Base64.getDecoder().decode(StringUtils.substringAfter(jwsKey, ":").getBytes()));
            delegate = new RSASSAVerifier((RSAPublicKey) kf.generatePublic(keySpecX509));
        } else if (JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
            delegate = new MACVerifier(jwsKey);
        } else {
            throw new IllegalArgumentException("Unsupported JWS algorithm: " + jwsAlgorithm.getName());
        }
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return delegate.supportedJWSAlgorithms();
    }

    @Override
    public JCAContext getJCAContext() {
        return delegate.getJCAContext();
    }

    @Override
    public boolean verify(
            final JWSHeader header,
            final byte[] signingInput,
            final Base64URL signature) throws JOSEException {

        return delegate.verify(header, signingInput, signature);
    }
}
