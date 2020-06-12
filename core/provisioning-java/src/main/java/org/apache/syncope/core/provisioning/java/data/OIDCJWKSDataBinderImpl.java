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

package org.apache.syncope.core.provisioning.java.data;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCJWKS;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OIDCJWKSDataBinderImpl implements OIDCJWKSDataBinder {
    @Autowired
    private EntityFactory entityFactory;

    @Override
    public OIDCJWKSTO get(final OIDCJWKS jwks) {
        return new OIDCJWKSTO.Builder().
            json(jwks.getJson()).
            key(jwks.getKey()).
            build();
    }

    @Override
    public OIDCJWKS create() {
        try {
            OIDCJWKS jwks = entityFactory.newEntity(OIDCJWKS.class);
            RSAKey jwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(SecureRandomUtils.generateRandomUUID().toString())
                .generate();
            jwks.setJson(new JWKSet(jwk).toString());
            return jwks;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to create OIDC JWKS", e);
        }
    }

    @Override
    public OIDCJWKS update(final OIDCJWKS oidcjwks, final OIDCJWKSTO jwksTO) {
        oidcjwks.setJson(jwksTO.getJson());
        return oidcjwks;
    }
}
