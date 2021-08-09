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
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class AccessTokenJWSSigner implements JWSSigner {

    private final JWSAlgorithm jwsAlgorithm;

    private final JWSSigner delegate;

    public AccessTokenJWSSigner(final JWSAlgorithm jwsAlgorithm, final String jwsKey)
            throws KeyLengthException, NoSuchAlgorithmException, InvalidKeySpecException {

        this.jwsAlgorithm = jwsAlgorithm;

        if (JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)) {
            if (jwsKey.indexOf(':') == -1) {
                throw new IllegalArgumentException("A key pair is required, in the 'private:public' format");
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(StringUtils.substringBefore(jwsKey, ":").getBytes()));
            delegate = new RSASSASigner(kf.generatePrivate(keySpecPKCS8));
        } else if (JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)) {
            delegate = new MACSigner(jwsKey);
        } else {
            throw new IllegalArgumentException("Unsupported JWS algorithm: " + jwsAlgorithm.getName());
        }
    }

    public JWSAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
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
    public Base64URL sign(final JWSHeader header, final byte[] signingInput) throws JOSEException {
        return delegate.sign(header, signingInput);
    }
}
