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
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SyncopeWASamlClientKeystoreGenerator extends BaseSAML2KeystoreGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWASamlClientKeystoreGenerator.class);

    private final WARestClient restClient;

    private final SAML2Client saml2Client;

    SyncopeWASamlClientKeystoreGenerator(final WARestClient restClient, final SAML2Client saml2Client) {
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
            String content = Base64.getEncoder().encodeToString(out.toByteArray());
        } finally {
            
        }
    }

    @Override
    public InputStream retrieve() throws Exception {
        return null;
    }
}
