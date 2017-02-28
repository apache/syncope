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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AccessTokenTest extends AbstractTest {

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Test
    public void crud() {
        AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
        accessToken.setKey(UUID.randomUUID().toString());
        accessToken.setBody("pointless body");
        accessToken.setExpiryTime(new Date());
        accessToken.setOwner("bellini");

        accessToken = accessTokenDAO.save(accessToken);
        assertNotNull(accessToken);

        accessTokenDAO.flush();

        accessToken = accessTokenDAO.findByOwner("bellini");
        assertNotNull(accessToken);
        assertEquals("bellini", accessToken.getOwner());

        accessTokenDAO.deleteExpired();

        accessTokenDAO.flush();

        accessToken = accessTokenDAO.findByOwner("bellini");
        assertNull(accessToken);
    }
}
