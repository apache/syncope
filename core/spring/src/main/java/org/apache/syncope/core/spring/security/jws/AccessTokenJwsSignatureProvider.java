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

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.PrivateKeyJwsSignatureProvider;
import org.springframework.beans.factory.InitializingBean;

public class AccessTokenJwsSignatureProvider implements JwsSignatureProvider, InitializingBean {

    private SignatureAlgorithm jwsAlgorithm;

    private String jwsKey;

    private JwsSignatureProvider delegate;

    public void setJwsAlgorithm(final SignatureAlgorithm jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    public void setJwsKey(final String jwsKey) {
        this.jwsKey = jwsKey;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jwsAlgorithm == null) {
            throw new IllegalArgumentException("An instance of " + SignatureAlgorithm.class + " is required");
        }

        if (SignatureAlgorithm.isPublicKeyAlgorithm(jwsAlgorithm)) {
            if (!jwsAlgorithm.getJwaName().startsWith("RS")) {
                throw new IllegalArgumentException(jwsAlgorithm.getJavaName() + " not supported.");
            }

            if (jwsKey == null || jwsKey.indexOf(':') == -1) {
                throw new IllegalArgumentException("A key pair is required, in the 'private:public' format");
            }

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(StringUtils.substringBefore(jwsKey, ":").getBytes()));
            delegate = new PrivateKeyJwsSignatureProvider(kf.generatePrivate(keySpecPKCS8), jwsAlgorithm);
        } else {
            if (jwsKey == null) {
                throw new IllegalArgumentException("A shared key is required");
            }

            delegate = new HmacJwsSignatureProvider(jwsKey.getBytes(), jwsAlgorithm);
        }
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public byte[] sign(final JwsHeaders headers, final byte[] content) {
        return delegate.sign(headers, content);
    }

    @Override
    public JwsSignature createJwsSignature(final JwsHeaders headers) {
        return delegate.createJwsSignature(headers);
    }
}
