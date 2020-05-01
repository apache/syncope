/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.wa.pac4j;

import org.apache.syncope.wa.WARestClient;
import org.opensaml.saml.metadata.resolver.impl.AbstractBatchMetadataResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.BaseSAML2MetadataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

public class SyncopeWASamlClientMetadataGenerator extends BaseSAML2MetadataGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASamlClientMetadataGenerator.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASamlClientMetadataGenerator(final WARestClient restClient, final SAML2Client saml2Client) {
        this.restClient = restClient;
        this.saml2Client = saml2Client;
    }

    @Override
    protected AbstractBatchMetadataResolver createMetadataResolver(final Resource metadataResource) {
        return new SyncopeWASamlMetadataResolver(restClient, saml2Client);
    }

    @Override
    public boolean storeMetadata(final String metadata, final Resource resource, final boolean force) throws Exception {
        return false;
    }
}
