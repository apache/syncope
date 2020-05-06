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
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class SAML2SPKeystoreITCase extends AbstractITCase {

    private static final String OWNER = "Syncope";

    private static void testIsValid(final SAML2SPKeystoreTO keystoreTO) {
        assertFalse(StringUtils.isBlank(keystoreTO.getOwner()));
        assertFalse(StringUtils.isBlank(keystoreTO.getKeystore()));
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
            keystoreTO = saml2SPKeystoreService.get(OWNER);
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
            saml2SPMetadataService.get(OWNER);
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

    @Test
    public void update() {
        SAML2SPKeystoreTO keystoreTO;
        try {
            keystoreTO = saml2SPKeystoreService.get(OWNER);
        } catch (NotFoundException e) {
            keystoreTO = createSAML2SPKeystore();
        }
        assertNotNull(keystoreTO);
        keystoreTO.setKeystore("new-keystore");
        keystoreTO.setOwner("Syncope4");

        saml2SPKeystoreConfService.update(keystoreTO);
        keystoreTO = saml2SPKeystoreService.read(keystoreTO.getKey());
        assertNotNull(keystoreTO);
        assertEquals("new-keystore", keystoreTO.getKeystore());
        assertEquals("Syncope4", keystoreTO.getOwner());
    }

}
