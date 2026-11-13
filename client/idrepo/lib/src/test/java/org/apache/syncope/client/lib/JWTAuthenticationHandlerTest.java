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
package org.apache.syncope.client.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class JWTAuthenticationHandlerTest {

    private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signature";

    @Test
    void constructor() {
        JWTAuthenticationHandler handler = new JWTAuthenticationHandler(JWT);

        assertNotNull(handler);
        assertEquals(JWT, handler.getJwt());
    }

    @Test
    void isAuthenticationHandler() {
        assertNotNull(new JWTAuthenticationHandler(JWT));
        assertEquals(AuthenticationHandler.class, new JWTAuthenticationHandler(JWT).
                getClass().getInterfaces()[0]);
    }
}
