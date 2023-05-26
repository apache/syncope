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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OIDCJWKSITCase extends AbstractITCase {

    private static OIDCJWKSService WA_OIDC_JWKS_SERVICE;

    @BeforeAll
    public static void setup() {
        assumeTrue(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        WA_OIDC_JWKS_SERVICE = ANONYMOUS_CLIENT.getService(OIDCJWKSService.class);
    }

    @Test
    public void deleteGetSet() {
        try {
            OIDC_JWKS_SERVICE.delete();

            WA_OIDC_JWKS_SERVICE.get();
            fail("Should not locate an OIDC JWKS");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        Response response = WA_OIDC_JWKS_SERVICE.generate("syncope", "RSA", 2048);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        try {
            WA_OIDC_JWKS_SERVICE.generate("syncope", "RSA", 2048);
            fail("Should not recreate an OIDC JWKS");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
