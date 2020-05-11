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

import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.SAML2IdPMetadataService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.saml.idp.metadata.locator.AbstractSamlIdPMetadataLocator;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class RestfulSamlIdPMetadataLocator extends AbstractSamlIdPMetadataLocator {

    private static final Logger LOG = LoggerFactory.getLogger(RestfulSamlIdPMetadataLocator.class);

    private final WARestClient restClient;

    public RestfulSamlIdPMetadataLocator(
            final CipherExecutor<String, String> metadataCipherExecutor,
            final WARestClient restClient) {

        super(metadataCipherExecutor);
        this.restClient = restClient;
    }

    private static String getAppliesToFor(final Optional<SamlRegisteredService> result) {
        if (result.isPresent()) {
            SamlRegisteredService registeredService = result.get();
            return registeredService.getName() + '-' + registeredService.getId();
        }
        return RestfulSamlIdPMetadataGenerator.DEFAULT_APPLIES_FOR;
    }

    @Override
    public SamlIdPMetadataDocument fetchInternal(final Optional<SamlRegisteredService> registeredService) {
        try {
            LOG.info("Locating SAML2 IdP metadata document");
            SAML2IdPMetadataTO saml2IdPMetadataTO = getSyncopeClient().getService(SAML2IdPMetadataService.class).
                getByOwner(getAppliesToFor(registeredService));

            if (saml2IdPMetadataTO == null) {
                LOG.warn("No SAML2 IdP metadata document obtained from core");
            } else {
                SamlIdPMetadataDocument document = new SamlIdPMetadataDocument();
                document.setMetadata(saml2IdPMetadataTO.getMetadata());
                document.setEncryptionCertificate(saml2IdPMetadataTO.getEncryptionCertificate());
                document.setEncryptionKey(saml2IdPMetadataTO.getEncryptionKey());
                document.setSigningKey(saml2IdPMetadataTO.getSigningKey());
                document.setSigningCertificate(saml2IdPMetadataTO.getSigningCertificate());
                document.setAppliesTo(saml2IdPMetadataTO.getAppliesTo());
                if (document.isValid()) {
                    LOG.debug("Found SAML2 IdP metadata document: {}", document.getId());
                    return document;
                }
                LOG.warn("Not a valid SAML2 IdP metadata document");
            }

            return null;
        } catch (SyncopeClientException ex) {
            if (ex.getType() == ClientExceptionType.NotFound) {
                LOG.debug("No SAML2 IdP metadata document is available");
            }
        }
        return null;
    }

    private SyncopeClient getSyncopeClient() {
        if (!WARestClient.isReady()) {
            LOG.info("Syncope client is not yet ready");
            throw new RuntimeException("Syncope core is not yet ready to access requests");
        }
        return restClient.getSyncopeClient();
    }
}
