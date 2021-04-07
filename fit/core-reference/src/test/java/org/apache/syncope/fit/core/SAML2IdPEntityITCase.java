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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.junit.jupiter.api.BeforeAll;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;

public class SAML2IdPEntityITCase extends AbstractITCase {

    private static SAML2IdPEntityService waSAML2IdPEntityService;

    @BeforeAll
    public static void setup() {
        assumeTrue(clientFactory.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        waSAML2IdPEntityService = anonymous.getService(SAML2IdPEntityService.class);
    }

    private static SAML2IdPEntityTO set() {
        SAML2IdPEntityTO entityTO = new SAML2IdPEntityTO.Builder().
                key(SAML2IdPEntityService.DEFAULT_OWNER).
                metadata(Base64.getEncoder().encodeToString(
                        "testMetadata".getBytes(StandardCharsets.UTF_8))).
                encryptionCertificate(Base64.getEncoder().encodeToString(
                        "testEncryptionCert".getBytes(StandardCharsets.UTF_8))).
                encryptionKey(Base64.getEncoder().encodeToString(
                        "testEncryptionKey".getBytes(StandardCharsets.UTF_8))).
                signingCertificate(Base64.getEncoder().encodeToString(
                        "testSigningCert".getBytes(StandardCharsets.UTF_8))).
                signingKey(Base64.getEncoder().encodeToString(
                        "testSigningKey".getBytes(StandardCharsets.UTF_8))).
                build();
        waSAML2IdPEntityService.set(entityTO);

        return entityTO;
    }

    @Test
    public void get() {
        SAML2IdPEntityTO entityTO;
        try {
            entityTO = waSAML2IdPEntityService.get(SAML2IdPEntityService.DEFAULT_OWNER);
        } catch (SyncopeClientException e) {
            entityTO = set();
        }
        assertNotNull(entityTO);

        assertEquals(SAML2IdPEntityService.DEFAULT_OWNER, entityTO.getKey());
    }

    @Test
    public void getAndSet() {
        SAML2IdPEntityTO entityTO;
        try {
            entityTO = waSAML2IdPEntityService.get(SAML2IdPEntityService.DEFAULT_OWNER);
        } catch (SyncopeClientException e) {
            entityTO = set();
        }
        assertNotNull(entityTO);

        entityTO.setMetadata(Base64.getEncoder().encodeToString("new metadata".getBytes(StandardCharsets.UTF_8)));
        waSAML2IdPEntityService.set(entityTO);

        entityTO = waSAML2IdPEntityService.get(entityTO.getKey());
        assertEquals(
                "new metadata",
                new String(Base64.getDecoder().decode(entityTO.getMetadata()), StandardCharsets.UTF_8));
    }
}
