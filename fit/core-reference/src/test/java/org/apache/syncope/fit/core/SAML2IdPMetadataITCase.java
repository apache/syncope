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

import static org.apache.syncope.fit.AbstractITCase.getObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.WASAML2IdPMetadataService;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.commons.util.StringUtils;

public class SAML2IdPMetadataITCase extends AbstractITCase {

    private static WASAML2IdPMetadataService waSAML2IdPMetadataService;

    @BeforeAll
    public static void setup() {
        assumeTrue(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        waSAML2IdPMetadataService = anonymous.getService(WASAML2IdPMetadataService.class);
    }

    private static void testIsValid(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getAppliesTo()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getMetadata()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getEncryptionKey()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getEncryptionCertificate()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getSigningCertificate()));
        assertFalse(StringUtils.isBlank(saml2IdPMetadataTO.getSigningKey()));
    }

    private SAML2IdPMetadataTO createSAML2IdPMetadata(final SAML2IdPMetadataTO saml2IdPMetadata) {
        Response response = waSAML2IdPMetadataService.set(saml2IdPMetadata);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), WASAML2IdPMetadataService.class, saml2IdPMetadata.getClass());
    }

    private SAML2IdPMetadataTO createSAML2IdPMetadata() {
        SAML2IdPMetadataTO result = createSAML2IdPMetadata(new SAML2IdPMetadataTO.Builder().
                appliesTo(OWNER).
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

    @Test
    public void read() {
        SAML2IdPMetadataTO saml2IdPMetadataTO;
        try {
            saml2IdPMetadataTO = waSAML2IdPMetadataService.getByOwner(OWNER);
        } catch (SyncopeClientException e) {
            saml2IdPMetadataTO = createSAML2IdPMetadata();
        }

        assertNotNull(saml2IdPMetadataTO);
        assertEquals(OWNER, saml2IdPMetadataTO.getAppliesTo());
        testIsValid(saml2IdPMetadataTO);
    }

    @Test
    public void create() {
        try {
            waSAML2IdPMetadataService.getByOwner(OWNER);
        } catch (SyncopeClientException e) {
            createSAML2IdPMetadata();
        }

        try {
            createSAML2IdPMetadata(new SAML2IdPMetadataTO.Builder().
                    appliesTo(OWNER).
                    metadata("testMetadata").
                    build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void update() {
        SAML2IdPMetadataTO saml2IdPMetadataTO;
        try {
            saml2IdPMetadataTO = waSAML2IdPMetadataService.getByOwner(OWNER);
        } catch (NotFoundException e) {
            saml2IdPMetadataTO = createSAML2IdPMetadata();
        }

        assertNotNull(saml2IdPMetadataTO);
        saml2IdPMetadataTO.setEncryptionKey("newKey");
        saml2IdPMetadataService.update(saml2IdPMetadataTO);
        saml2IdPMetadataTO = waSAML2IdPMetadataService.getByOwner(saml2IdPMetadataTO.getAppliesTo());
        assertNotNull(saml2IdPMetadataTO);

        assertEquals("newKey", saml2IdPMetadataTO.getEncryptionKey());
    }
}
