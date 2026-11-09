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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SAML2IdPEntityDataBinderTest extends AbstractDataBinderTest {

    private static final String KEY = "test-saml2-idp";

    @Autowired
    private SAML2IdPEntityDataBinder binder;

    @Autowired
    private SAML2IdPEntityDAO saml2IdPEntityDAO;

    protected String base64(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createGetUpdate() {
        SAML2IdPEntityTO entityTO = new SAML2IdPEntityTO();
        entityTO.setKey(KEY);
        entityTO.setMetadata(base64("metadata"));
        entityTO.setSigningCertificate(base64("signingCertificate"));
        entityTO.setSigningKey(base64("signingKey"));
        entityTO.setEncryptionCertificate(base64("encryptionCertificate"));
        entityTO.setEncryptionKey(base64("encryptionKey"));

        SAML2IdPEntity created = saml2IdPEntityDAO.save(binder.create(entityTO));
        entityManager.flush();

        SAML2IdPEntityTO actual = binder.getSAML2IdPEntityTO(
                saml2IdPEntityDAO.findById(KEY).orElseThrow());
        assertEquals(KEY, actual.getKey());
        assertEquals(base64("metadata"), actual.getMetadata());
        assertEquals(base64("signingCertificate"), actual.getSigningCertificate());
        assertEquals(base64("signingKey"), actual.getSigningKey());
        assertEquals(base64("encryptionCertificate"), actual.getEncryptionCertificate());
        assertEquals(base64("encryptionKey"), actual.getEncryptionKey());

        SAML2IdPEntityTO update = new SAML2IdPEntityTO();
        update.setKey(KEY);
        update.setMetadata(base64("updatedMetadata"));
        binder.update(created, update);
        saml2IdPEntityDAO.save(created);
        entityManager.flush();

        SAML2IdPEntityTO updated = binder.getSAML2IdPEntityTO(
                saml2IdPEntityDAO.findById(KEY).orElseThrow());
        assertEquals(base64("updatedMetadata"), updated.getMetadata());
        assertNull(updated.getSigningCertificate());
        assertNull(updated.getSigningKey());
        assertNull(updated.getEncryptionCertificate());
        assertNull(updated.getEncryptionKey());

        SAML2IdPEntity entity = saml2IdPEntityDAO.findById(KEY).orElseThrow();
        assertArrayEquals("updatedMetadata".getBytes(StandardCharsets.UTF_8), entity.getMetadata());
    }
}
