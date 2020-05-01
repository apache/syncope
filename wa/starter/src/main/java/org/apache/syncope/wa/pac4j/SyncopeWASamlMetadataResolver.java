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

import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.apache.syncope.wa.WARestClient;
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver;
import org.pac4j.saml.client.SAML2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeWASamlMetadataResolver extends AbstractReloadingMetadataResolver {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASamlMetadataResolver.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASamlMetadataResolver(final WARestClient restClient, final SAML2Client saml2Client) {
        this.restClient = restClient;
        this.saml2Client = saml2Client;
    }

    @Override
    protected String getMetadataIdentifier() {
        return saml2Client.getName();
    }

    @Override
    protected byte[] fetchMetadata() throws ResolverException {
        return new byte[0];
    }
}
