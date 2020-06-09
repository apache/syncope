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

import org.apereo.cas.oidc.jwks.OidcJsonWebKeystoreGeneratorService;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.OIDCJWKSService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;

public class SyncopeWAOIDCJWKSGeneratorService implements OidcJsonWebKeystoreGeneratorService {
    private final WARestClient waRestClient;

    public SyncopeWAOIDCJWKSGeneratorService(final WARestClient restClient) {
        this.waRestClient = restClient;
    }

    @Override
    public Resource generate() {
        OIDCJWKSService service = waRestClient.getSyncopeClient().
            getService(OIDCJWKSService.class);
        try {
            Response response = service.set();
            OIDCJWKSTO jwksTO = response.readEntity(new GenericType<OIDCJWKSTO>() {
            });
            return new ByteArrayResource(jwksTO.getJson().getBytes(StandardCharsets.UTF_8), "OIDC JWKS");
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.EntityExists) {
                OIDCJWKSTO jwksTO = service.get();
                return new ByteArrayResource(jwksTO.getJson().getBytes(StandardCharsets.UTF_8), "OIDC JWKS");
            }
        }
        throw new RuntimeException("Unable to determine OIDC JWKS resource");
    }
}
