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

import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional("Master")
public class AuthProfileTest extends AbstractTest {

    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    public void googleMfaToken() {
        String id = UUID.randomUUID().toString();

        createAuthProfileWithToken(id, 123456);

        Optional<AuthProfile> result = authProfileDAO.findByOwner(id);
        assertTrue(result.isPresent());

        assertFalse(authProfileDAO.findAll().isEmpty());

        AuthProfile authProfile = result.get();
        result = authProfileDAO.findByKey(authProfile.getKey());
        assertTrue(result.isPresent());

        authProfile.setOwner("SyncopeCreate-New");
        authProfile.getGoogleMfaAuthTokens().clear();
        authProfileDAO.save(authProfile);

        assertFalse(authProfileDAO.findByOwner(id).isPresent());
    }

    private AuthProfile createAuthProfileWithToken(final String owner, final Integer otp) {
        AuthProfile profile = entityFactory.newEntity(AuthProfile.class);
        profile.setOwner(owner);
        GoogleMfaAuthToken token = new GoogleMfaAuthToken.Builder()
            .issueDate(new Date())
            .token(otp)
            .owner(owner)
            .build();
        profile.add(token);
        return authProfileDAO.save(profile);
    }
}
