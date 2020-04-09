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
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;

@Transactional("Master")
public class SAML2SPTest extends AbstractClientAppTest {

    @Autowired
    private SAML2SPDAO saml2spDAO;

    @Test
    public void find() {
        int beforeCount = saml2spDAO.findAll().size();
        SAML2SP sp = entityFactory.newEntity(SAML2SP.class);
        sp.setName("SAML2");
        sp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        sp.setDescription("This is a sample SAML2 SP");
        sp.setEntityId("urn:example:saml2:sp");
        sp.setMetadataLocation("https://example.org/metadata.xml");
        sp.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        sp.setEncryptionOptional(true);
        sp.setEncryptAssertions(true);

        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        sp.setAccessPolicy(accessPolicy);

        AuthPolicy authnPolicy = buildAndSaveAuthPolicy();
        sp.setAuthPolicy(authnPolicy);

        saml2spDAO.save(sp);

        assertNotNull(sp);
        assertNotNull(sp.getKey());

        int afterCount = saml2spDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);

        sp = saml2spDAO.findByEntityId(sp.getEntityId());
        assertNotNull(sp);

        sp = saml2spDAO.findByName(sp.getName());
        assertNotNull(sp);

        sp = saml2spDAO.findByClientAppId(sp.getClientAppId());
        assertNotNull(sp);

        saml2spDAO.deleteByEntityId(sp.getEntityId());
        assertNull(saml2spDAO.findByName(sp.getName()));
    }
}
