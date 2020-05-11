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

package org.apache.syncope.wa.starter.pac4j.saml;

import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.rest.api.service.wa.SAML2SPMetadataService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.SAML2MetadataGenerator;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncopeWASAML2ClientMetadataGeneratorTest extends BaseSyncopeWASAML2ClientTest {
    private static WARestClient getWaRestClient(final Response response) throws IOException {
        WARestClient restClient = mock(WARestClient.class);
        SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO.Builder()
            .owner("Syncope")
            .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(), StandardCharsets.UTF_8))
            .build();

        SAML2SPMetadataService saml2SPMetadataService = mock(SAML2SPMetadataService.class);
        when(saml2SPMetadataService.getByOwner(anyString())).thenReturn(metadataTO);
        when(saml2SPMetadataService.set(any())).thenReturn(response);

        SyncopeClient syncopeClient = mock(SyncopeClient.class);
        when(syncopeClient.getService(SAML2SPMetadataService.class)).thenReturn(saml2SPMetadataService);
        when(restClient.getSyncopeClient()).thenReturn(syncopeClient);
        return restClient;
    }

    @Test
    public void storeMetadata() throws Exception {
        SAML2Client client = getSAML2Client();
        String keystoreFile = File.createTempFile("keystore", "jks").getCanonicalPath();
        client.getConfiguration().setKeystoreResourceFilepath(keystoreFile);

        SAML2MetadataGenerator generator = new SyncopeWASAML2ClientMetadataGenerator(
            getWaRestClient(Response.created(new URI("http://localhost:9080/syncop-wa")).build()), client);
        EntityDescriptor entityDescriptor = generator.buildEntityDescriptor();
        String metadata = generator.getMetadata(entityDescriptor);
        assertNotNull(generator.storeMetadata(metadata, null, false));
    }

    @Test
    public void storeMetadataFails() throws Exception {
        SAML2Client client = getSAML2Client();
        String keystoreFile = File.createTempFile("keystore", "jks").getCanonicalPath();
        client.getConfiguration().setKeystoreResourceFilepath(keystoreFile);
        WARestClient restClient = getWaRestClient(Response.serverError().build());
        SAML2MetadataGenerator generator = new SyncopeWASAML2ClientMetadataGenerator(restClient, client);
        EntityDescriptor entityDescriptor = generator.buildEntityDescriptor();
        String metadata = generator.getMetadata(entityDescriptor);
        assertThrows(SyncopeClientException.class, () -> generator.storeMetadata(metadata, null, false));
    }
}
