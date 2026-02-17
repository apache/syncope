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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.rest.api.beans.AuthProfileQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleMfaAuthTokenITCase extends AbstractITCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static GoogleMfaAuthToken createGoogleMfaAuthToken() {
        int token = SECURE_RANDOM.ints(100_000, 999_999).findFirst().getAsInt();
        return new GoogleMfaAuthToken.Builder().token(token).issueDate(LocalDateTime.now()).build();
    }

    @BeforeEach
    public void setup() {
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.delete((LocalDateTime) null);
    }

    @Test
    public void create() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        assertDoesNotThrow(() -> GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(UUID.randomUUID().toString(), token));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner, token);
        assertEquals(1, GOOGLE_MFA_AUTH_TOKEN_SERVICE.list().getTotalCount());
        assertEquals(1, GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getTotalCount());
    }

    @Test
    public void listProfiles() {
        String owner1 = "owner" + UUID.randomUUID();
        String owner2 = "owner" + UUID.randomUUID();
        String test = "test" + UUID.randomUUID();
        PagedResult<AuthProfileTO> owners = null;
        PagedResult<AuthProfileTO> tests = null;
        try {
            GoogleMfaAuthToken token = createGoogleMfaAuthToken();
            GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner1, token);

            GoogleMfaAuthToken token1 = createGoogleMfaAuthToken();
            GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner2, token1);

            GoogleMfaAuthToken token2 = createGoogleMfaAuthToken();
            GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(test, token2);

            owners = AUTH_PROFILE_SERVICE.search(
                    new AuthProfileQuery.Builder().page(1).size(100).keyword("owner*").build());
            assertEquals(2, owners.getTotalCount());
            assertEquals(2, owners.getResult().size());

            tests = AUTH_PROFILE_SERVICE.search(
                    new AuthProfileQuery.Builder().page(1).size(100).keyword("test*").build());
            assertEquals(1, tests.getTotalCount());
            assertEquals(1, tests.getResult().size());
        } finally {
            AuthProfileTO profileTO = owners.getResult().stream().
                    filter(p -> owner1.equals(p.getOwner())).findFirst().orElseThrow();
            AUTH_PROFILE_SERVICE.delete(profileTO.getKey());

            profileTO = owners.getResult().stream().
                    filter(p -> owner2.equals(p.getOwner())).findFirst().orElseThrow();
            AUTH_PROFILE_SERVICE.delete(profileTO.getKey());

            profileTO = tests.getResult().stream().
                    filter(p -> test.equals(p.getOwner())).findFirst().orElseThrow();
            AUTH_PROFILE_SERVICE.delete(profileTO.getKey());
        }
    }

    @Test
    public void deleteByToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner, token);
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.delete(token.getOtp());
        assertTrue(GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getResult().isEmpty());
    }

    @Test
    public void delete() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner, token);
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.delete(owner);
        assertTrue(GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByOwnerAndToken() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.store(owner, token);
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.delete(owner, token.getOtp());
        assertTrue(GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getResult().isEmpty());
    }

    @Test
    public void deleteByDate() {
        String owner = UUID.randomUUID().toString();
        createGoogleMfaAuthToken();
        GOOGLE_MFA_AUTH_TOKEN_SERVICE.delete(LocalDateTime.now().minusDays(1));
        assertTrue(GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getResult().isEmpty());
        assertEquals(0, GOOGLE_MFA_AUTH_TOKEN_SERVICE.read(owner).getTotalCount());
    }
}
