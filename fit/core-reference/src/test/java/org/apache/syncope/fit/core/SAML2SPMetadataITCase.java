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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.WASAML2SPMetadataService;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SAML2SPMetadataITCase extends AbstractITCase {

    private static WASAML2SPMetadataService waSAML2SPMetadataService;

    @BeforeAll
    public static void setup() {
        assumeTrue(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        waSAML2SPMetadataService = anonymous.getService(WASAML2SPMetadataService.class);
    }

    private SAML2SPMetadataTO createSAML2SPMetadata(final SAML2SPMetadataTO metadata) {
        Response response = waSAML2SPMetadataService.set(metadata);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), WASAML2SPMetadataService.class, metadata.getClass());
    }

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
            metadataTO = waSAML2SPMetadataService.getByOwner(OWNER);
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
            waSAML2SPMetadataService.getByOwner(OWNER);
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
            metadataTO = waSAML2SPMetadataService.getByOwner(OWNER);
        } catch (NotFoundException e) {
            metadataTO = createSAML2SPMetadata();
        }
        assertNotNull(metadataTO);
        metadataTO.setMetadata("new-metadata");
        metadataTO.setOwner("Syncope4");

        saml2SPMetadataService.update(metadataTO);
        metadataTO = waSAML2SPMetadataService.read(metadataTO.getKey());
        assertNotNull(metadataTO);
        assertEquals("new-metadata", metadataTO.getMetadata());
        assertEquals("Syncope4", metadataTO.getOwner());
    }
}
