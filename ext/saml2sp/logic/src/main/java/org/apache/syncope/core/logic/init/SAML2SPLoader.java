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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.PropertyUtils;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.SAML2SPEntitlement;
import org.apache.syncope.common.lib.types.SAML2SPImplementationType;
import org.apache.syncope.core.logic.saml2.SAML2IdPCache;
import org.apache.syncope.core.logic.saml2.SAML2ReaderWriter;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2SPLoader implements SyncopeCoreLoader {

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
    private SAML2IdPCache cache;

    @Autowired
    private SAML2IdPDAO idpDAO;

    private boolean inited;

    private KeyStore keystore;

    private String keyPass;

    private Credential credential;

    private String signatureAlgorithm;

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public void load() {
        EntitlementsHolder.getInstance().addAll(SAML2SPEntitlement.values());
        ImplementationTypesHolder.getInstance().putAll(SAML2SPImplementationType.values());

        Properties props = PropertyUtils.read(getClass(), SAML2SP_LOGIC_PROPERTIES, "conf.directory");
        String confDirectory = props.getProperty("conf.directory");

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
        signatureAlgorithm = props.getProperty("signature.algorithm");

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

            inited = true;
        } catch (Exception e) {
            LOG.error("Could not initialize the SAML 2.0 Service Provider certificate", e);
            inited = false;
        }
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        AuthContextUtils.callAsAdmin(domain, () -> {
            idpDAO.findAll().forEach(idp -> {
                try {
                    cache.put(idp);
                } catch (Exception e) {
                    LOG.error("Could not cache the SAML 2.0 IdP with key {}", idp.getEntityID(), e);
                }
            });
            return null;
        });
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

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
