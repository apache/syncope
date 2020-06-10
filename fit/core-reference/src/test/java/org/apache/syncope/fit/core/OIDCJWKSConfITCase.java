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
package org.apache.syncope.fit.core;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OIDCJWKSConfITCase extends AbstractITCase {

    private static OIDCJWKSTO getCurrentJwksTO() {
        try {
            return oidcJwksService.get();
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                Response response = oidcJwksService.set();
                assertEquals(HttpStatus.CREATED.value(), response.getStatus());
                return response.readEntity(new GenericType<OIDCJWKSTO>() {
                });
            }
        }
        throw new RuntimeException("Unable to locate current OIDC JWKS");
    }

    @Test
    public void verifyJwks() throws Exception {
        RSAKey jwk = new RSAKeyGenerator(2048)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .generate();
        String json = new JWKSet(jwk).toString();

        assertDoesNotThrow(new Executable() {
            @Override
            public void execute() {
                OIDCJWKSTO currentTO = getCurrentJwksTO();
                currentTO.setJson(json);
                oidcJwksConfService.update(currentTO);
            }
        });
        OIDCJWKSTO currentTO = getCurrentJwksTO();
        assertEquals(json, currentTO.getJson());
    }

}
