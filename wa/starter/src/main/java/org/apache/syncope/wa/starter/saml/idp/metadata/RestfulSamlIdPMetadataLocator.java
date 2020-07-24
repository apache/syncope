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
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.saml.idp.metadata.locator.AbstractSamlIdPMetadataLocator;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import org.apache.syncope.common.rest.api.service.wa.WASAML2IdPMetadataService;

public class RestfulSamlIdPMetadataLocator extends AbstractSamlIdPMetadataLocator {

    private static final Logger LOG = LoggerFactory.getLogger(RestfulSamlIdPMetadataLocator.class);

    private final WARestClient restClient;

    public RestfulSamlIdPMetadataLocator(
            final CipherExecutor<String, String> metadataCipherExecutor,
            final WARestClient restClient) {

        super(metadataCipherExecutor);
        this.restClient = restClient;
    }

    @Override
    public SamlIdPMetadataDocument fetchInternal(final Optional<SamlRegisteredService> registeredService) {
        try {
            LOG.info("Locating SAML2 IdP metadata document");

            SAML2IdPMetadataTO saml2IdPMetadataTO = fetchFromCore(registeredService);
            if (saml2IdPMetadataTO != null) {
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
            }

            LOG.warn("Not a valid SAML2 IdP metadata document");
            return null;
        } catch (SyncopeClientException e) {
            LOG.error("While fetching SAML2 IdP metadata", e);
        }

        return null;
    }

    private SAML2IdPMetadataTO fetchFromCore(final Optional<SamlRegisteredService> registeredService) {
        SAML2IdPMetadataTO result = null;

        String appliesToFor = registeredService.map(SamlRegisteredService::getName).
                orElse(WASAML2IdPMetadataService.DEFAULT_OWNER);
        try {
            result = getSyncopeClient().getService(WASAML2IdPMetadataService.class).getByOwner(appliesToFor);
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound && registeredService.isPresent()) {
                result = getSyncopeClient().getService(WASAML2IdPMetadataService.class).
                        getByOwner(WASAML2IdPMetadataService.DEFAULT_OWNER);
            } else {
                throw e;
            }
        }

        return result;
    }

    private SyncopeClient getSyncopeClient() {
        if (!WARestClient.isReady()) {
            LOG.info("Syncope client is not yet ready");
            throw new RuntimeException("Syncope core is not yet ready to access requests");
        }
        return restClient.getSyncopeClient();
    }
}
