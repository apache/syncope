/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.ws.rs.core.Response;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoogleMfaAuthTokenITCase extends AbstractITCase {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static GoogleMfaAuthToken createGoogleMfaAuthToken() {
        Integer token = SECURE_RANDOM.ints(100_000, 999_999)
            .findFirst()
            .getAsInt();
        return new GoogleMfaAuthToken.Builder()
            .owner(UUID.randomUUID().toString())
            .token(token)
            .issueDate(new Date())
            .build();
    }

    @BeforeEach
    public void setup() {
        googleMfaAuthTokenService.deleteTokens();
    }

    @Test
    public void create() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        assertDoesNotThrow(new Executable() {
            @Override
            public void execute() throws Throwable {
                Response response = googleMfaAuthTokenService.save(token);
                if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                    Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
                    if (ex != null) {
                        throw ex;
                    }
                }
            }
        });
    }

    @Test
    public void count() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.save(token);
        assertEquals(1, googleMfaAuthTokenService.countTokens());
        assertEquals(1, googleMfaAuthTokenService.countTokensForOwner(token.getOwner()));
    }

    @Test
    public void verifyProfile() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        googleMfaAuthTokenService.save(token);
        final List<AuthProfileTO> results = authProfileService.list();
        assertFalse(results.isEmpty());
        AuthProfileTO profileTO = results.get(0);
        assertNotNull(authProfileService.findByKey(profileTO.getKey()));
        assertNotNull(authProfileService.findByOwner(profileTO.getOwner()));
        Response response = authProfileService.deleteByOwner(token.getOwner());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertThrows(SyncopeClientException.class, () -> authProfileService.findByOwner(token.getOwner()));
    }

    @Test
    public void deleteByToken() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        Response response = googleMfaAuthTokenService.save(token);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);
        response = googleMfaAuthTokenService.deleteToken(token.getToken());
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertTrue(googleMfaAuthTokenService.findTokensFor(token.getOwner()).isEmpty());
    }

    @Test
    public void deleteByOwner() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        Response response = googleMfaAuthTokenService.save(token);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);
        response = googleMfaAuthTokenService.deleteTokensFor(token.getOwner());
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertTrue(googleMfaAuthTokenService.findTokensFor(token.getOwner()).isEmpty());
    }

    @Test
    public void deleteByOwnerAndToken() {
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        Response response = googleMfaAuthTokenService.save(token);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);
        response = googleMfaAuthTokenService.deleteToken(token.getOwner(), token.getToken());
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertTrue(googleMfaAuthTokenService.findTokensFor(token.getOwner()).isEmpty());
    }

    @Test
    public void deleteByDate() {
        Date dateTime = Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        GoogleMfaAuthToken token = createGoogleMfaAuthToken();
        final Response response = googleMfaAuthTokenService.deleteTokensByDate(dateTime);
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertTrue(googleMfaAuthTokenService.findTokensFor(token.getOwner()).isEmpty());
        assertEquals(0, googleMfaAuthTokenService.countTokensForOwner(token.getOwner()));
    }
}
