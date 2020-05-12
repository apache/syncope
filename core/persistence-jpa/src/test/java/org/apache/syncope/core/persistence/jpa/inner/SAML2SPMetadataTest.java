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

import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPMetadataDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPMetadata;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.tika.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Transactional("Master")
public class SAML2SPMetadataTest extends AbstractTest {

    @Autowired
    private SAML2SPMetadataDAO saml2SPMetadataDAO;

    @Test
    public void find() throws Exception {
        create("Syncope");
        SAML2SPMetadata saml2SPMetadata = saml2SPMetadataDAO.findByOwner("Syncope");
        assertNotNull(saml2SPMetadata);

        saml2SPMetadata = saml2SPMetadataDAO.findByOwner(UUID.randomUUID().toString());
        assertNull(saml2SPMetadata);
    }

    @Test
    public void save() throws Exception {
        create("SyncopeCreate");
    }

    @Test
    public void update() throws Exception {
        SAML2SPMetadata saml2SPMetadata = create("SyncopeUpdate");
        assertNotNull(saml2SPMetadata);
        saml2SPMetadata.setOwner("OtherSyncope");

        saml2SPMetadata = saml2SPMetadataDAO.save(saml2SPMetadata);
        assertNotNull(saml2SPMetadata);
        assertNotNull(saml2SPMetadata.getKey());
        SAML2SPMetadata found = saml2SPMetadataDAO.findByOwner(saml2SPMetadata.getOwner());
        assertNotNull(found);
        assertEquals("OtherSyncope", found.getOwner());
    }

    private SAML2SPMetadata create(final String owner) throws Exception {
        SAML2SPMetadata saml2SPMetadata = entityFactory.newEntity(SAML2SPMetadata.class);
        saml2SPMetadata.setOwner(owner);
        String metadata = IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream());
        saml2SPMetadata.setMetadata(metadata);
        saml2SPMetadataDAO.save(saml2SPMetadata);
        assertNotNull(saml2SPMetadata);
        assertNotNull(saml2SPMetadata.getKey());
        assertNotNull(saml2SPMetadataDAO.findByOwner(saml2SPMetadata.getOwner()));
        return saml2SPMetadata;
    }

}
