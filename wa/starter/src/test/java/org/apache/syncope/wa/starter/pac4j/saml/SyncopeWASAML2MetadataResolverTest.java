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
package org.apache.syncope.wa.starter.pac4j.saml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.rest.api.service.SAML2SPMetadataService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.core.io.ClassPathResource;

public class SyncopeWASAML2MetadataResolverTest extends BaseSyncopeWASAML2ClientTest {

    @Test
    public void fetchMetadata() throws Exception {
        SAML2Client client = getSAML2Client();
        String keystoreFile = File.createTempFile("keystore", "jks").getCanonicalPath();
        client.getConfiguration().setKeystoreResourceFilepath(keystoreFile);
        WARestClient restClient = mock(WARestClient.class);

        SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO.Builder()
                .owner("Syncope")
                .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(),
                        StandardCharsets.UTF_8))
                .build();

        SAML2SPMetadataService saml2SPMetadataService = mock(SAML2SPMetadataService.class);
        when(saml2SPMetadataService.readFor(anyString())).thenReturn(metadataTO);
        when(saml2SPMetadataService.set(any())).thenReturn(Response.created(new URI("http://localhost:9081/syncop-wa")).
                build());

        SyncopeClient syncopeClient = mock(SyncopeClient.class);
        when(syncopeClient.getService(SAML2SPMetadataService.class)).thenReturn(saml2SPMetadataService);
        when(restClient.getSyncopeClient()).thenReturn(syncopeClient);

        SyncopeWASAML2MetadataResolver resolver = new SyncopeWASAML2MetadataResolver(restClient, client);
        assertNotNull(resolver.fetchMetadata());
    }
}
