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
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.opensaml.saml.metadata.resolver.impl.AbstractBatchMetadataResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.BaseSAML2MetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.opensaml.saml.metadata.resolver.MetadataResolver;

public class SyncopeWASAML2ClientMetadataGenerator extends BaseSAML2MetadataGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASAML2ClientMetadataGenerator.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASAML2ClientMetadataGenerator(final WARestClient restClient, final SAML2Client saml2Client) {
        this.restClient = restClient;
        this.saml2Client = saml2Client;
    }

    @Override
    protected AbstractBatchMetadataResolver createMetadataResolver(final Resource metadataResource) {
        return new SyncopeWASAML2MetadataResolver(restClient, saml2Client);
    }

    @Override
    public MetadataResolver buildMetadataResolver(final Resource metadataResource) throws Exception {
        String encodedMetadata = Base64.getEncoder().encodeToString(
                getMetadata(buildEntityDescriptor()).getBytes(StandardCharsets.UTF_8));

        SAML2SPEntityTO entityTO;
        try {
            entityTO = restClient.getSyncopeClient().getService(SAML2SPEntityService.class).
                    get(saml2Client.getName());
            entityTO.setMetadata(encodedMetadata);
        } catch (Exception e) {
            LOG.debug("SP Entity {} not found, creating new", saml2Client.getName(), e);

            entityTO = new SAML2SPEntityTO.Builder().
                    key(saml2Client.getName()).
                    metadata(encodedMetadata).
                    build();
        }

        LOG.debug("Storing SP Entity {}", entityTO);
        restClient.getSyncopeClient().getService(SAML2SPEntityService.class).set(entityTO);

        return super.buildMetadataResolver(metadataResource);
    }

    @Override
    public boolean storeMetadata(final String metadata, final Resource resource, final boolean force) throws Exception {
        return true;
    }
}
