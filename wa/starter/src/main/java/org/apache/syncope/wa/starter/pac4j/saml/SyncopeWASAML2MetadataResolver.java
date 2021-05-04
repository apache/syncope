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

import java.util.Base64;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;

public class SyncopeWASAML2MetadataResolver extends AbstractReloadingMetadataResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASAML2MetadataResolver.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASAML2MetadataResolver(final WARestClient restClient, final SAML2Client saml2Client) {
        this.restClient = restClient;
        this.saml2Client = saml2Client;
    }

    @Override
    protected String getMetadataIdentifier() {
        return saml2Client.getName();
    }

    @Override
    protected byte[] fetchMetadata() throws ResolverException {
        try {
            SAML2SPEntityTO metadataTO = restClient.getSyncopeClient().
                    getService(SAML2SPEntityService.class).get(saml2Client.getName());
            return Base64.getDecoder().decode(metadataTO.getMetadata());
        } catch (final Exception e) {
            final String message = "Unable to fetch SP metadata for " + saml2Client.getName();
            LOG.error(message, e);
            throw new ResolverException(message);
        }
    }
}
