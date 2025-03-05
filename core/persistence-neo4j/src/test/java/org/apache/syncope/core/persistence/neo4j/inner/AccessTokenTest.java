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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AccessTokenTest extends AbstractTest {

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    private void create(final String key, final long minusMinutes) {
        AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
        accessToken.setKey(key);
        accessToken.setBody("pointless body");
        accessToken.setExpirationTime(OffsetDateTime.now().minusMinutes(minusMinutes));
        accessToken.setOwner(UUID.randomUUID().toString());

        accessTokenDAO.save(accessToken);
    }

    @Test
    public void findAll() {
        for (long i = 0; i < 5; i++) {
            create(String.valueOf(6 - i), i);
        }

        assertEquals(5, accessTokenDAO.count());

        Page<? extends AccessToken> page =
                accessTokenDAO.findAll(PageRequest.of(0, 2, Sort.by("expirationTime").descending()));
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getNumberOfElements());

        List<? extends AccessToken> list = page.get().toList();
        assertEquals(2, list.size());
        assertEquals("6", list.get(0).getKey());
        assertEquals("5", list.get(1).getKey());

        page = accessTokenDAO.findAll(PageRequest.of(1, 2, Sort.by("expirationTime").descending()));
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getNumberOfElements());

        list = page.get().toList();
        assertEquals(2, list.size());
        assertEquals("4", list.get(0).getKey());
        assertEquals("3", list.get(1).getKey());

        page = accessTokenDAO.findAll(PageRequest.of(2, 2, Sort.by("expirationTime").descending()));
        assertEquals(5, page.getTotalElements());
        assertEquals(1, page.getNumberOfElements());

        list = page.get().toList();
        assertEquals(1, list.size());
        assertEquals("2", list.getFirst().getKey());
    }

    @Test
    public void crud() {
        AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
        accessToken.setKey(UUID.randomUUID().toString());
        accessToken.setBody("pointless body");
        accessToken.setExpirationTime(OffsetDateTime.now());
        accessToken.setOwner("bellini");

        accessToken = accessTokenDAO.save(accessToken);
        assertNotNull(accessToken);

        accessToken = accessTokenDAO.findByOwner("bellini").orElse(null);
        assertNotNull(accessToken);
        assertEquals("bellini", accessToken.getOwner());

        int deleted = accessTokenDAO.deleteExpired(OffsetDateTime.now());
        assertEquals(1, deleted);

        accessToken = accessTokenDAO.findByOwner("bellini").orElse(null);
        assertNull(accessToken);
    }

    @Test
    public void unique() {
        AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
        accessToken.setKey(UUID.randomUUID().toString());
        accessToken.setBody("pointless body");
        accessToken.setExpirationTime(OffsetDateTime.now());
        accessToken.setOwner("bellini");

        accessTokenDAO.save(accessToken);

        try {
            accessToken.setKey(UUID.randomUUID().toString());
            accessTokenDAO.save(accessToken);
            fail();
        } catch (DataIntegrityViolationException e) {
            assertNotNull(e);
        }
    }
}
