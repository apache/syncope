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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.GoogleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.JDBCAuthModuleConf;
import org.apache.syncope.common.lib.auth.JaasAuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.RadiusAuthModuleConf;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.auth.U2FAuthModuleConf;
import org.apache.syncope.core.persistence.api.dao.auth.AuthModuleDAO;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModuleItem;

@Transactional("Master")
public class AuthModuleTest extends AbstractTest {

    @Autowired
    private AuthModuleDAO authModuleDAO;

    @Test
    public void find() {
        AuthModule authModule = authModuleDAO.find("be456831-593d-4003-b273-4c3fb61700df");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3ed8f6-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3edbbc-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3ed7e8-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3ed4e6-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3edc98-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3ed9d2-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("4c3edd60-7008-11ea-bc55-0242ac130003");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("07c528f3-63b4-4dc1-a4da-87f35b8bdec8");
        assertNotNull(authModule);

        authModule = authModuleDAO.find("f6e1288d-50d9-45fe-82ee-597c42242205");
        assertNotNull(authModule);

        authModule = authModuleDAO.find(UUID.randomUUID().toString());
        assertNull(authModule);
    }

    @Test
    public void findAll() {
        List<AuthModule> modules = authModuleDAO.findAll();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        assertTrue(modules.size() >= 10);
    }

    @Test
    public void findByAuthModuleImpl() {
        AuthModule ldapAuthModule = authModuleDAO.find("be456831-593d-4003-b273-4c3fb61700df");
        assertNotNull(ldapAuthModule);
        AuthModule jdbcAuthModule = authModuleDAO.find("4c3ed7e8-7008-11ea-bc55-0242ac130003");
        assertNotNull(jdbcAuthModule);
        AuthModule googleMfaAuthModule = authModuleDAO.find("4c3ed4e6-7008-11ea-bc55-0242ac130003");
        assertNotNull(googleMfaAuthModule);
        AuthModule oidcAuthModule = authModuleDAO.find("4c3ed8f6-7008-11ea-bc55-0242ac130003");
        assertNotNull(oidcAuthModule);
        AuthModule saml2IdPAuthModule = authModuleDAO.find("4c3ed9d2-7008-11ea-bc55-0242ac130003");
        assertNotNull(saml2IdPAuthModule);
        AuthModule jaasAuthModule = authModuleDAO.find("4c3edbbc-7008-11ea-bc55-0242ac130003");
        assertNotNull(jaasAuthModule);
        AuthModule staticAuthModule = authModuleDAO.find("4c3edc98-7008-11ea-bc55-0242ac130003");
        assertNotNull(staticAuthModule);
        AuthModule syncopeAuthModule = authModuleDAO.find("4c3edd60-7008-11ea-bc55-0242ac130003");
        assertNotNull(syncopeAuthModule);
        AuthModule radiusAuthModule = authModuleDAO.find("07c528f3-63b4-4dc1-a4da-87f35b8bdec8");
        assertNotNull(radiusAuthModule);
        AuthModule u2fAuthModule = authModuleDAO.find("f6e1288d-50d9-45fe-82ee-597c42242205");
        assertNotNull(u2fAuthModule);

        assertTrue(isSpecificConf(ldapAuthModule.getConf(), LDAPAuthModuleConf.class));
        assertFalse(isSpecificConf(ldapAuthModule.getConf(), JDBCAuthModuleConf.class));

        assertTrue(isSpecificConf(jdbcAuthModule.getConf(), JDBCAuthModuleConf.class));
        assertFalse(isSpecificConf(jdbcAuthModule.getConf(), GoogleMfaAuthModuleConf.class));

        assertTrue(isSpecificConf(googleMfaAuthModule.getConf(), GoogleMfaAuthModuleConf.class));
        assertFalse(isSpecificConf(googleMfaAuthModule.getConf(), OIDCAuthModuleConf.class));

        assertTrue(isSpecificConf(oidcAuthModule.getConf(), OIDCAuthModuleConf.class));
        assertFalse(isSpecificConf(oidcAuthModule.getConf(), SAML2IdPAuthModuleConf.class));

        assertTrue(isSpecificConf(saml2IdPAuthModule.getConf(), SAML2IdPAuthModuleConf.class));
        assertFalse(isSpecificConf(saml2IdPAuthModule.getConf(), JaasAuthModuleConf.class));

        assertTrue(isSpecificConf(jaasAuthModule.getConf(), JaasAuthModuleConf.class));
        assertFalse(isSpecificConf(jaasAuthModule.getConf(), StaticAuthModuleConf.class));

        assertTrue(isSpecificConf(staticAuthModule.getConf(), StaticAuthModuleConf.class));
        assertFalse(isSpecificConf(staticAuthModule.getConf(), SyncopeAuthModuleConf.class));

        assertTrue(isSpecificConf(syncopeAuthModule.getConf(), SyncopeAuthModuleConf.class));
        assertFalse(isSpecificConf(syncopeAuthModule.getConf(), RadiusAuthModuleConf.class));

        assertTrue(isSpecificConf(radiusAuthModule.getConf(), RadiusAuthModuleConf.class));
        assertFalse(isSpecificConf(radiusAuthModule.getConf(), U2FAuthModuleConf.class));

        assertTrue(isSpecificConf(u2fAuthModule.getConf(), U2FAuthModuleConf.class));
        assertFalse(isSpecificConf(u2fAuthModule.getConf(), LDAPAuthModuleConf.class));
    }

