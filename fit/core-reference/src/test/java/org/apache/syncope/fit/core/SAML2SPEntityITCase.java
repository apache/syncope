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
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SAML2SPEntityITCase extends AbstractITCase {

    private static final String OWNER = "owner";

    private static SAML2SPEntityService WA_SAML2SP_ENTITY_SERVICE;

    @BeforeAll
    public static void setup() {
        assumeTrue(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        WA_SAML2SP_ENTITY_SERVICE = ANONYMOUS_CLIENT.getService(SAML2SPEntityService.class);
    }

    private static SAML2SPEntityTO set() {
        SAML2SPEntityTO entityTO = new SAML2SPEntityTO.Builder().
                key(OWNER).
                metadata(Base64.getEncoder().encodeToString("testMetadata".getBytes(StandardCharsets.UTF_8))).
                build();
        WA_SAML2SP_ENTITY_SERVICE.set(entityTO);

        return entityTO;
    }

    @Test
    public void get() {
        SAML2SPEntityTO entityTO;
        try {
            entityTO = WA_SAML2SP_ENTITY_SERVICE.get(OWNER);
        } catch (SyncopeClientException e) {
            entityTO = set();
        }
        assertNotNull(entityTO);

        assertEquals(OWNER, entityTO.getKey());
    }

    @Test
    public void getAndSet() {
        SAML2SPEntityTO entityTO;
        try {
            entityTO = WA_SAML2SP_ENTITY_SERVICE.get(OWNER);
        } catch (SyncopeClientException e) {
            entityTO = set();
        }
        assertNotNull(entityTO);

        entityTO.setMetadata(Base64.getEncoder().encodeToString("new metadata".getBytes(StandardCharsets.UTF_8)));
        WA_SAML2SP_ENTITY_SERVICE.set(entityTO);

        entityTO = WA_SAML2SP_ENTITY_SERVICE.get(entityTO.getKey());
        assertEquals(
                "new metadata",
                new String(Base64.getDecoder().decode(entityTO.getMetadata()), StandardCharsets.UTF_8));
    }
}
