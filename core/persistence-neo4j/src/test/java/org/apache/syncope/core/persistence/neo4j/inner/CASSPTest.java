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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CASSPTest extends AbstractClientAppTest {

    @Autowired
    private CASSPClientAppDAO casspDAO;

    @Test
    public void find() {
        long beforeCount = casspDAO.count();

        CASSPClientApp rp = entityFactory.newEntity(CASSPClientApp.class);
        rp.setName("CAS");
        rp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        rp.setDescription("This is a sample CAS RP");
        rp.setServiceId("https://syncope.apache.org/.*");

        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        rp.setAccessPolicy(accessPolicy);

        AuthPolicy authPolicy = buildAndSaveAuthPolicy();
        rp.setAuthPolicy(authPolicy);

        casspDAO.save(rp);

        assertNotNull(rp);
        assertNotNull(rp.getKey());

        long afterCount = casspDAO.count();
        assertEquals(afterCount, beforeCount + 1);

        rp = casspDAO.findByName("CAS").orElseThrow();
        assertNotNull(rp);

        rp = casspDAO.findByClientAppId(rp.getClientAppId()).orElseThrow();
        assertNotNull(rp);

        casspDAO.delete(rp);
        assertTrue(casspDAO.findByName("CAS").isEmpty());
    }
}
