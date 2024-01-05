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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SAML2SPTest extends AbstractClientAppTest {

    @Autowired
    private SAML2SPClientAppDAO saml2spDAO;

    @Test
    public void find() {
        long beforeCount = saml2spDAO.count();

        SAML2SPClientApp sp = entityFactory.newEntity(SAML2SPClientApp.class);
        sp.setName("SAML2");
        sp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        sp.setDescription("This is a sample SAML2 SP");
        sp.setEntityId("urn:example:saml2:sp");
        sp.setMetadataLocation("https://example.org/metadata.xml");
        sp.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        sp.setEncryptionOptional(true);
        sp.setEncryptAssertions(true);
        sp.getEncryptionDataAlgorithms().add(XmlSecAlgorithm.AES_128_GCM);
        sp.getEncryptionKeyAlgorithms().add(XmlSecAlgorithm.RSA_OAEP_11);
        sp.getSigningSignatureReferenceDigestMethods().add(XmlSecAlgorithm.SHA1);
        sp.getSigningSignatureAlgorithms().add(XmlSecAlgorithm.SHA256);
        sp.getSigningSignatureAlgorithms().add(XmlSecAlgorithm.SHA512);

        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        sp.setAccessPolicy(accessPolicy);

        AuthPolicy authnPolicy = buildAndSaveAuthPolicy();
        sp.setAuthPolicy(authnPolicy);

        saml2spDAO.save(sp);

        assertNotNull(sp);
        assertNotNull(sp.getKey());

        long afterCount = saml2spDAO.count();
        assertEquals(afterCount, beforeCount + 1);

        sp = saml2spDAO.findByEntityId(sp.getEntityId()).orElseThrow();
        assertNotNull(sp);

        sp = saml2spDAO.findByName(sp.getName()).orElseThrow();
        assertNotNull(sp);

        sp = saml2spDAO.findByClientAppId(sp.getClientAppId()).orElseThrow();
        assertNotNull(sp);

        assertFalse(sp.getSigningSignatureAlgorithms().isEmpty());
        assertFalse(sp.getSigningSignatureReferenceDigestMethods().isEmpty());
        assertFalse(sp.getEncryptionDataAlgorithms().isEmpty());
        assertFalse(sp.getEncryptionKeyAlgorithms().isEmpty());

        saml2spDAO.deleteByEntityId(sp.getEntityId());
        assertTrue(saml2spDAO.findByName(sp.getName()).isEmpty());
    }
}
