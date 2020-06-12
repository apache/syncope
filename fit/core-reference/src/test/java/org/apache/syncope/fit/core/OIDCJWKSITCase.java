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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OIDCJWKSITCase extends AbstractITCase {

    @Test
    public void verifyJwks() {
        try {
            oidcJwksConfService.delete();

            oidcJwksService.get();
            fail("Should not locate an OIDC JWKS");
        } catch (final SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        Response response = oidcJwksService.set();
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        try {
            oidcJwksService.set();
            fail("Should not recreate an OIDC JWKS");
        } catch (final SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

}
