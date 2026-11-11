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
package org.apache.syncope.core.logic.saml2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

class SAML2ClientCacheTest {

    @Test
    void serviceProviderMetadataPathIsDerivedFromEntityID() {
        String path = SAML2ClientCache.getSPMetadataPath("https://idp.example.com/idp").orElseThrow();

        assertTrue(path.endsWith("idp.example.com_idp"), path);

        String httpPath = SAML2ClientCache.getSPMetadataPath("http://idp.example.com/idp").orElseThrow();
        assertEquals(path, httpPath);
    }

    private static SAML2Client client(final String idpEntityID, final String spEntityID) {
        SAML2Configuration configuration = mock(SAML2Configuration.class);
        when(configuration.getServiceProviderEntityId()).thenReturn(spEntityID);

        SAML2Client client = mock(SAML2Client.class);
        when(client.getIdentityProviderResolvedEntityId()).thenReturn(idpEntityID);
        when(client.getConfiguration()).thenReturn(configuration);
        return client;
    }

    @Test
    void getAndRemoveAll() {
        SAML2ClientCache cache = new SAML2ClientCache();
        SAML2Client matching = client("idp1", "sp");
        SAML2Client otherIdP = client("idp2", "sp");
        SAML2Client otherSP = client("idp1", "otherSP");
        cache.cache.add(matching);
        cache.cache.add(otherIdP);
        cache.cache.add(otherSP);

        assertSame(matching, cache.get("idp1", "sp").orElseThrow());
        assertTrue(cache.get("idp3", "sp").isEmpty());
        assertTrue(cache.get("idp1", "unknown").isEmpty());

        assertTrue(cache.removeAll("idp1"));
        assertTrue(cache.get("idp1", "sp").isEmpty());
        assertSame(otherIdP, cache.get("idp2", "sp").orElseThrow());
        assertFalse(cache.removeAll("idp1"));
    }

    @Test
    void emptyCache() {
        SAML2ClientCache cache = new SAML2ClientCache();

        assertEquals(Optional.empty(), cache.get("idp", "sp"));
        assertFalse(cache.removeAll("idp"));
    }
}
