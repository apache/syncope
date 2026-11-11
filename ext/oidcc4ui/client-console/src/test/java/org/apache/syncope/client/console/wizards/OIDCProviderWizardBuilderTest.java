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
package org.apache.syncope.client.console.wizards;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.syncope.client.console.panels.OIDCProvidersDirectoryPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.OIDCProviderRestClient;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.wicket.PageReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OIDCProviderWizardBuilderTest {

    private OIDCProviderRestClient oidcProviderRestClient;

    private OIDCProviderWizardBuilder builder;

    @BeforeEach
    void setUp() {
        oidcProviderRestClient = mock(OIDCProviderRestClient.class);

        builder = new OIDCProviderWizardBuilder(
                mock(OIDCProvidersDirectoryPanel.class),
                new OIDCC4UIProviderTO(),
                mock(ImplementationRestClient.class),
                oidcProviderRestClient,
                mock(PageReference.class));
    }

    @Test
    void createWithoutDiscovery() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setName("op");
        opTO.setHasDiscovery(false);

        assertSame(opTO, builder.onApplyInternal(opTO));

        verify(oidcProviderRestClient).create(opTO);
        verify(oidcProviderRestClient, never()).createFromDiscovery(opTO);
        verify(oidcProviderRestClient, never()).update(opTO);
    }

    @Test
    void createWithDiscovery() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setIssuer("https://op.example.com");
        opTO.setHasDiscovery(true);

        assertSame(opTO, builder.onApplyInternal(opTO));

        verify(oidcProviderRestClient).createFromDiscovery(opTO);
        verify(oidcProviderRestClient, never()).create(opTO);
        verify(oidcProviderRestClient, never()).update(opTO);
    }

    @Test
    void updateExistingProvider() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");

        assertSame(opTO, builder.onApplyInternal(opTO));

        verify(oidcProviderRestClient).update(opTO);
        verify(oidcProviderRestClient, never()).create(opTO);
        verify(oidcProviderRestClient, never()).createFromDiscovery(opTO);
    }
}
