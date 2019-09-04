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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCLoginRequestTO;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.OIDCClientDetector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OIDCClientITCase extends AbstractITCase {

    @BeforeAll
    public static void createOIDCProviderWithoutDiscovery() throws Exception {
        if (!OIDCClientDetector.isOIDCClientAvailable()) {
            return;
        }

        assertTrue(oidcProviderService.list().isEmpty());

        OIDCProviderTO google = new OIDCProviderTO();
        google.setAuthorizationEndpoint("AuthorizationEndpoint");
        google.setClientID("ClientID");
        google.setClientSecret("ClientSecret");
        google.setIssuer("https://accounts.google.com");
        google.setJwksUri("JwksUri");
        google.setName("Google");
        google.setTokenEndpoint("TokenEndpoint");
        google.setUserinfoEndpoint("UserinfoEndpoint");

        oidcProviderService.create(google);
    }

    @AfterAll
    public static void clearProviders() throws Exception {
        if (!OIDCClientDetector.isOIDCClientAvailable()) {
            return;
        }

        oidcProviderService.list().forEach(op -> oidcProviderService.delete(op.getKey()));
    }

    @Test
    public void createLoginRequest() {
        assumeTrue(OIDCClientDetector.isOIDCClientAvailable());

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        OIDCLoginRequestTO loginRequest = anonymous.getService(OIDCClientService.class).
                createLoginRequest("http://localhost:9080/syncope-console/oidcclient/code-consumer", "Google");

        assertNotNull(loginRequest);
        assertEquals("http://localhost:9080/syncope-console/oidcclient/code-consumer", loginRequest.getRedirectURI());
        assertNotNull(loginRequest.getProviderAddress());
        assertNotNull(loginRequest.getClientId());
        assertNotNull(loginRequest.getResponseType());
        assertNotNull(loginRequest.getScope());
        assertNotNull(loginRequest.getState());
    }

    @Test
    public void setProviderMapping() {
        assumeTrue(OIDCClientDetector.isOIDCClientAvailable());

        OIDCProviderTO google = oidcProviderService.list().stream().
                filter(object -> "Google".equals(object.getName())).findFirst().orElse(null);
        assertNotNull(google);
        assertFalse(google.isCreateUnmatching());
        assertNull(google.getUserTemplate());
        assertFalse(google.getItems().isEmpty());
        assertNotEquals("fullname", google.getConnObjectKeyItem().getIntAttrName());
        assertNotEquals("given_name", google.getConnObjectKeyItem().getExtAttrName());

        google.setCreateUnmatching(true);

        UserTO userTemplate = new UserTO();
        userTemplate.setRealm("'/'");
        google.setUserTemplate(userTemplate);

        google.getItems().clear();
        ItemTO keyMapping = new ItemTO();
        keyMapping.setIntAttrName("fullname");
        keyMapping.setExtAttrName("given_name");
        google.setConnObjectKeyItem(keyMapping);

        oidcProviderService.update(google);

        google = oidcProviderService.read(google.getKey());
        assertTrue(google.isCreateUnmatching());
        assertEquals(userTemplate, google.getUserTemplate());
        assertEquals("fullname", google.getConnObjectKeyItem().getIntAttrName());
        assertEquals("given_name", google.getConnObjectKeyItem().getExtAttrName());
    }
}
