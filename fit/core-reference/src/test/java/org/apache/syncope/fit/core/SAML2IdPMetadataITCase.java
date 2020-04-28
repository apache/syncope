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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.junit.platform.commons.util.StringUtils;

public class SAML2IdPMetadataITCase extends AbstractITCase {
    
    private static final String APPLIES_TO = "Syncope";

    private SAML2IdPMetadataTO createSAML2IdPMetadata() {
        SAML2IdPMetadataTO result = createSAML2IdPMetadata(new SAML2IdPMetadataTO.Builder().
                appliesTo(APPLIES_TO).
                metadata("testMetadata").
                encryptionCertificate("testEncryptionCert").
                encryptionKey("testEncryptionKey").
                signingCertificate("testSigningCert").
                signingKey("testSigningKey").
                build());
        assertNotNull(result);
        testIsValid(result);

        return result;
    }

    private void testIsValid(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getAppliesTo()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getMetadata()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getEncryptionKey()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getEncryptionCertificate()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getSigningCertificate()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getSigningKey()));
    }

    @Test
    public void read() {
        SAML2IdPMetadataTO saml2IdPMetadataTO = null;
        try {
            saml2IdPMetadataTO = saml2IdPMetadataService.get(APPLIES_TO);
        } catch (SyncopeClientException e) {
            saml2IdPMetadataTO = createSAML2IdPMetadata();
        }

        assertNotNull(saml2IdPMetadataTO);
        assertEquals(APPLIES_TO, saml2IdPMetadataTO.getAppliesTo());
        testIsValid(saml2IdPMetadataTO);
    }

    @Test
    public void create() {
        try {
            saml2IdPMetadataService.get(APPLIES_TO);
        } catch (SyncopeClientException e) {
            createSAML2IdPMetadata();
        }

        try {
            createSAML2IdPMetadata(new SAML2IdPMetadataTO.Builder().
                    appliesTo(APPLIES_TO).
                    metadata("testMetadata").
                    build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void update() {
        SAML2IdPMetadataTO saml2IdPMetadataTO = null;
        try {
            saml2IdPMetadataTO = saml2IdPMetadataService.get(APPLIES_TO);
        } catch (NotFoundException e) {
            saml2IdPMetadataTO = createSAML2IdPMetadata();
        }

        assertNotNull(saml2IdPMetadataTO);
        saml2IdPMetadataTO.setEncryptionKey("newKey");
        saml2IdPMetadataConfService.update(saml2IdPMetadataTO);
        saml2IdPMetadataTO = saml2IdPMetadataService.get(saml2IdPMetadataTO.getAppliesTo());
        assertNotNull(saml2IdPMetadataTO);

        assertEquals("newKey", saml2IdPMetadataTO.getEncryptionKey());
    }

}
