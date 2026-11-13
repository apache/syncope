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
import static org.mockito.Mockito.verify;

import org.apache.syncope.client.console.panels.SAML2IdPsDirectoryPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.wicket.PageReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2IdPWizardBuilderTest {

    private SAML2IdPsRestClient saml2IdPsRestClient;

    private SAML2IdPWizardBuilder builder;

    @BeforeEach
    void setUp() {
        saml2IdPsRestClient = mock(SAML2IdPsRestClient.class);

        builder = new SAML2IdPWizardBuilder(
                mock(SAML2IdPsDirectoryPanel.class),
                new SAML2SP4UIIdPTO(),
                mock(ImplementationRestClient.class),
                saml2IdPsRestClient,
                mock(PageReference.class));
    }

    @Test
    void applyRequiresSingleConnObjectKey() {
        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        Item connObjectKey = new Item();
        connObjectKey.setIntAttrName("username");
        connObjectKey.setExtAttrName("NameID");
        connObjectKey.setConnObjectKey(true);
        idpTO.getItems().add(connObjectKey);

        assertSame(idpTO, builder.onApplyInternal(idpTO));

        verify(saml2IdPsRestClient).update(idpTO);
    }
}
