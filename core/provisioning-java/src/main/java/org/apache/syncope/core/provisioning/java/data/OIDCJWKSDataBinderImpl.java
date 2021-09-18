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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JWSAlgorithm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCJWKS;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDCJWKSDataBinderImpl implements OIDCJWKSDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(OIDCJWKSDataBinder.class);

    protected final EntityFactory entityFactory;

    public OIDCJWKSDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public OIDCJWKSTO getOIDCJWKSTO(final OIDCJWKS jwks) {
        return new OIDCJWKSTO.Builder().json(jwks.getJson()).key(jwks.getKey()).build();
    }

    @Override
    public OIDCJWKS create(final int size, final JWSAlgorithm algorithm) {
        try {
            OIDCJWKS jwks = entityFactory.newEntity(OIDCJWKS.class);
            RSAKey jwk = new RSAKeyGenerator(size).
                    keyUse(KeyUse.SIGNATURE).
                    keyID(SecureRandomUtils.generateRandomUUID().toString()).
                    algorithm(new com.nimbusds.jose.JWSAlgorithm(algorithm.name())).
                    generate();
            jwks.setJson(JSONObjectUtils.toJSONString(new JWKSet(jwk).toJSONObject(false)));
            return jwks;
        } catch (JOSEException e) {
            LOG.error("Could not create OIDC JWKS", e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }
}
