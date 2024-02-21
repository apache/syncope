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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ApplicationTest extends AbstractTest {

    @Autowired
    private ApplicationDAO applicationDAO;

    @Test
    public void findAll() {
        List<? extends Application> applications = applicationDAO.findAll();
        assertFalse(applications.isEmpty());
        assertEquals(1, applications.size());
    }

    @Test
    public void find() {
        Application mightyApp = applicationDAO.findById("mightyApp").orElseThrow();
        assertEquals(2, mightyApp.getPrivileges().size());

        Privilege getMighty = applicationDAO.findPrivilege("getMighty").orElseThrow();
        assertEquals(getMighty, mightyApp.getPrivilege("getMighty").get());

    }

    @Test
    public void crud() {
        // 1. create application
        Application application = entityFactory.newEntity(Application.class);
        application.setKey(UUID.randomUUID().toString());

        String privilege1Key = UUID.randomUUID().toString();
        Privilege privilege = entityFactory.newEntity(Privilege.class);
        privilege.setKey(privilege1Key);
        privilege.setSpec("{ \"one\": true }");
        application.add(privilege);

        String privilege2Key = UUID.randomUUID().toString();
        privilege = entityFactory.newEntity(Privilege.class);
        privilege.setKey(privilege2Key);
        privilege.setSpec("{ \"two\": true }");
        application.add(privilege);

        String privilege3Key = UUID.randomUUID().toString();
        privilege = entityFactory.newEntity(Privilege.class);
        privilege.setKey(privilege3Key);
        privilege.setSpec("{ \"three\": true }");
        application.add(privilege);

        application = applicationDAO.save(application);
        assertNotNull(application);
        assertNull(application.getDescription());
        assertEquals(3, application.getPrivileges().size());

        // 2. update application
        application.setDescription("A description");

        Privilege priv3 = applicationDAO.findPrivilege(privilege3Key).orElseThrow();
        priv3.setApplication(null);
        application.getPrivileges().remove(priv3);
        assertEquals(2, application.getPrivileges().size());

        applicationDAO.save(application);

        application = applicationDAO.findById(application.getKey()).orElseThrow();
        assertNotNull(application.getDescription());
        assertEquals(2, application.getPrivileges().size());

        // 3. delete application
        applicationDAO.delete(application);

        assertTrue(applicationDAO.findById(application.getKey()).isEmpty());
        assertTrue(applicationDAO.findPrivilege(privilege1Key).isEmpty());
        assertTrue(applicationDAO.findPrivilege(privilege2Key).isEmpty());
    }
}
