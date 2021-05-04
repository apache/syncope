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

import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactoryCustomizer;

import org.apache.syncope.wa.bootstrap.WARestClient;
import org.pac4j.core.client.Client;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeWASAML2ClientCustomizer implements DelegatedClientFactoryCustomizer<Client> {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASAML2ClientCustomizer.class);

    private final WARestClient restClient;

    public SyncopeWASAML2ClientCustomizer(final WARestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void customize(final Client client) {
        if (client instanceof SAML2Client) {
            LOG.debug("Customizing SAML2 client {}", client.getName());
            SAML2Client saml2Client = (SAML2Client) client;
            SAML2Configuration configuration = saml2Client.getConfiguration();
            configuration.setKeystoreGenerator(new SyncopeWASAML2ClientKeystoreGenerator(restClient, saml2Client));
            configuration.setMetadataGenerator(new SyncopeWASAML2ClientMetadataGenerator(restClient, saml2Client));
        }
    }
}
