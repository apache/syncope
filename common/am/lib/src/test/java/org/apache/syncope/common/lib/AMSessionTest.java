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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AMSessionTest {

    @Test
    void session() {
        OffsetDateTime authenticationDate = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        AMSession session = new AMSession();
        session.setKey("key");
        session.setAuthenticationDate(authenticationDate);
        session.setPrincipal("principal");
        session.setJson("json");

        assertEquals("key", session.getKey());
        assertEquals(authenticationDate, session.getAuthenticationDate());
        assertEquals("principal", session.getPrincipal());
        assertEquals("json", session.getJson());
    }

    @Test
    void standardScopes() {
        assertSame(OIDCStandardScope.openid, OIDCStandardScope.valueOf("openid"));
        assertEquals(8, OIDCStandardScope.values().length);
    }
}
