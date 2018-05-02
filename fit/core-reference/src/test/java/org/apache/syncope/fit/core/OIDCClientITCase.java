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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.BasicAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCLoginRequestTO;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.apache.syncope.common.rest.api.service.OIDCProviderService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.OIDCClientDetector;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class OIDCClientITCase extends AbstractITCase {

    private static SyncopeClient anonymous;

    private static SyncopeClient admin;

    @BeforeClass
    public static void setup() {
        anonymous = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).
                create(new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));

        admin = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).
                create(new BasicAuthenticationHandler(ADMIN_UNAME, ADMIN_PWD));
    }

    @BeforeClass
    public static void createOIDCProviderWithoutDiscovery() throws Exception {
        if (!OIDCClientDetector.isOIDCClientAvailable()) {
            return;
        }

        assertTrue(oidcProviderService.list().isEmpty());

        OIDCProviderTO oidcProviderTO = new OIDCProviderTO();
        oidcProviderTO.setAuthorizationEndpoint("AuthorizationEndpoint");
        oidcProviderTO.setClientID("ClientID");
        oidcProviderTO.setClientSecret("ClientSecret");
        oidcProviderTO.setIssuer("https://accounts.google.com");
        oidcProviderTO.setJwksUri("JwksUri");
        oidcProviderTO.setName("Google");
        oidcProviderTO.setTokenEndpoint("TokenEndpoint");
        oidcProviderTO.setUserinfoEndpoint("UserinfoEndpoint");

        admin.getService(OIDCProviderService.class).create(oidcProviderTO);
    }

    @AfterClass
    public static void clearProviders() throws Exception {
        if (!OIDCClientDetector.isOIDCClientAvailable()) {
            return;
        }

        for (OIDCProviderTO op : oidcProviderService.list()) {
            oidcProviderService.delete(op.getKey());
        }
    }

    @Test
    public void createLoginRequest() {
        Assume.assumeTrue(OIDCClientDetector.isOIDCClientAvailable());

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
        Assume.assumeTrue(OIDCClientDetector.isOIDCClientAvailable());

        OIDCProviderTO ssoCircle = IterableUtils.find(oidcProviderService.list(), new Predicate<OIDCProviderTO>() {

            @Override
            public boolean evaluate(final OIDCProviderTO object) {
                return "Google".equals(object.getName());
            }
        });
        assertNotNull(ssoCircle);
        assertFalse(ssoCircle.isCreateUnmatching());
        assertNull(ssoCircle.getUserTemplate());
        assertFalse(ssoCircle.getItems().isEmpty());
        assertNotNull(ssoCircle.getConnObjectKeyItem());
        assertNotEquals("fullname", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertNotEquals("given_name", ssoCircle.getConnObjectKeyItem().getExtAttrName());

        ssoCircle.setCreateUnmatching(true);

        UserTO userTemplate = new UserTO();
        userTemplate.setRealm("'/'");
        ssoCircle.setUserTemplate(userTemplate);

        ssoCircle.getItems().clear();
        ItemTO keyMapping = new ItemTO();
        keyMapping.setIntAttrName("fullname");
        keyMapping.setExtAttrName("given_name");
        ssoCircle.setConnObjectKeyItem(keyMapping);

        oidcProviderService.update(ssoCircle);

        ssoCircle = oidcProviderService.read(ssoCircle.getKey());
        assertTrue(ssoCircle.isCreateUnmatching());
        assertEquals(userTemplate, ssoCircle.getUserTemplate());
        assertEquals("fullname", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertEquals("given_name", ssoCircle.getConnObjectKeyItem().getExtAttrName());
    }

}
