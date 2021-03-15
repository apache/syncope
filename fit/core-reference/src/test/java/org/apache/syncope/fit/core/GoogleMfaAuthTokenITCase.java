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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleMfaAuthTokenITCase extends AbstractITCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static GoogleMfaAuthToken createGoogleMfaAuthToken() {
        Integer token = SECURE_RANDOM.ints(100_000, 999_999)
                .findFirst()
                .getAsInt();
        return new GoogleMfaAuthToken.Builder()
                .token(token)
                .issueDate(new Date())
                .build();
    }

    @BeforeEach
    public void setup() {
        googleMfaAuthTokenService.delete(null);
    }

    @Test
    public void create() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        assertDoesNotThrow(() -> googleMfaAuthTokenService.store(UUID.randomUUID().toString(), token));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        assertEquals(1, googleMfaAuthTokenService.list().getTotalCount());
        assertEquals(1, googleMfaAuthTokenService.readFor(owner).getTotalCount());
    }

    @Test
    public void verifyProfile() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        List<AuthProfileTO> results = authProfileService.list();
        assertFalse(results.isEmpty());
        AuthProfileTO profileTO = results.get(0);
        assertNotNull(authProfileService.read(profileTO.getKey()));
        assertNotNull(authProfileService.readByOwner(profileTO.getOwner()));
        authProfileService.deleteByOwner(owner);
        assertThrows(SyncopeClientException.class, () -> authProfileService.readByOwner(owner));
    }

    @Test
    public void deleteByToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.delete(token.getOtp());
        assertTrue(googleMfaAuthTokenService.readFor(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByOwner() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.deleteFor(owner);
        assertTrue(googleMfaAuthTokenService.readFor(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByOwnerAndToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.delete(owner, token.getOtp());
        assertTrue(googleMfaAuthTokenService.readFor(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByDate() {
        String owner = UUID.randomUUID().toString();
        Date dateTime = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.delete(dateTime);
        assertTrue(googleMfaAuthTokenService.readFor(owner).getResult().isEmpty());
        assertEquals(0, googleMfaAuthTokenService.readFor(owner).getTotalCount());
    }
}
