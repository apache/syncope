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
import java.util.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.core.io.ClassPathResource;

public class WASAML2MetadataResolverTest extends BaseWASAML2ClientTest {

    @Test
    public void fetchMetadata() throws Exception {
        SAML2Client client = getSAML2Client();
        String keystoreFile = File.createTempFile("keystore", "jks").getCanonicalPath();
        client.getConfiguration().setKeystoreResourceFilepath(keystoreFile);

        SAML2SPEntityTO metadataTO = new SAML2SPEntityTO.Builder()
                .key("Syncope")
                .metadata(Base64.getEncoder().encodeToString(
                        IOUtils.toByteArray(new ClassPathResource("sp-metadata.xml").getInputStream())))
                .build();

        SAML2SPEntityService saml2SPMetadataService = mock(SAML2SPEntityService.class);
        when(saml2SPMetadataService.get(anyString())).thenReturn(metadataTO);
        doNothing().when(saml2SPMetadataService).set(any(SAML2SPEntityTO.class));

        WARestClient waRestClient = mock(WARestClient.class);
        when(waRestClient.getService(SAML2SPEntityService.class)).thenReturn(saml2SPMetadataService);

        WASAML2MetadataResolver resolver = new WASAML2MetadataResolver(waRestClient, client);
        assertNotNull(resolver.fetchMetadata());
    }
}
