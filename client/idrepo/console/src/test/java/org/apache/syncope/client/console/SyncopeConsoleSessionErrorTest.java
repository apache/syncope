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
package org.apache.syncope.client.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SyncopeConsoleSessionErrorTest {

    @Test
    void errorEnum() {
        assertEquals(3, SyncopeConsoleSession.Error.values().length);

        SyncopeConsoleSession.Error sessionExpired = SyncopeConsoleSession.Error.SESSION_EXPIRED;
        assertNotNull(sessionExpired);
        assertEquals("error.session.expired", sessionExpired.key());
        assertEquals("Session expired: please login again", sessionExpired.fallback());

        SyncopeConsoleSession.Error authorization = SyncopeConsoleSession.Error.AUTHORIZATION;
        assertNotNull(authorization);
        assertEquals("error.authorization", authorization.key());
        assertEquals("Insufficient access rights when performing the requested operation", authorization.fallback());

        SyncopeConsoleSession.Error rest = SyncopeConsoleSession.Error.REST;
        assertNotNull(rest);
        assertEquals("error.rest", rest.key());
        assertEquals("There was an error while contacting the Core server", rest.fallback());
    }
}
