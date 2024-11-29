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
package org.apache.syncope.wa.starter.oidc;

import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.oidc.jwks.generator.OidcJsonWebKeystoreGeneratorService;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class WAOIDCJWKSGeneratorService implements OidcJsonWebKeystoreGeneratorService {

    protected static final Logger LOG = LoggerFactory.getLogger(WAOIDCJWKSGeneratorService.class);

    protected final WARestClient waRestClient;

    protected final String jwksKeyId;

    protected final String jwksType;

    protected final int jwksKeySize;

    public WAOIDCJWKSGeneratorService(
            final WARestClient waRestClient,
            final String jwksKeyId,
            final String jwksType,
            final int jwksKeySize) {

        this.waRestClient = waRestClient;
        this.jwksKeyId = jwksKeyId;
        this.jwksType = jwksType;
        this.jwksKeySize = jwksKeySize;
    }

    @Override
    public JsonWebKeySet store(final JsonWebKeySet jsonWebKeySet) {
        OIDCJWKSService service = waRestClient.getService(OIDCJWKSService.class);
        OIDCJWKSTO to = new OIDCJWKSTO();
        to.setJson(jsonWebKeySet.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
        service.set(to);
        return jsonWebKeySet;
    }

    @Override
    public Optional<Resource> find() {
        return Optional.of(generate());
    }

    @Override
    public Resource generate() {
        OIDCJWKSService service = waRestClient.getService(OIDCJWKSService.class);
        OIDCJWKSTO jwksTO = null;
        try {
            jwksTO = service.get();
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                try (Response response = service.generate(jwksKeyId, jwksType, jwksKeySize)) {
                    jwksTO = response.readEntity(OIDCJWKSTO.class);
                } catch (Exception ge) {
                    LOG.error("While generating new OIDC JWKS", ge);
                }
            } else {
                LOG.error("While reading OIDC JWKS", e);
            }
        }
        if (jwksTO == null) {
            throw new IllegalStateException("Unable to determine OIDC JWKS resource");
        }
        return new ByteArrayResource(jwksTO.getJson().getBytes(StandardCharsets.UTF_8), "OIDC JWKS");
    }
}
