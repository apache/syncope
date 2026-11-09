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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder.AccessTokenInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccessTokenDataBinderTest extends AbstractDataBinderTest {

    private static final String OWNER = "verdi";

    @Autowired
    private AccessTokenDataBinder binder;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Test
    void create() {
        AccessTokenInfo info = binder.create(
                Optional.of("access-token-key"), OWNER, Map.of("claim", "claimValue"), "AUTH1", false);

        assertNotNull(info.jwt());
        assertTrue(info.expiration().isAfter(OffsetDateTime.now()));

        AccessToken accessToken = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        assertEquals("access-token-key", accessToken.getKey());
        assertEquals(info.jwt(), accessToken.getBody());
        assertEquals(OWNER, accessToken.getOwner());
        assertEquals("AUTH1", accessToken.getAuthorities());

        AccessTokenTO accessTokenTO = binder.getAccessTokenTO(accessToken);
        assertEquals(accessToken.getKey(), accessTokenTO.getKey());
        assertEquals(accessToken.getExpirationTime(), accessTokenTO.getExpirationTime());
        assertEquals(OWNER, accessTokenTO.getOwner());
    }

    @Test
    void createWithoutReplace() {
        AccessTokenInfo first = binder.create(Optional.empty(), OWNER, Map.of(), "AUTH1", false);
        AccessTokenInfo second = binder.create(Optional.empty(), OWNER, Map.of(), "AUTH2", false);

        assertEquals(first.jwt(), second.jwt());
        assertEquals("AUTH1", accessTokenDAO.findByOwner(OWNER).orElseThrow().getAuthorities());

        AccessToken existing = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        String key = existing.getKey();
        existing.setExpirationTime(OffsetDateTime.now().minusDays(1));
        accessTokenDAO.save(existing);
        entityManager.flush();

        AccessTokenInfo refreshed = binder.create(Optional.empty(), OWNER, Map.of(), "AUTH2", false);
        AccessToken afterRefresh = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        assertEquals(key, afterRefresh.getKey());
        assertEquals(refreshed.jwt(), afterRefresh.getBody());
        assertTrue(afterRefresh.getExpirationTime().isAfter(OffsetDateTime.now()));
        assertEquals("AUTH2", afterRefresh.getAuthorities());
    }

    @Test
    void createWithReplace() {
        binder.create(Optional.empty(), OWNER, Map.of(), "AUTH1", false);
        AccessToken before = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        String key = before.getKey();

        AccessTokenInfo replaced = binder.create(Optional.empty(), OWNER, Map.of(), "AUTH2", true);

        assertNotNull(replaced.jwt());
        AccessToken accessToken = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        assertEquals(key, accessToken.getKey());
        assertEquals(replaced.jwt(), accessToken.getBody());
        assertEquals("AUTH2", accessToken.getAuthorities());
    }

    @Test
    void createForAdmin() {
        AccessTokenInfo info = binder.create(Optional.empty(), "admin", Map.of(), "AUTH", false);

        assertNotNull(info.jwt());
        AccessToken accessToken = accessTokenDAO.findByOwner("admin").orElseThrow();
        assertNull(accessToken.getAuthorities());
    }

    @Test
    void update() {
        binder.create(Optional.empty(), OWNER, Map.of(), "AUTH1", false);
        AccessToken accessToken = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        String key = accessToken.getKey();

        AccessTokenInfo updated = binder.update(accessToken, "AUTH2");

        assertTrue(updated.expiration().isAfter(OffsetDateTime.now()));

        AccessToken actual = accessTokenDAO.findByOwner(OWNER).orElseThrow();
        assertEquals(key, actual.getKey());
        assertEquals(updated.jwt(), actual.getBody());
        assertEquals(updated.expiration(), actual.getExpirationTime());
        assertEquals("AUTH2", actual.getAuthorities());
        assertFalse(actual.getBody().isEmpty());
    }
}
