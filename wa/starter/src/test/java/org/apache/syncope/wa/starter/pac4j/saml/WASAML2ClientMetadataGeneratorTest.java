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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.SAML2MetadataGenerator;
import org.springframework.core.io.ClassPathResource;

public class WASAML2ClientMetadataGeneratorTest extends BaseWASAML2ClientTest {

    private static WARestClient getWaRestClient() throws IOException {
        SAML2SPEntityTO metadataTO = new SAML2SPEntityTO.Builder()
                .key("Syncope")
                .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(),
                        StandardCharsets.UTF_8))
                .build();

        SAML2SPEntityService saml2SPMetadataService = mock(SAML2SPEntityService.class);
        when(saml2SPMetadataService.get(anyString())).thenReturn(metadataTO);
        doNothing().when(saml2SPMetadataService).set(any(SAML2SPEntityTO.class));

        WARestClient waRestClient = mock(WARestClient.class);
        when(waRestClient.getService(SAML2SPEntityService.class)).thenReturn(saml2SPMetadataService);
        return waRestClient;
    }

    @Test
    public void storeMetadata() throws Exception {
        SAML2Client client = getSAML2Client();
        String keystoreFile = File.createTempFile("keystore", "jks").getCanonicalPath();
        client.getConfiguration().setKeystoreResourceFilepath(keystoreFile);

        SAML2MetadataGenerator generator = new WASAML2ClientMetadataGenerator(getWaRestClient(), client);
        EntityDescriptor entityDescriptor = generator.buildEntityDescriptor();
        String metadata = generator.getMetadata(entityDescriptor);
        assertNotNull(generator.storeMetadata(metadata, false));
    }
}
