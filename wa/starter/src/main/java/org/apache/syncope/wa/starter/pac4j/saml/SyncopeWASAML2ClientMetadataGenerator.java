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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.SAML2SPMetadataService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.opensaml.saml.metadata.resolver.impl.AbstractBatchMetadataResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.BaseSAML2MetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.ws.rs.core.Response;

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
    public boolean storeMetadata(final String metadata, final Resource resource, final boolean force) throws Exception {
        SAML2SPMetadataService metadataService = restClient.getSyncopeClient().
            getService(SAML2SPMetadataService.class);
        SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO.Builder().
            metadata(metadata).
            owner(saml2Client.getName()).
            build();
        LOG.debug("Storing metadata {}", metadataTO);
        Response response = metadataService.set(metadataTO);
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            LOG.info("Stored metadata for SAML2 SP {}", saml2Client.getName());
            return true;
        }
        if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
            LOG.info("Stored metadata for SAML2 SP {} already exists", saml2Client.getName());
            return true;
        }
        LOG.error("Unexpected response when storing SAML2 SP metadata: {}\n{}",
            response.getStatus(), response.getHeaders());
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
        sce.getElements().add("Unexpected response when storing SAML2 SP metadata");
        throw sce;
    }
}
