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
package org.apache.syncope.common.lib.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.junit.jupiter.api.Test;

class OIDCLoginResponseTest {

    @Test
    void attributesAndSessionData() {
        OIDCLoginResponse response = new OIDCLoginResponse();
        response.setUsername("john");
        response.setAccessToken("jwt");
        OffsetDateTime expiration = OffsetDateTime.now().plusMinutes(5);
        response.setAccessTokenExpiryTime(expiration);
        response.setLogoutSupported(true);
        response.setSelfReg(true);

        assertEquals("john", response.getUsername());
        assertEquals("jwt", response.getAccessToken());
        assertEquals(expiration, response.getAccessTokenExpiryTime());
        assertTrue(response.isLogoutSupported());
        assertTrue(response.isSelfReg());

        Attr email = new Attr.Builder("email").value("john@example.com").build();
        response.getAttrs().add(email);

        assertEquals(email, response.getAttr("email").orElseThrow());
        assertTrue(response.getAttr("firstname").isEmpty());
    }

    @Test
    void multipleAttributesAreKept() {
        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());
        response.getAttrs().add(new Attr.Builder("firstname").value("John").build());

        assertEquals(
                List.of("email", "firstname"),
                response.getAttrs().stream().map(Attr::getSchema).sorted().toList());
    }
}
