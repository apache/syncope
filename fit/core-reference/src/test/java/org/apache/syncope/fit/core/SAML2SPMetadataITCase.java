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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class SAML2SPMetadataITCase extends AbstractITCase {

    private static final String OWNER = "Syncope";

    private static void testIsValid(final SAML2SPMetadataTO metadataTO) {
        assertFalse(StringUtils.isBlank(metadataTO.getOwner()));
        assertFalse(StringUtils.isBlank(metadataTO.getMetadata()));
    }

    private SAML2SPMetadataTO createSAML2SPMetadata() {
        SAML2SPMetadataTO result = createSAML2SPMetadata(new SAML2SPMetadataTO.Builder().
            owner(OWNER).
            metadata("testMetadata").
            build());
        assertNotNull(result);
        testIsValid(result);
        return result;
    }

    @Test
    public void read() {
        SAML2SPMetadataTO metadataTO;
        try {
            metadataTO = saml2SPMetadataService.getByOwner(OWNER);
        } catch (SyncopeClientException e) {
            metadataTO = createSAML2SPMetadata();
        }
        assertNotNull(metadataTO);
        assertEquals(OWNER, metadataTO.getOwner());
        testIsValid(metadataTO);
    }

    @Test
    public void create() {
        try {
            saml2SPMetadataService.getByOwner(OWNER);
        } catch (SyncopeClientException e) {
            createSAML2SPMetadata();
        }

        try {
            createSAML2SPMetadata(new SAML2SPMetadataTO.Builder().
                owner(OWNER).
                metadata("testMetadata").
                build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void update() {
        SAML2SPMetadataTO metadataTO;
        try {
            metadataTO = saml2SPMetadataService.getByOwner(OWNER);
        } catch (NotFoundException e) {
            metadataTO = createSAML2SPMetadata();
        }
        assertNotNull(metadataTO);
        metadataTO.setMetadata("new-metadata");
        metadataTO.setOwner("Syncope4");

        saml2SPMetadataConfService.update(metadataTO);
        metadataTO = saml2SPMetadataService.read(metadataTO.getKey());
        assertNotNull(metadataTO);
        assertEquals("new-metadata", metadataTO.getMetadata());
        assertEquals("Syncope4", metadataTO.getOwner());
    }

}
