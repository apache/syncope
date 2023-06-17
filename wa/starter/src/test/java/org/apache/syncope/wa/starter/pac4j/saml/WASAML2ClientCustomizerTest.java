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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.core.io.ClassPathResource;

public class WASAML2ClientCustomizerTest extends BaseWASAML2ClientTest {

    @Test
    public void customize() throws Exception {
        SAML2SPEntityTO entityTO = new SAML2SPEntityTO.Builder()
                .key("CAS")
                .keystore(getKeystoreAsString())
                .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(),
                        StandardCharsets.UTF_8))
                .build();
        SAML2SPEntityService service = mock(SAML2SPEntityService.class);
        when(service.get(anyString())).thenReturn(entityTO);
        doNothing().when(service).set(any(SAML2SPEntityTO.class));

        WARestClient waRestClient = mock(WARestClient.class);
        when(waRestClient.getService(SAML2SPEntityService.class)).thenReturn(service);

        WASAML2ClientCustomizer customizer = new WASAML2ClientCustomizer(waRestClient);
        SAML2Client client = getSAML2Client();
        customizer.customize(client);
        client.init();
        assertTrue(client.getConfiguration().getKeystoreGenerator() instanceof WASAML2ClientKeystoreGenerator);
        assertTrue(client.getConfiguration().toMetadataGenerator() instanceof WASAML2ClientMetadataGenerator);
    }
}
