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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SAML2IdPEntityTest extends AbstractTest {

    @Autowired
    private SAML2IdPEntityDAO saml2IdPEntityDAO;

    private SAML2IdPEntity create(final String owner) {
        SAML2IdPEntity entity = entityFactory.newEntity(SAML2IdPEntity.class);
        entity.setKey(owner);
        entity.setMetadata("metadata".getBytes(StandardCharsets.UTF_8));
        entity.setEncryptionCertificate("encryptionCert".getBytes(StandardCharsets.UTF_8));
        entity.setEncryptionKey("encryptionKey".getBytes(StandardCharsets.UTF_8));
        entity.setSigningCertificate("signatureCert".getBytes(StandardCharsets.UTF_8));
        entity.setSigningKey("signatureKey".getBytes(StandardCharsets.UTF_8));
        entity = saml2IdPEntityDAO.save(entity);
        assertNotNull(entity);
        assertNotNull(saml2IdPEntityDAO.findById(entity.getKey()));

        return entity;
    }

    @Test
    public void find() {
        create("Syncope");

        assertTrue(saml2IdPEntityDAO.findById("Syncope").isPresent());

        assertTrue(saml2IdPEntityDAO.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    public void save() {
        create("SyncopeCreate");
    }

    @Test
    public void update() {
        SAML2IdPEntity entity = create("SyncopeUpdate");
        assertNotNull(entity);
        entity.setKey("OtherSyncope");

        entity = saml2IdPEntityDAO.save(entity);
        assertNotNull(entity);
        
        entityManager.flush();

        SAML2IdPEntity found = saml2IdPEntityDAO.findById(entity.getKey()).orElseThrow();
        assertEquals("OtherSyncope", found.getKey());
    }
}
