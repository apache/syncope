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
package org.apache.syncope.core.logic;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.apache.syncope.common.lib.to.EntityTO;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;

abstract class AbstractSAML2SP4UILogic extends AbstractTransactionalLogic<EntityTO> {

    protected final SAML2SP4UIProperties props;

    protected final ResourcePatternResolver resourceResolver;

    protected AbstractSAML2SP4UILogic(
            final SAML2SP4UIProperties props,
            final ResourcePatternResolver resourceResolver) {

        this.props = props;
        this.resourceResolver = resourceResolver;
    }

    protected SAML2Configuration newSAML2Configuration() {
        SAML2Configuration cfg = new SAML2Configuration(
                resourceResolver.getResource(props.getKeystore()),
                props.getKeystoreAlias(),
                props.getKeystoreType(),
                props.getKeystoreStorepass(),
                props.getKeystoreKeypass(),
                null);

        if (cfg.getKeystoreResource() instanceof FileUrlResource) {
            cfg.setKeystoreGenerator(new BaseSAML2KeystoreGenerator(cfg) {

                @Override
                protected void store(
                        final KeyStore ks,
                        final X509Certificate certificate,
                        final PrivateKey privateKey) {

                    // nothing to do
                }

                @Override
                public InputStream retrieve() throws Exception {
                    return cfg.getKeystoreResource().getInputStream();
                }
            });
        }

        cfg.setWantsAssertionsSigned(true);
        cfg.setAuthnRequestSigned(true);
        cfg.setSpLogoutRequestSigned(true);
        cfg.setMaximumAuthenticationLifetime(props.getMaximumAuthenticationLifetime());
        cfg.setAcceptedSkew(props.getAcceptedSkew());

        return cfg;
    }
}
