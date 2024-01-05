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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.GoogleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.JDBCAuthModuleConf;
import org.apache.syncope.common.lib.auth.JaasAuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.common.lib.auth.SimpleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AuthModuleTest extends AbstractTest {

    private static boolean isSpecificConf(final AuthModuleConf conf, final Class<? extends AuthModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Autowired
    private AuthModuleDAO authModuleDAO;

    @Test
    public void findAll() {
        List<? extends AuthModule> modules = authModuleDAO.findAll();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        assertTrue(modules.size() >= 10);
    }

    @Test
    public void find() {
        AuthModule authModule = authModuleDAO.findById("DefaultLDAPAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof LDAPAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultSimpleMfaAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof SimpleMfaAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultJDBCAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof JDBCAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultGoogleMfaAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof GoogleMfaAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultDuoMfaAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof DuoMfaAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultOIDCAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof OIDCAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultSAML2IdPAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof SAML2IdPAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultJaasAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof JaasAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultStaticAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof StaticAuthModuleConf);

        authModule = authModuleDAO.findById("DefaultSyncopeAuthModule").orElseThrow();
        assertTrue(authModule.getConf() instanceof SyncopeAuthModuleConf);
    }

    @Test
    public void findByType() {
        List<? extends AuthModule> authModules = authModuleDAO.findAll();
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), LDAPAuthModuleConf.class)
                && authModule.getKey().equals("DefaultLDAPAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JDBCAuthModuleConf.class)
                && authModule.getKey().equals("DefaultJDBCAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), GoogleMfaAuthModuleConf.class)
                && authModule.getKey().equals("DefaultGoogleMfaAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), DuoMfaAuthModuleConf.class)
                && authModule.getKey().equals("DefaultDuoMfaAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), OIDCAuthModuleConf.class)
                && authModule.getKey().equals("DefaultOIDCAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SAML2IdPAuthModuleConf.class)
                && authModule.getKey().equals("DefaultSAML2IdPAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JaasAuthModuleConf.class)
                && authModule.getKey().equals("DefaultJaasAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), StaticAuthModuleConf.class)
                && authModule.getKey().equals("DefaultStaticAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SyncopeAuthModuleConf.class)
                && authModule.getKey().equals("DefaultSyncopeAuthModule")));
    }

    private void saveAuthModule(final String key, final AuthModuleConf conf) {
        AuthModule module = entityFactory.newEntity(AuthModule.class);
        module.setKey(key);
        module.setDescription("An authentication module");
        module.setState(AuthModuleState.ACTIVE);
        module.setConf(conf);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        module.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        module.getItems().add(fullnameMapping);

        module = authModuleDAO.save(module);
        entityManager.flush();

        assertNotNull(module);
        assertNotNull(module.getKey());
        assertEquals(module, authModuleDAO.findById(module.getKey()).orElseThrow());
        assertEquals(2, module.getItems().size());
    }

    @Test
    public void saveWithStaticModule() {
        StaticAuthModuleConf conf = new StaticAuthModuleConf();
        conf.getUsers().put("user1", UUID.randomUUID().toString());
        conf.getUsers().put("user2", "user2Password123");

        saveAuthModule("StaticAuthModuleTest", conf);
    }

    @Test
    public void saveWithJaasModule() {
        JaasAuthModuleConf conf = new JaasAuthModuleConf();
        conf.setKerberosKdcSystemProperty("sample-value");
        conf.setKerberosRealmSystemProperty("sample-value");
        conf.setLoginConfigType("JavaLoginConfig");
        conf.setRealm("SYNCOPE");
        conf.setLoginConfigurationFile("/opt/jaas/login.conf");

        saveAuthModule("JaasAuthModuleTest", conf);
    }

    @Test
    public void saveWithLdapModule() {
        LDAPAuthModuleConf conf = new LDAPAuthModuleConf();
        conf.setBaseDn("dc=example,dc=org");
        conf.setSearchFilter("cn={user}");
        conf.setSubtreeSearch(true);
        conf.setLdapUrl("ldap://localhost:1389");
        conf.setPrincipalAttributeId("uid");
        conf.setBindCredential("Password");

        saveAuthModule("LDAPAuthModuleTest", conf);
    }

    @Test
    public void saveWithGoogleAuthenticatorModule() {
        GoogleMfaAuthModuleConf conf = new GoogleMfaAuthModuleConf();
        conf.setCodeDigits(6);
        conf.setIssuer("SyncopeTest");
        conf.setLabel("Syncope");
        conf.setTimeStepSize(30);
        conf.setWindowSize(3);

        saveAuthModule("GoogleMfaAuthModuleTest", conf);
    }

    @Test
    public void saveWithDuoAuthenticatorModule() {
        DuoMfaAuthModuleConf conf = new DuoMfaAuthModuleConf();
        conf.setSecretKey("Q2IU2i6BFNd6VYflZT8Evl6lF7oPlj4PM15BmRU7");
        conf.setIntegrationKey("DIOXVRZD1UMZ8XXMNFQ6");
        conf.setApiHost("theapi.duosecurity.com");
        saveAuthModule("DuoMfaAuthModuleTest", conf);
    }

    @Test
    public void saveWithOIDCAuthModule() {
        OIDCAuthModuleConf conf = new OIDCAuthModuleConf();
        conf.setClientId("OIDCTestId");
        conf.setDiscoveryUri("www.testurl.com");
        conf.setUserIdAttribute("username");
        conf.setResponseType("code");
        conf.setScope("openid email profile");

        saveAuthModule("OIDCAuthModuleTest", conf);
    }

    @Test
    public void saveWithJDBCModule() {
        JDBCAuthModuleConf conf = new JDBCAuthModuleConf();
        conf.setSql("SELECT * FROM table WHERE name=?");
        conf.setFieldPassword("password");

        saveAuthModule("JDBCAuthModuleTest", conf);
    }

    @Test
    public void saveWithSyncopeModule() {
        SyncopeAuthModuleConf conf = new SyncopeAuthModuleConf();
        conf.setDomain("Master");

        saveAuthModule("SyncopeAuthModuleTest", conf);
    }

    @Test
    public void saveWithSAML2IdPModule() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        conf.setServiceProviderEntityId("testEntityId");
        conf.setProviderName("testProviderName");
        saveAuthModule("SAML2IdPAuthModuleTest", conf);
    }

    @Test
    public void saveWithSimpleMfaModule() {
        SimpleMfaAuthModuleConf conf = new SimpleMfaAuthModuleConf();
        conf.setTokenLength(9);
        conf.setTimeToKillInSeconds(120);
        saveAuthModule("SimpleMfaAuthModuleConf", conf);
    }

    @Test
    public void updateWithLDAPModule() {
        AuthModule module = authModuleDAO.findById("DefaultLDAPAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        LDAPAuthModuleConf.class.cast(conf).setBaseDn("dc=example2,dc=org");
        LDAPAuthModuleConf.class.cast(conf).setSearchFilter("cn={user2}");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("dc=example2,dc=org", LDAPAuthModuleConf.class.cast(found.getConf()).getBaseDn());
        assertEquals("cn={user2}", LDAPAuthModuleConf.class.cast(found.getConf()).getSearchFilter());
    }

    @Test
    public void updateWithJDBCModule() {
        AuthModule module = authModuleDAO.findById("DefaultJDBCAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        JDBCAuthModuleConf.class.cast(conf).setSql("SELECT * FROM otherTable WHERE name=?");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("SELECT * FROM otherTable WHERE name=?", JDBCAuthModuleConf.class.cast(found.getConf()).getSql());
    }

    @Test
    public void updateWithGoogleMfaModule() {
        AuthModule module = authModuleDAO.findById("DefaultGoogleMfaAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        GoogleMfaAuthModuleConf.class.cast(conf).setLabel("newLabel");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("newLabel", GoogleMfaAuthModuleConf.class.cast(found.getConf()).getLabel());
    }

    @Test
    public void updateWithDuoMfaModule() {
        AuthModule module = authModuleDAO.findById("DefaultDuoMfaAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        String secretKey = UUID.randomUUID().toString();
        DuoMfaAuthModuleConf.class.cast(conf).setSecretKey(secretKey);
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals(secretKey, DuoMfaAuthModuleConf.class.cast(found.getConf()).getSecretKey());
    }

    @Test
    public void updateWithSAML2IdPModule() {
        AuthModule module = authModuleDAO.findById("DefaultSAML2IdPAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        SAML2IdPAuthModuleConf.class.cast(conf).setServiceProviderEntityId("newEntityId");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertNotNull(found);
        assertEquals("newEntityId", SAML2IdPAuthModuleConf.class.cast(found.getConf()).getServiceProviderEntityId());
    }

    @Test
    public void updateWithOIDCModule() {
        AuthModule module = authModuleDAO.findById("DefaultOIDCAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        OIDCAuthModuleConf.class.cast(conf).setResponseType("newCode");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("newCode", OIDCAuthModuleConf.class.cast(found.getConf()).getResponseType());
    }

    @Test
    public void updateWithJaasModule() {
        AuthModule module = authModuleDAO.findById("DefaultJaasAuthModule").orElseThrow();
        AuthModuleConf conf = module.getConf();
        JaasAuthModuleConf.class.cast(conf).setRealm("SYNCOPE_NEW");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("SYNCOPE_NEW", JaasAuthModuleConf.class.cast(found.getConf()).getRealm());
    }

    @Test
    public void updateWithStaticModule() {
        AuthModule module = authModuleDAO.findById("DefaultStaticAuthModule").orElseThrow();
        assertNotNull(module);
        assertEquals(1, StaticAuthModuleConf.class.cast(module.getConf()).getUsers().size());
        AuthModuleConf conf = module.getConf();
        StaticAuthModuleConf.class.cast(conf).getUsers().put("user3", "user3Password123");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals(2, StaticAuthModuleConf.class.cast(found.getConf()).getUsers().size());
    }

    @Test
    public void updateWithSyncopeModule() {
        AuthModule module = authModuleDAO.findById("DefaultSyncopeAuthModule").orElseThrow();

        AuthModuleConf conf = module.getConf();
        SyncopeAuthModuleConf.class.cast(conf).setDomain("Two");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("Two", SyncopeAuthModuleConf.class.cast(found.getConf()).getDomain());
    }

    @Test
    public void delete() {
        assertTrue(authModuleDAO.findById("DefaultSyncopeAuthModule").isPresent());

        authModuleDAO.deleteById("DefaultSyncopeAuthModule");

        assertTrue(authModuleDAO.findById("DefaultSyncopeAuthModule").isEmpty());
    }
}
