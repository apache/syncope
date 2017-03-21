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

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.common.lib.types.SAML2SPEntitlement;
import org.apache.syncope.core.logic.saml2.SAML2ReaderWriter;
import org.apache.syncope.core.logic.saml2.SAML2Signer;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2SPLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2SPLoader.class);

    private static final String SAML2SP_LOGIC_PROPERTIES = "saml2sp-logic.properties";

    private static <T> T assertNotNull(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument '" + name + "' may not be null.");
        }
        return argument;
    }

    static {
        OpenSAMLUtil.initSamlEngine(false);
    }

    @Autowired
    private SAML2ReaderWriter saml2rw;

    @Autowired
    private SAML2Signer signer;

    private boolean inited;

    private KeyStore keystore;

    private String keyPass;

    private Credential credential;

    @Override
    public Integer getPriority() {
        return 1000;
    }

    @Override
    public void load() {
        EntitlementsHolder.getInstance().init(SAML2SPEntitlement.values());

        String confDirectory = null;

        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/" + SAML2SP_LOGIC_PROPERTIES)) {
            props.load(is);
            confDirectory = props.getProperty("conf.directory");

            File confDir = new File(confDirectory);
            if (confDir.exists() && confDir.canRead() && confDir.isDirectory()) {
                File confDirProps = FileUtils.getFile(confDir, SAML2SP_LOGIC_PROPERTIES);
                if (confDirProps.exists() && confDirProps.canRead() && confDirProps.isFile()) {
                    props.clear();
                    props.load(FileUtils.openInputStream(confDirProps));
                    confDirectory = props.getProperty("conf.directory");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read " + SAML2SP_LOGIC_PROPERTIES, e);
        }

        assertNotNull(confDirectory, "<conf.directory>");

        String name = props.getProperty("keystore.name");
        assertNotNull(name, "<keystore.name>");
        String type = props.getProperty("keystore.type");
        assertNotNull(type, "<keystore.type>");
        String storePass = props.getProperty("keystore.storepass");
        assertNotNull(storePass, "<keystore.storepass>");
        keyPass = props.getProperty("keystore.keypass");
        assertNotNull(keyPass, "<keystore.keypass>");
        String certAlias = props.getProperty("sp.cert.alias");
        assertNotNull(certAlias, "<sp.cert.alias>");

        LOG.debug("Attempting to load the provided keystore...");
        try {
            ResourceWithFallbackLoader loader = new ResourceWithFallbackLoader();
            loader.setResourceLoader(ApplicationContextProvider.getApplicationContext());
            loader.setPrimary(StringUtils.appendIfMissing("file:" + confDirectory, "/") + name);
            loader.setFallback("classpath:" + name);

            keystore = KeyStore.getInstance(type);
            try (InputStream inputStream = loader.getResource().getInputStream()) {
                keystore.load(inputStream, storePass.toCharArray());
                LOG.debug("Keystore loaded");
            }

            Map<String, String> passwordMap = new HashMap<>();
            passwordMap.put(certAlias, keyPass);
            KeyStoreCredentialResolver resolver = new KeyStoreCredentialResolver(keystore, passwordMap);

            this.credential = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(certAlias)));
            LOG.debug("SAML 2.0 Service Provider certificate loaded");

            saml2rw.init();
            signer.init();

            inited = true;
        } catch (Exception e) {
            LOG.error("Could not initialize the SAML 2.0 Service Provider certificate", e);
            inited = false;
        }
    }

    public boolean isInited() {
        return inited;
    }

    public KeyStore getKeyStore() {
        return keystore;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public Credential getCredential() {
        return credential;
    }

}
