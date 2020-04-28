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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPMetadataDAO;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;

@Transactional("Master")
public class SAML2IdPMetadataTest extends AbstractTest {

    @Autowired
    private SAML2IdPMetadataDAO saml2IdPMetadataDAO;

    @Test
    public void find() {
        create("Syncope");
        SAML2IdPMetadata saml2IdPMetadata = saml2IdPMetadataDAO.findByOwner("Syncope");
        assertNotNull(saml2IdPMetadata);

        saml2IdPMetadata = saml2IdPMetadataDAO.findByOwner(UUID.randomUUID().toString());
        assertNull(saml2IdPMetadata);
    }

    @Test
    public void save() {
        create("SyncopeCreate");
    }

    @Test
    public void update() {
        SAML2IdPMetadata saml2IdPMetadata = create("SyncopeUpdate");
        assertNotNull(saml2IdPMetadata);
        saml2IdPMetadata.setAppliesTo("OtherSyncope");

        saml2IdPMetadata = saml2IdPMetadataDAO.save(saml2IdPMetadata);
        assertNotNull(saml2IdPMetadata);
        assertNotNull(saml2IdPMetadata.getKey());
        SAML2IdPMetadata found = saml2IdPMetadataDAO.findByOwner(saml2IdPMetadata.getAppliesTo());
        assertNotNull(found);
        assertEquals("OtherSyncope", found.getAppliesTo());
    }

    private SAML2IdPMetadata create(final String appliesTo) {
        SAML2IdPMetadata saml2IdPMetadata = entityFactory.newEntity(SAML2IdPMetadata.class);
        saml2IdPMetadata.setAppliesTo(appliesTo);
        saml2IdPMetadata.setMetadata("metadata");
        saml2IdPMetadata.setEncryptionCertificate("encryptionCert");
        saml2IdPMetadata.setEncryptionKey("encryptionKey");
        saml2IdPMetadata.setSigningCertificate("signatureCert");
        saml2IdPMetadata.setSigningKey("signatureKey");
        saml2IdPMetadataDAO.save(saml2IdPMetadata);
        assertNotNull(saml2IdPMetadata);
        assertNotNull(saml2IdPMetadata.getKey());
        assertNotNull(saml2IdPMetadataDAO.findByOwner(saml2IdPMetadata.getAppliesTo()));

        return saml2IdPMetadata;
    }

}
