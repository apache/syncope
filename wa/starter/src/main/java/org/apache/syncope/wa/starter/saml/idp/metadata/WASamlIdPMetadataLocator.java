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

import com.github.benmanes.caffeine.cache.Cache;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.support.saml.idp.metadata.locator.AbstractSamlIdPMetadataLocator;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class WASamlIdPMetadataLocator extends AbstractSamlIdPMetadataLocator {

    protected static final Logger LOG = LoggerFactory.getLogger(WASamlIdPMetadataLocator.class);

    protected final WARestClient waRestClient;

    public WASamlIdPMetadataLocator(
            final CipherExecutor<String, String> metadataCipherExecutor,
            final Cache<String, SamlIdPMetadataDocument> metadataCache,
            final ApplicationContext applicationContext,
            final WARestClient waRestClient) {

        super(metadataCipherExecutor, metadataCache, applicationContext);
        this.waRestClient = waRestClient;
    }

    protected SAML2IdPEntityTO fetchFromCore(final Optional<SamlRegisteredService> registeredService) {
        SAML2IdPEntityService idpEntityService = waRestClient.getService(SAML2IdPEntityService.class);

        SAML2IdPEntityTO result = null;
        try {
            result = idpEntityService.get(registeredService.
                    map(SamlRegisteredService::getName).
                    orElse(SAML2IdPEntityService.DEFAULT_OWNER));
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound && registeredService.isPresent()) {
                result = idpEntityService.get(SAML2IdPEntityService.DEFAULT_OWNER);
            } else {
                throw e;
            }
        }

        return result;
    }

    @Override
    public String getAppliesToFor(final Optional<SamlRegisteredService> registeredService) {
        return registeredService.
                map(SamlRegisteredService::getName).
                orElse(SAML2IdPEntityService.DEFAULT_OWNER);
    }

    @Override
    public SamlIdPMetadataDocument fetchInternal(final Optional<SamlRegisteredService> registeredService) {
        try {
            LOG.info("Locating SAML2 IdP metadata document");

            SAML2IdPEntityTO entityTO = fetchFromCore(registeredService);
            if (entityTO != null) {
                SamlIdPMetadataDocument document = new SamlIdPMetadataDocument();
                document.setAppliesTo(entityTO.getKey());
                document.setMetadata(new String(Base64.getDecoder().decode(
                        entityTO.getMetadata()), StandardCharsets.UTF_8));
                if (entityTO.getSigningCertificate() != null) {
                    document.setSigningCertificate(new String(Base64.getDecoder().decode(
                            entityTO.getSigningCertificate()), StandardCharsets.UTF_8));
                }
                if (entityTO.getSigningKey() != null) {
                    document.setSigningKey(new String(Base64.getDecoder().decode(
                            entityTO.getSigningKey().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                }
                if (entityTO.getEncryptionCertificate() != null) {
                    document.setEncryptionCertificate(new String(Base64.getDecoder().decode(
                            entityTO.getEncryptionCertificate()), StandardCharsets.UTF_8));
                }
                if (entityTO.getEncryptionKey() != null) {
                    document.setEncryptionKey(new String(Base64.getDecoder().decode(
                            entityTO.getEncryptionKey().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                }

                if (document.isValid()) {
                    LOG.debug("Found SAML2 IdP metadata document: {}", document.getId());
                    return document;
                }
            }

            LOG.warn("Not a valid SAML2 IdP metadata document");
            return null;
        } catch (Exception e) {
            if (e instanceof final SyncopeClientException syncopeClientException
                    && syncopeClientException.getType() == ClientExceptionType.NotFound) {
                LOG.info(e.getMessage());
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.error("While fetching SAML2 IdP metadata", e);
                } else {
                    LOG.error("While fetching SAML2 IdP metadata: {}", e.getMessage());
                }
            }
        }

        return null;
    }
}
