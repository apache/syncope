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
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OIDCRPTest extends AbstractClientAppTest {

    @Autowired
    private OIDCRPClientAppDAO oidcrpDAO;

    @Test
    public void find() {
        int beforeCount = oidcrpDAO.findAll().size();

        OIDCRPClientApp rp = entityFactory.newEntity(OIDCRPClientApp.class);
        rp.setName("OIDC");
        rp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        rp.setDescription("This is a sample OIDC RP");
        rp.setClientId("clientid");
        rp.setClientSecret("secret");
        rp.setSubjectType(OIDCSubjectType.PUBLIC);
        rp.getSupportedGrantTypes().add(OIDCGrantType.password);
        rp.getSupportedResponseTypes().add(OIDCResponseType.CODE);

        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        rp.setAccessPolicy(accessPolicy);

        AuthPolicy authPolicy = buildAndSaveAuthPolicy();
        rp.setAuthPolicy(authPolicy);

        oidcrpDAO.save(rp);

        assertNotNull(rp);
        assertNotNull(rp.getKey());

        int afterCount = oidcrpDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);

        rp = oidcrpDAO.findByClientId("clientid").orElseThrow();
        assertNotNull(rp.getAuthPolicy());

        rp = oidcrpDAO.findByName("OIDC").orElseThrow();

        assertTrue(oidcrpDAO.findByClientAppId(rp.getClientAppId()).isPresent());

        oidcrpDAO.deleteByClientId("clientid");

        assertTrue(oidcrpDAO.findByName("OIDC").isEmpty());
    }
}
