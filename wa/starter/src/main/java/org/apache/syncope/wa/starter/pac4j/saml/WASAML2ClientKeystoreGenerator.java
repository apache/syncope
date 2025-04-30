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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WASAML2ClientKeystoreGenerator extends BaseSAML2KeystoreGenerator {

    protected static final Logger LOG = LoggerFactory.getLogger(WASAML2ClientKeystoreGenerator.class);

    protected final WARestClient waRestClient;

    protected final SAML2Client saml2Client;

    public WASAML2ClientKeystoreGenerator(final WARestClient waRestClient, final SAML2Client saml2Client) {
        super(saml2Client.getConfiguration());
        this.waRestClient = waRestClient;
        this.saml2Client = saml2Client;
    }

    @Override
    public boolean shouldGenerate() {
        try {
            SAML2SPEntityTO spEntity = waRestClient.getService(SAML2SPEntityService.class).get(saml2Client.getName());
            return spEntity.getKeystore() == null;
        } catch (Exception e) {
            LOG.error("While attempting to read if keystore is available for SP Entity {}", saml2Client.getName(), e);
            return true;
        }
    }

    @Override
    protected void store(final KeyStore ks, final X509Certificate certificate, final PrivateKey privateKey)
            throws Exception {

        String encodedKeystore;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            char[] password = saml2Configuration.getKeystorePassword().toCharArray();
            ks.store(out, password);
            out.flush();
            encodedKeystore = Base64.getEncoder().encodeToString(out.toByteArray());
            LOG.debug("Encoded keystore {}", encodedKeystore);
        }

        SAML2SPEntityTO entityTO;
        try {
            entityTO = waRestClient.getService(SAML2SPEntityService.class).get(saml2Client.getName());
            entityTO.setKeystore(encodedKeystore);
        } catch (Exception e) {
            LOG.debug("SP Entity {} keystore not found, creating new", saml2Client.getName(), e);

            entityTO = new SAML2SPEntityTO.Builder().key(saml2Client.getName()).keystore(encodedKeystore).build();
        }

        LOG.debug("Storing SP Entity {}", entityTO);
        waRestClient.getService(SAML2SPEntityService.class).set(entityTO);
    }

    @Override
    public InputStream retrieve() throws Exception {
        try {
            SAML2SPEntityTO spEntity = waRestClient.getService(SAML2SPEntityService.class).get(saml2Client.getName());

            LOG.debug("Retrieved keystore {}", spEntity.getKeystore());
            return new ByteArrayInputStream(Base64.getDecoder().decode(spEntity.getKeystore()));
        } catch (Exception e) {
            String message = "Unable to fetch SAML2 SP keystore for " + saml2Client.getName();
            LOG.error(message, e);
            throw new Exception(message, e);
        }
    }
}
