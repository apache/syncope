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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.jose4j.jwk.EcJwkGenerator;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwk.Use;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;
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
        return new OIDCJWKSTO.Builder().
                key(jwks.getKey()).
                json(jwks.getJson()).
                build();
    }

    protected PublicJsonWebKey generate(
            final String jwksKeyId,
            final String jwksType,
            final int jwksKeySize,
            final String use,
            final JsonWebKeyLifecycleState state) throws JoseException {

        PublicJsonWebKey jwk;
        switch (jwksType.trim().toLowerCase(Locale.ENGLISH)) {
            case "ec" -> {
                switch (jwksKeySize) {
                    case 384:
                        jwk = EcJwkGenerator.generateJwk(EllipticCurves.P384);
                        jwk.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384);
                        break;

                    case 512:
                        jwk = EcJwkGenerator.generateJwk(EllipticCurves.P521);
                        jwk.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512);
                        break;

                    default:
                        jwk = EcJwkGenerator.generateJwk(EllipticCurves.P256);
                        jwk.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512);
                }
            }

            default ->
                jwk = RsaJwkGenerator.generateJwk(jwksKeySize);
        }

        jwk.setKeyId(jwksKeyId.concat("-").concat(SecureRandomUtils.generateRandomLetters(8)));
        jwk.setUse(use);
        jwk.setOtherParameter(PARAMETER_STATE, state.getState());
        return jwk;
    }

    @Override
    public OIDCJWKS create(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        List<PublicJsonWebKey> keys = new ArrayList<>();
        try {
            keys.add(generate(jwksKeyId, jwksType, jwksKeySize, Use.SIGNATURE, JsonWebKeyLifecycleState.CURRENT));
            keys.add(generate(jwksKeyId, jwksType, jwksKeySize, Use.ENCRYPTION, JsonWebKeyLifecycleState.CURRENT));
            keys.add(generate(jwksKeyId, jwksType, jwksKeySize, Use.SIGNATURE, JsonWebKeyLifecycleState.FUTURE));
            keys.add(generate(jwksKeyId, jwksType, jwksKeySize, Use.ENCRYPTION, JsonWebKeyLifecycleState.FUTURE));
        } catch (JoseException e) {
            LOG.error("Could not create OIDC JWKS", e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        OIDCJWKS oidcJWKS = entityFactory.newEntity(OIDCJWKS.class);
        oidcJWKS.setJson(new JsonWebKeySet(keys).toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
        return oidcJWKS;
    }
}
