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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.rest.api.service.wa.WASAML2SPService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.AbstractMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.BaseSAML2MetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WASAML2ClientMetadataGenerator extends BaseSAML2MetadataGenerator {

    protected static final Logger LOG = LoggerFactory.getLogger(WASAML2ClientMetadataGenerator.class);

    protected final WARestClient waRestClient;

    protected final SAML2Client saml2Client;

    public WASAML2ClientMetadataGenerator(final WARestClient waRestClient, final SAML2Client saml2Client) {
        this.waRestClient = waRestClient;
        this.saml2Client = saml2Client;
    }

    @Override
    public boolean storeMetadata(final String metadata, final boolean force) {
        return true;
    }

    protected Optional<String> metadataAvailable() {
        try {
            String encodedMetadata = waRestClient.getService(WASAML2SPService.class).
                    getSAML2SPMetadata(saml2Client.getName()).readEntity(String.class);

            LOG.debug("Retrieved metadata {}", encodedMetadata);
            return Optional.of(new String(Base64.getDecoder().decode(encodedMetadata), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("While attempting to read metadata for SP Entity {}", saml2Client.getName(), e);
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code null} if metadata was found in Syncope Core via REST
     */
    @Override
    public EntityDescriptor buildEntityDescriptor() {
        return metadataAvailable().
                map(m -> (EntityDescriptor) null).
                orElseGet(() -> {
                    EntityDescriptor entityDescriptor = super.buildEntityDescriptor();
                    entityDescriptor.setValidUntil(null);
                    if (signMetadata) {
                        signMetadata(entityDescriptor);
                    }
                    return entityDescriptor;
                });
    }

    /**
     * {@inheritDoc}
     *
     * return metadata from Syncope Core via REST, if available; otherwise, generate
     */
    @Override
    public String getMetadata(final EntityDescriptor entityDescriptor) throws Exception {
        String metadata = metadataAvailable().orElse(null);
        if (metadata == null) {
            return super.getMetadata(entityDescriptor);
        }
        return metadata;
    }

    @Override
    protected AbstractMetadataResolver createMetadataResolver() {
        return new WASAML2MetadataResolver(waRestClient, saml2Client);
    }

    @Override
    public MetadataResolver buildMetadataResolver() throws Exception {
        EntityDescriptor entityDescriptor = buildEntityDescriptor();
        if (entityDescriptor != null) {
            String encodedMetadata = Base64.getEncoder().encodeToString(
                    getMetadata(entityDescriptor).getBytes(StandardCharsets.UTF_8));
            LOG.debug("Encoded SP metadata {}", encodedMetadata);

            try {
                waRestClient.getService(WASAML2SPService.class).setSAML2SPMetadata(
                        saml2Client.getName(), IOUtils.toInputStream(encodedMetadata, StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOG.error("While storing SP {} metadata", saml2Client.getName(), e);
            }
        }

        return super.buildMetadataResolver();
    }
}
