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
package org.apache.syncope.wa.starter.saml.idp.metadata;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.rest.api.service.wa.SAML2IdPMetadataService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.saml.idp.metadata.generator.BaseSamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class RestfulSamlIdPMetadataGenerator extends BaseSamlIdPMetadataGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(RestfulSamlIdPMetadataGenerator.class);

    public static final String DEFAULT_APPLIES_FOR = "Syncope";

    private final WARestClient restClient;

    public RestfulSamlIdPMetadataGenerator(
            final SamlIdPMetadataGeneratorConfigurationContext samlIdPMetadataGeneratorConfigurationContext,
            final WARestClient restClient) {

        super(samlIdPMetadataGeneratorConfigurationContext);
        this.restClient = restClient;
    }

    @Override
    protected SamlIdPMetadataDocument finalizeMetadataDocument(
            final SamlIdPMetadataDocument doc,
            final Optional<SamlRegisteredService> registeredService) {

        LOG.info("Generating new SAML2 IdP metadata document");
        doc.setAppliesTo(DEFAULT_APPLIES_FOR);
        SAML2IdPMetadataTO metadataTO = new SAML2IdPMetadataTO.Builder().
                metadata(doc.getMetadata()).
                encryptionKey(doc.getEncryptionKey()).
                encryptionCertificate(doc.getEncryptionCertificate()).
                signingCertificate(doc.getSigningCertificate()).
                signingKey(doc.getSigningKey()).
                appliesTo(doc.getAppliesTo()).
                build();

        SyncopeClient client = getSyncopeClient();
        Response response = null;
        try {
            response = client.getService(SAML2IdPMetadataService.class).set(metadataTO);
        } catch (Exception ex) {
            LOG.warn("While generating SAML2 IdP metadata document", ex);
        }

        return response != null && HttpStatus.valueOf(response.getStatus()).is2xxSuccessful() ? doc : null;
    }

    @Override
    public Pair<String, String> buildSelfSignedEncryptionCert(final Optional<SamlRegisteredService> registeredService) {
        return generateCertificateAndKey();
    }

    @Override
    public Pair<String, String> buildSelfSignedSigningCert(final Optional<SamlRegisteredService> registeredService) {
        return generateCertificateAndKey();
    }

    private SyncopeClient getSyncopeClient() {
        if (!WARestClient.isReady()) {
            LOG.info("Syncope client is not yet ready");
            throw new RuntimeException("Syncope core is not yet ready to access requests");
        }
        return restClient.getSyncopeClient();
    }

}
