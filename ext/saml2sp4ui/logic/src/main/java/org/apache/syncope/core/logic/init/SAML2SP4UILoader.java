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
package org.apache.syncope.core.logic.init;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.SAML2SP4UIEntitlement;
import org.apache.syncope.common.lib.types.SAML2SP4UIImplementationType;
import org.apache.syncope.core.logic.saml2.NoOpLogoutHandler;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.keystore.BaseSAML2KeystoreGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class SAML2SP4UILoader implements SyncopeCoreLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2SP4UILoader.class);

    private static final String SAML2SP4UI_LOGIC_PROPERTIES = "saml2sp4ui-logic.properties";

    @Autowired
    private ResourcePatternResolver resourceResolver;

    private Properties props;

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public void load() {
        EntitlementsHolder.getInstance().addAll(SAML2SP4UIEntitlement.values());
        ImplementationTypesHolder.getInstance().putAll(SAML2SP4UIImplementationType.values());

        props = PropertyUtils.read(getClass(), SAML2SP4UI_LOGIC_PROPERTIES, "conf.directory");
    }

    public SAML2Configuration newSAML2Configuration() {
        SAML2Configuration cfg = new SAML2Configuration(
                resourceResolver.getResource(props.getProperty("saml2.sp4ui.keystore")),
                props.getProperty("saml2.sp4ui.keystore.storepass"),
                props.getProperty("saml2.sp4ui.keystore.keypass"),
                null);

        cfg.setKeystoreType(props.getProperty("saml2.sp4ui.keystore.type"));
        if (cfg.getKeystoreResource() instanceof FileUrlResource) {
            cfg.setKeystoreGenerator(new BaseSAML2KeystoreGenerator(cfg) {

                @Override
                protected void store(
                        final KeyStore ks,
                        final X509Certificate certificate,
                        final PrivateKey privateKey) throws Exception {

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

        try {
            cfg.setAcceptedSkew(Integer.valueOf(props.getProperty("saml2.sp4ui.skew")));
        } catch (NumberFormatException e) {
            LOG.error("Invalid value provided for 'saml2.sp4ui.skew': {}", props.getProperty("saml2.sp4ui.skew"), e);
        }

        cfg.setLogoutHandler(new NoOpLogoutHandler());

        return cfg;
    }
}
