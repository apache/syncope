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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.saml.idp.metadata.generator.BaseSamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WASamlIdPMetadataGenerator extends BaseSamlIdPMetadataGenerator {

    protected static final Logger LOG = LoggerFactory.getLogger(WASamlIdPMetadataGenerator.class);

    protected final WARestClient waRestClient;

    public WASamlIdPMetadataGenerator(
            final SamlIdPMetadataGeneratorConfigurationContext samlIdPMetadataGeneratorConfigurationContext,
            final WARestClient waRestClient) {

        super(samlIdPMetadataGeneratorConfigurationContext);
        this.waRestClient = waRestClient;
    }

    @Override
    public String getAppliesToFor(final Optional<SamlRegisteredService> registeredService) {
        return registeredService.
                map(SamlRegisteredService::getName).
                orElse(SAML2IdPEntityService.DEFAULT_OWNER);
    }

    @Override
    protected SamlIdPMetadataDocument finalizeMetadataDocument(
            final SamlIdPMetadataDocument doc,
            final Optional<SamlRegisteredService> registeredService) {

        doc.setAppliesTo(getAppliesToFor(registeredService));

        LOG.info("Setting new SAML2 IdP metadata document for {}", doc.getAppliesTo());

        SAML2IdPEntityTO entityTO = new SAML2IdPEntityTO.Builder().
                key(doc.getAppliesTo()).
                metadata(Base64.getEncoder().encodeToString(doc.getMetadata().getBytes(StandardCharsets.UTF_8))).
                build();
        if (doc.getSigningKey() != null) {
            entityTO.setSigningKey(Base64.getEncoder().encodeToString(
                    doc.getSigningKey().getBytes(StandardCharsets.UTF_8)));
        }
        if (doc.getSigningCertificate() != null) {
            entityTO.setSigningCertificate(Base64.getEncoder().encodeToString(
                    doc.getSigningCertificate().getBytes(StandardCharsets.UTF_8)));
        }
        if (doc.getEncryptionKey() != null) {
            entityTO.setEncryptionKey(Base64.getEncoder().encodeToString(
                    doc.getEncryptionKey().getBytes(StandardCharsets.UTF_8)));
        }
        if (doc.getEncryptionCertificate() != null) {
            entityTO.setEncryptionCertificate(Base64.getEncoder().encodeToString(
                    doc.getEncryptionCertificate().getBytes(StandardCharsets.UTF_8)));
        }

        waRestClient.getService(SAML2IdPEntityService.class).set(entityTO);

        return doc;
    }

    @Override
    public Pair<String, String> buildSelfSignedEncryptionCert(final Optional<SamlRegisteredService> registeredService)
            throws Exception {

        return generateCertificateAndKey();
    }

    @Override
    public Pair<String, String> buildSelfSignedSigningCert(final Optional<SamlRegisteredService> registeredService)
            throws Exception {

        return generateCertificateAndKey();
    }
}
