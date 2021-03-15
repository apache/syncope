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
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.SAML2SPKeystoreService;
import org.apache.syncope.common.rest.api.service.SAML2SPMetadataService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SAML2SPKeystoreITCase extends AbstractITCase {

    private static SAML2SPKeystoreService waSAML2SPKeystoreService;

    private static SAML2SPMetadataService waSAML2SPMetadataService;

    @BeforeAll
    public static void setup() {
        assumeTrue(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        waSAML2SPKeystoreService = anonymous.getService(SAML2SPKeystoreService.class);
        waSAML2SPMetadataService = anonymous.getService(SAML2SPMetadataService.class);
    }

    private static void testIsValid(final SAML2SPKeystoreTO keystoreTO) {
        assertFalse(StringUtils.isBlank(keystoreTO.getOwner()));
        assertFalse(StringUtils.isBlank(keystoreTO.getKeystore()));
    }

    private SAML2SPKeystoreTO createSAML2SPKeystore(final SAML2SPKeystoreTO keystoreTO) {
        Response response = waSAML2SPKeystoreService.set(keystoreTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), SAML2SPKeystoreService.class, keystoreTO.getClass());
    }

    private SAML2SPKeystoreTO createSAML2SPKeystore() {
        SAML2SPKeystoreTO result = createSAML2SPKeystore(new SAML2SPKeystoreTO.Builder().
                owner(OWNER).
                keystore("testkyStore").
                build());
        assertNotNull(result);
        testIsValid(result);
        return result;
    }

    @Test
    public void read() {
        SAML2SPKeystoreTO keystoreTO;
        try {
            keystoreTO = waSAML2SPKeystoreService.readFor(OWNER);
        } catch (SyncopeClientException e) {
            keystoreTO = createSAML2SPKeystore();
        }
        assertNotNull(keystoreTO);
        assertEquals(OWNER, keystoreTO.getOwner());
        testIsValid(keystoreTO);
    }

    @Test
    public void create() {
        try {
            waSAML2SPMetadataService.readFor(OWNER);
        } catch (SyncopeClientException e) {
            createSAML2SPKeystore();
        }

        try {
            createSAML2SPKeystore(new SAML2SPKeystoreTO.Builder().
                    owner(OWNER).
                    keystore("testMetadata").
                    build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
