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

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.persistence.neo4j.PersistenceTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MultitenancyTest extends AbstractTest {

    static {
        PersistenceTestContext.TEST_DOMAIN.set("Two");
    }

    @AfterAll
    public static void restoreDomain() {
        PersistenceTestContext.TEST_DOMAIN.set(SyncopeConstants.MASTER_DOMAIN);
    }

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void readPlainSchemas() {
        assertEquals(1, plainSchemaDAO.findAll().size());
    }

    @Test
    public void readRealm() {
        assertEquals(
                1,
                realmSearchDAO.findDescendants(realmDAO.getRoot().getFullPath(), null, Pageable.unpaged()).size());
        assertEquals(
                realmDAO.getRoot(),
                realmSearchDAO.findDescendants(realmDAO.getRoot().getFullPath(), null, Pageable.unpaged()).getFirst());
    }

    @Test
    public void createUser() {
        assertNull(realmDAO.getRoot().getPasswordPolicy());

        User user = entityFactory.newEntity(User.class);
        user.setRealm(realmDAO.getRoot());
        user.setPassword("password");
        user.setCipherAlgorithm(CipherAlgorithm.SHA256);
        user.setUsername("username");

        User actual = userDAO.save(user);
        assertNotNull(actual);
        assertEquals(0, actual.getPasswordHistory().size());
    }
}
