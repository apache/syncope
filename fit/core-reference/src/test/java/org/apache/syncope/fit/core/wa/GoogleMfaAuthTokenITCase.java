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
package org.apache.syncope.fit.core.wa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleMfaAuthTokenITCase extends AbstractITCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static GoogleMfaAuthToken createGoogleMfaAuthToken() {
        int token = SECURE_RANDOM.ints(100_000, 999_999).findFirst().getAsInt();
        return new GoogleMfaAuthToken.Builder().token(token).issueDate(OffsetDateTime.now()).build();
    }

    @BeforeEach
    public void setup() {
        googleMfaAuthTokenService.delete((OffsetDateTime) null);
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
        assertEquals(1, googleMfaAuthTokenService.read(owner).getTotalCount());
    }

    @Test
    public void verifyProfile() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        PagedResult<AuthProfileTO> results = authProfileService.list(1, 100);
        assertFalse(results.getResult().isEmpty());
        AuthProfileTO profileTO = results.getResult().stream().
                filter(p -> owner.equals(p.getOwner())).findFirst().get();
        assertEquals(profileTO, authProfileService.read(profileTO.getKey()));
        authProfileService.delete(profileTO.getKey());
        assertThrows(SyncopeClientException.class, () -> authProfileService.read(profileTO.getKey()));
    }

    @Test
    public void deleteByToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.delete(token.getOtp());
        assertTrue(googleMfaAuthTokenService.read(owner).getResult().isEmpty());
    }

    @Test
    public void delete() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.delete(owner);
        assertTrue(googleMfaAuthTokenService.read(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByOwnerAndToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.store(owner, token);
        googleMfaAuthTokenService.delete(owner, token.getOtp());
        assertTrue(googleMfaAuthTokenService.read(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByDate() {
        String owner = UUID.randomUUID().toString();
        createGoogleMfaAuthToken();
        googleMfaAuthTokenService.delete(OffsetDateTime.now().minusDays(1));
        assertTrue(googleMfaAuthTokenService.read(owner).getResult().isEmpty());
        assertEquals(0, googleMfaAuthTokenService.read(owner).getTotalCount());
    }
}