    @Test
    public void findByType() {
        List<AuthModule> authModules = authModuleDAO.findAll();
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), LDAPAuthModuleConf.class)
                && authModule.getName().equals("DefaultLDAPAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JDBCAuthModuleConf.class)
                && authModule.getName().equals("DefaultJDBCAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), GoogleMfaAuthModuleConf.class)
                && authModule.getName().equals("DefaultGoogleMfaAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), OIDCAuthModuleConf.class)
                && authModule.getName().equals("DefaultOIDCAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SAML2IdPAuthModuleConf.class)
                && authModule.getName().equals("DefaultSAML2IdPAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JaasAuthModuleConf.class)
                && authModule.getName().equals("DefaultJaasAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), StaticAuthModuleConf.class)
                && authModule.getName().equals("DefaultStaticAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SyncopeAuthModuleConf.class)
                && authModule.getName().equals("DefaultSyncopeAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), RadiusAuthModuleConf.class)
                && authModule.getName().equals("DefaultRadiusAuthModule")));
        assertTrue(authModules.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), U2FAuthModuleConf.class)
                && authModule.getName().equals("DefaultU2FAuthModule")));
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
        conf.setUserIdAttribute("uid");
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
    public void saveWithOIDCAuthModule() {
        OIDCAuthModuleConf conf = new OIDCAuthModuleConf();
        conf.setId("OIDCTestId");
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
        conf.getPrincipalAttributeList().addAll(List.of("sn", "cn:commonName", "givenName"));

        saveAuthModule("JDBCAuthModuleTest", conf);
    }

    @Test
    public void saveWithSyncopeModule() {
        SyncopeAuthModuleConf conf = new SyncopeAuthModuleConf();
        conf.setDomain("Master");
        conf.setUrl("http://mydomain.com/syncope/rest");

        saveAuthModule("SyncopeAuthModuleTest", conf);
    }

    @Test
    public void saveWithSAML2IdPModule() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        conf.setServiceProviderEntityId("testEntityId");
        conf.setProviderName("testProviderName");
        conf.setServiceProviderMetadataPath("file:/etc/metadata");

        saveAuthModule("SAML2IdPAuthModuleTest", conf);
    }

    @Test
    public void saveWithRadiusModule() {
        RadiusAuthModuleConf conf = new RadiusAuthModuleConf();
        conf.setProtocol("MSCHAPv2");
        conf.setInetAddress("1.2.3.4");
        conf.setSharedSecret("xyz");
        conf.setSocketTimeout(40);

        saveAuthModule("RadiusAuthModuleTest", conf);
    }

    @Test
    public void saveWithU2FModule() {
        U2FAuthModuleConf conf = new U2FAuthModuleConf();
        conf.setExpireDevices(50);

        saveAuthModule("U2FAuthModuleTest", conf);
    }

    @Test
    public void updateWithLDAPModule() {
        AuthModule module = authModuleDAO.find("be456831-593d-4003-b273-4c3fb61700df");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        LDAPAuthModuleConf.class.cast(conf).setBaseDn("dc=example2,dc=org");
        LDAPAuthModuleConf.class.cast(conf).setSearchFilter("cn={user2}");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("dc=example2,dc=org", LDAPAuthModuleConf.class.cast(found.getConf()).getBaseDn());
        assertEquals("cn={user2}", LDAPAuthModuleConf.class.cast(found.getConf()).getSearchFilter());
    }

    @Test
    public void updateWithJDBCModule() {
        AuthModule module = authModuleDAO.find("4c3ed7e8-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        JDBCAuthModuleConf.class.cast(conf).setSql("SELECT * FROM otherTable WHERE name=?");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("SELECT * FROM otherTable WHERE name=?", JDBCAuthModuleConf.class.cast(found.getConf()).getSql());
    }

    @Test
    public void updateWithGoogleMfaModule() {
        AuthModule module = authModuleDAO.find("4c3ed4e6-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        GoogleMfaAuthModuleConf.class.cast(conf).setLabel("newLabel");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("newLabel", GoogleMfaAuthModuleConf.class.cast(found.getConf()).getLabel());
    }

    @Test
    public void updateWithSAML2IdPModule() {
        AuthModule module = authModuleDAO.find("4c3ed9d2-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        SAML2IdPAuthModuleConf.class.cast(conf).setServiceProviderEntityId("newEntityId");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("newEntityId", SAML2IdPAuthModuleConf.class.cast(found.getConf()).getServiceProviderEntityId());
    }

    @Test
    public void updateWithOIDCModule() {
        AuthModule module = authModuleDAO.find("4c3ed8f6-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        OIDCAuthModuleConf.class.cast(conf).setResponseType("newCode");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("newCode", OIDCAuthModuleConf.class.cast(found.getConf()).getResponseType());
    }

    @Test
    public void updateWithJaasModule() {
        AuthModule module = authModuleDAO.find("4c3edbbc-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        JaasAuthModuleConf.class.cast(conf).setRealm("SYNCOPE_NEW");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("SYNCOPE_NEW", JaasAuthModuleConf.class.cast(found.getConf()).getRealm());
    }

    @Test
    public void updateWithStaticModule() {
        AuthModule module = authModuleDAO.find("4c3edc98-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);
        assertEquals(1, StaticAuthModuleConf.class.cast(module.getConf()).getUsers().size());
        AuthModuleConf conf = module.getConf();
        StaticAuthModuleConf.class.cast(conf).getUsers().put("user3", "user3Password123");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals(2, StaticAuthModuleConf.class.cast(found.getConf()).getUsers().size());
    }

    @Test
    public void updateWithRadiusModule() {
        AuthModule module = authModuleDAO.find("07c528f3-63b4-4dc1-a4da-87f35b8bdec8");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        RadiusAuthModuleConf.class.cast(conf).setSocketTimeout(45);
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals(45, RadiusAuthModuleConf.class.cast(found.getConf()).getSocketTimeout());
    }

    @Test
    public void updateWithU2fModule() {
        AuthModule module = authModuleDAO.find("f6e1288d-50d9-45fe-82ee-597c42242205");
        assertNotNull(module);
        AuthModuleConf conf = module.getConf();
        U2FAuthModuleConf.class.cast(conf).setExpireDevices(24);
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals(24, U2FAuthModuleConf.class.cast(found.getConf()).getExpireDevices());
    }

    @Test
    public void updateWithSyncopeModule() {
        AuthModule module = authModuleDAO.find("4c3edd60-7008-11ea-bc55-0242ac130003");
        assertNotNull(module);

        AuthModuleConf conf = module.getConf();
        SyncopeAuthModuleConf.class.cast(conf).setDomain("Two");
        module.setConf(conf);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AuthModule found = authModuleDAO.find(module.getKey());
        assertNotNull(found);
        assertEquals("Two", SyncopeAuthModuleConf.class.cast(found.getConf()).getDomain());
    }

    @Test
    public void delete() {
        AuthModule authModule = authModuleDAO.find("be456831-593d-4003-b273-4c3fb61700df");
        assertNotNull(authModule);

        authModuleDAO.delete("be456831-593d-4003-b273-4c3fb61700df");

        authModule = authModuleDAO.find("be456831-593d-4003-b273-4c3fb61700df");
        assertNull(authModule);
    }

    private void saveAuthModule(final String name, final AuthModuleConf conf) {
        AuthModule module = entityFactory.newEntity(AuthModule.class);
        module.setName(name);
        module.setDescription("An authentication module");
        module.setConf(conf);

        AuthModuleItem keyMapping = entityFactory.newEntity(AuthModuleItem.class);
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        keyMapping.setAuthModule(module);
        module.add(keyMapping);

        AuthModuleItem fullnameMapping = entityFactory.newEntity(AuthModuleItem.class);
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        fullnameMapping.setAuthModule(module);
        module.add(fullnameMapping);

        module = authModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        assertEquals(module, authModuleDAO.find(module.getKey()));
        assertEquals(2, module.getItems().size());
    }

    private static boolean isSpecificConf(final AuthModuleConf conf, final Class<? extends AuthModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }
}
