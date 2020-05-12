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
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.wa.SAML2SPKeystoreService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SyncopeWASAML2ClientKeystoreGenerator extends BaseSAML2KeystoreGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASAML2ClientKeystoreGenerator.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASAML2ClientKeystoreGenerator(final WARestClient restClient, final SAML2Client saml2Client) {
        super(saml2Client.getConfiguration());
        this.restClient = restClient;
        this.saml2Client = saml2Client;
    }

    @Override
    public boolean shouldGenerate() {
        return true;
    }

    @Override
    protected void store(final KeyStore ks, final X509Certificate certificate,
                         final PrivateKey privateKey) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            char[] password = saml2Configuration.getKeystorePassword().toCharArray();
            ks.store(out, password);
            out.flush();
            String encodedKeystore = Base64.getEncoder().encodeToString(out.toByteArray());
            LOG.debug("Encoded keystore {}", encodedKeystore);

            SAML2SPKeystoreService keystoreService = restClient.getSyncopeClient().
                getService(SAML2SPKeystoreService.class);

            SAML2SPKeystoreTO keystoreTO = new SAML2SPKeystoreTO.Builder().
                keystore(encodedKeystore).
                owner(saml2Client.getName()).
                build();

            LOG.debug("Storing keystore {}", keystoreTO);
            Response response = keystoreService.set(keystoreTO);
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                LOG.info("Stored keystore for SAML2 SP {}", saml2Client.getName());
            } else if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                LOG.info("Stored keystore for SAML2 SP {} already exists", saml2Client.getName());
            } else {
                LOG.error("Unexpected response when storing SAML2 SP keystore: {}\n{}",
                    response.getStatus(), response.getHeaders());
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                sce.getElements().add("Unexpected response when storing SAML2 SP keystore");
                throw sce;
            }
        }
    }

    @Override
    public InputStream retrieve() throws Exception {
        try {
            SAML2SPKeystoreService keystoreService = restClient.getSyncopeClient().
                getService(SAML2SPKeystoreService.class);
            SAML2SPKeystoreTO keystoreTO = keystoreService.getByOwner(saml2Client.getName());
            LOG.debug("Retrieved keystore {}", keystoreTO);
            byte[] decode = Base64.getDecoder().decode(keystoreTO.getKeystore());
            return new ByteArrayInputStream(decode);
        } catch (final Exception e) {
            final String message = "Unable to fetch SAML2 SP keystore for " + saml2Client.getName();
            LOG.error(message, e);
            throw new Exception(message);
        }
    }
}
