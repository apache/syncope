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
package org.apache.syncope.client.console.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2SPBeforeLogoutTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester();
        tester.setFollowRedirects(false);
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    @Test
    void redirectsToLogoutResourceWithIdP() {
        tester.getSession().setAttribute(
                SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID, "https://idp.example.com");

        tester.startPage(SAML2SPBeforeLogout.class);

        String location = tester.getLastResponse().getRedirectLocation();
        assertNotNull(location);
        assertTrue(location.contains("saml2sp4ui/logout?"));
        assertTrue(location.contains(
                SAML2SP4UIConstants.SAML2SP4UI_IDP_ENTITY_ID + "=https%3A%2F%2Fidp.example.com"), location);
    }

    @Test
    void redirectsToLogoutResourceWithoutIdP() {
        tester.startPage(SAML2SPBeforeLogout.class);

        String location = tester.getLastResponse().getRedirectLocation();
        assertNotNull(location);
        assertTrue(location.endsWith("saml2sp4ui/logout"), location);
    }
}
