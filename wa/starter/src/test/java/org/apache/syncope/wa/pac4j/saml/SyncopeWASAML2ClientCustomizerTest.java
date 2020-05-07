/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.wa.pac4j.saml;

import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.rest.api.service.SAML2SPKeystoreService;
import org.apache.syncope.common.rest.api.service.SAML2SPMetadataService;
import org.apache.syncope.wa.WARestClient;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncopeWASAML2ClientCustomizerTest extends BaseSyncopeWASAML2Client {

    @Test
    public void customize() throws Exception {

        SAML2SPKeystoreTO keystoreTO = new SAML2SPKeystoreTO.Builder()
            .keystore(getKeystoreAsString())
            .owner("CAS")
            .build();
        SAML2SPKeystoreService saml2SPKeystoreService = mock(SAML2SPKeystoreService.class);
        when(saml2SPKeystoreService.get(anyString())).thenReturn(keystoreTO);
        when(saml2SPKeystoreService.set(any())).thenReturn(Response.ok().build());

        SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO.Builder()
            .owner("Syncope")
            .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(), StandardCharsets.UTF_8))
            .build();

        SAML2SPMetadataService saml2SPMetadataService = mock(SAML2SPMetadataService.class);
        when(saml2SPMetadataService.get(anyString())).thenReturn(metadataTO);
        when(saml2SPMetadataService.set(any())).thenReturn(Response.ok().build());

        WARestClient restClient = mock(WARestClient.class);

        SyncopeClient syncopeClient = mock(SyncopeClient.class);
        when(syncopeClient.getService(SAML2SPKeystoreService.class)).thenReturn(saml2SPKeystoreService);
        when(syncopeClient.getService(SAML2SPMetadataService.class)).thenReturn(saml2SPMetadataService);
        when(restClient.getSyncopeClient()).thenReturn(syncopeClient);

        SyncopeWASAML2ClientCustomizer customizer = new SyncopeWASAML2ClientCustomizer(restClient);
        SAML2Client client = getSAML2Client();
        customizer.customize(client);
        client.init();
        assertTrue(client.getConfiguration().getKeystoreGenerator() instanceof SyncopeWASAML2ClientKeystoreGenerator);
        assertTrue(client.getConfiguration().toMetadataGenerator() instanceof SyncopeWASAML2ClientMetadataGenerator);
    }
}
