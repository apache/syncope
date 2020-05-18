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
package org.apache.syncope.core.persistence.jpa.inner;

import org.apache.syncope.core.persistence.api.dao.auth.GoogleMfaAuthTokenDAO;
import org.apache.syncope.core.persistence.api.entity.auth.GoogleMfaAuthToken;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

@Transactional("Master")
public class GoogleMfaAuthTokenTest extends AbstractTest {

    @Autowired
    private GoogleMfaAuthTokenDAO googleMfaAuthTokenDAO;

    @BeforeEach
    public void setup() {
        googleMfaAuthTokenDAO.deleteAll();
    }

    @Test
    public void save() {
        create("SyncopeCreate", 123456);
    }

    @Test
    public void count() {
        create("SyncopeCount", 123456);
        assertEquals(1, googleMfaAuthTokenDAO.count());
        assertEquals(1, googleMfaAuthTokenDAO.count("SyncopeCount"));
    }

    @Test
    public void deleteByToken() {
        GoogleMfaAuthToken token = create("SyncopeDelete", 123456);
        assertTrue(googleMfaAuthTokenDAO.delete(token.getToken()));
        assertNull(googleMfaAuthTokenDAO.find(token.getUser(), token.getToken()));
    }

    @Test
    public void deleteByUser() {
        GoogleMfaAuthToken token = create("SyncopeDelete", 123456);
        assertTrue(googleMfaAuthTokenDAO.delete(token.getUser()));
        assertNull(googleMfaAuthTokenDAO.find(token.getUser(), token.getToken()));
    }

    @Test
    public void deleteByUserAndToken() {
        GoogleMfaAuthToken token = create("SyncopeDelete", 123456);
        assertTrue(googleMfaAuthTokenDAO.delete(token.getUser(), token.getToken()));
        assertNull(googleMfaAuthTokenDAO.find(token.getUser(), token.getToken()));
    }

    @Test
    public void deleteByDate() {
        LocalDateTime dateTime = LocalDateTime.now().minusDays(1);
        GoogleMfaAuthToken token = create("SyncopeDelete", 123456);
        assertTrue(googleMfaAuthTokenDAO.delete(dateTime));
        assertNull(googleMfaAuthTokenDAO.find(token.getUser(), token.getToken()));
    }

    private GoogleMfaAuthToken create(final String user, final Integer otp) {
        GoogleMfaAuthToken token = entityFactory.newEntity(GoogleMfaAuthToken.class);
        token.setUser(user);
        token.setToken(otp);
        token.setIssuedDateTime(LocalDateTime.now());
        googleMfaAuthTokenDAO.save(token);
        assertNotNull(token);
        assertNotNull(token.getKey());
        assertNotNull(googleMfaAuthTokenDAO.find(token.getUser(), token.getToken()));
        return token;
    }

}
