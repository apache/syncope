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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.GoogleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.JDBCAuthModuleConf;
import org.apache.syncope.common.lib.auth.JaasAuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OAuth20AuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AuthModuleITCase extends AbstractITCase {

    private enum AuthModuleSupportedType {
        GOOGLE_MFA,
        DUO,
        SAML2_IDP,
        STATIC,
        SYNCOPE,
        LDAP,
        JAAS,
        JDBC,
        OIDC,
        OAUTH20;

    };

    private static AuthModuleTO createAuthModule(final AuthModuleTO authModule) {
        Response response = AUTH_MODULE_SERVICE.create(authModule);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), AuthModuleService.class, authModule.getClass());
    }

    private static AuthModuleTO buildAuthModuleTO(final AuthModuleSupportedType type) {
        AuthModuleTO authModuleTO = new AuthModuleTO();
        authModuleTO.setKey("Test" + type + "AuthenticationModule" + getUUIDString());
        authModuleTO.setDescription("A test " + type + " Authentication Module");

        AuthModuleConf conf;
        switch (type) {
            case LDAP:
                conf = new LDAPAuthModuleConf();
                LDAPAuthModuleConf.class.cast(conf).setBaseDn("dc=example,dc=org");
                LDAPAuthModuleConf.class.cast(conf).setSearchFilter("cn={user}");
                LDAPAuthModuleConf.class.cast(conf).setSubtreeSearch(true);
                LDAPAuthModuleConf.class.cast(conf).setLdapUrl("ldap://localhost:1389");
                LDAPAuthModuleConf.class.cast(conf).setPrincipalAttributeId("uid");
                LDAPAuthModuleConf.class.cast(conf).setBaseDn("cn=Directory Manager,dc=example,dc=org");
                LDAPAuthModuleConf.class.cast(conf).setBindCredential("Password");
                break;

            case GOOGLE_MFA:
                conf = new GoogleMfaAuthModuleConf();
                GoogleMfaAuthModuleConf.class.cast(conf).setCodeDigits(6);
                GoogleMfaAuthModuleConf.class.cast(conf).setIssuer("SyncopeTest");
                GoogleMfaAuthModuleConf.class.cast(conf).setLabel("Syncope");
                GoogleMfaAuthModuleConf.class.cast(conf).setTimeStepSize(30);
                GoogleMfaAuthModuleConf.class.cast(conf).setWindowSize(3);
                break;

            case DUO:
                conf = new DuoMfaAuthModuleConf();
                DuoMfaAuthModuleConf.class.cast(conf).setSecretKey("Q2IU2i6BFNd6VYflZT8Evl6lF7oPlj4PM15BmRU7");
                DuoMfaAuthModuleConf.class.cast(conf).setIntegrationKey("DIOXVRZD1UMZ8XXMNFQ6");
                DuoMfaAuthModuleConf.class.cast(conf).setApiHost("theapi.duosecurity.com");
                break;

            case JAAS:
                conf = new JaasAuthModuleConf();
                JaasAuthModuleConf.class.cast(conf).setKerberosKdcSystemProperty("sample-value");
                JaasAuthModuleConf.class.cast(conf).setKerberosRealmSystemProperty("sample-value");
                JaasAuthModuleConf.class.cast(conf).setLoginConfigType("JavaLoginConfig");
                JaasAuthModuleConf.class.cast(conf).setRealm("SYNCOPE");
                JaasAuthModuleConf.class.cast(conf).setLoginConfigurationFile("/opt/jaas/login.conf");
                break;

            case JDBC:
                conf = new JDBCAuthModuleConf();
                JDBCAuthModuleConf.class.cast(conf).setSql("SELECT * FROM table WHERE name=?");
                JDBCAuthModuleConf.class.cast(conf).setFieldPassword("password");
                break;

            case OIDC:
                conf = new OIDCAuthModuleConf();
                OIDCAuthModuleConf.class.cast(conf).setClientId("OIDCTestId");
                OIDCAuthModuleConf.class.cast(conf).setDiscoveryUri("www.testurl.com");
                OIDCAuthModuleConf.class.cast(conf).setUserIdAttribute("username");
                OIDCAuthModuleConf.class.cast(conf).setResponseType("code");
                OIDCAuthModuleConf.class.cast(conf).setScope("openid email profile");
                break;

            case OAUTH20:
                conf = new OAuth20AuthModuleConf();
                OAuth20AuthModuleConf.class.cast(conf).setClientId("OAUTH20TestId");
                OAuth20AuthModuleConf.class.cast(conf).setClientSecret("secret");
                OAuth20AuthModuleConf.class.cast(conf).setClientName("oauth20");
                OAuth20AuthModuleConf.class.cast(conf).setEnabled(true);
                OAuth20AuthModuleConf.class.cast(conf).setCustomParams(Map.of("param1", "param1"));
                OAuth20AuthModuleConf.class.cast(conf).setAuthUrl("https://localhost/oauth2/auth");
                OAuth20AuthModuleConf.class.cast(conf).setProfileUrl("https://localhost/oauth2/profile");
                OAuth20AuthModuleConf.class.cast(conf).setTokenUrl("https://localhost/oauth2/token");
                OAuth20AuthModuleConf.class.cast(conf).setResponseType("code");
                OAuth20AuthModuleConf.class.cast(conf).setScope("oauth test");
                OAuth20AuthModuleConf.class.cast(conf).setUserIdAttribute("username");
                OAuth20AuthModuleConf.class.cast(conf).setWithState(true);
                break;

            case SAML2_IDP:
                conf = new SAML2IdPAuthModuleConf();
                SAML2IdPAuthModuleConf.class.cast(conf).setServiceProviderEntityId("testEntityId");
                SAML2IdPAuthModuleConf.class.cast(conf).setProviderName("testProviderName");
                break;

            case SYNCOPE:
                conf = new SyncopeAuthModuleConf();
                SyncopeAuthModuleConf.class.cast(conf).setDomain(SyncopeConstants.MASTER_DOMAIN);
                break;

            case STATIC:
            default:
                conf = new StaticAuthModuleConf();
                StaticAuthModuleConf.class.cast(conf).getUsers().put("user1", UUID.randomUUID().toString());
                StaticAuthModuleConf.class.cast(conf).getUsers().put("user2", "user2Password123");
                break;
        }
        authModuleTO.setConf(conf);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        authModuleTO.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        authModuleTO.getItems().add(fullnameMapping);

        return authModuleTO;
    }

    private static boolean isSpecificConf(final AuthModuleConf conf, final Class<? extends AuthModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Test
    public void list() {
        List<AuthModuleTO> authModuleTOs = AUTH_MODULE_SERVICE.list();
        assertNotNull(authModuleTOs);
        assertFalse(authModuleTOs.isEmpty());

        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), LDAPAuthModuleConf.class)
                && authModule.getKey().equals("DefaultLDAPAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JDBCAuthModuleConf.class)
                && authModule.getKey().equals("DefaultJDBCAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), GoogleMfaAuthModuleConf.class)
                && authModule.getKey().equals("DefaultGoogleMfaAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), DuoMfaAuthModuleConf.class)
                && authModule.getKey().equals("DefaultDuoMfaAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), OIDCAuthModuleConf.class)
                && authModule.getKey().equals("DefaultOIDCAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SAML2IdPAuthModuleConf.class)
                && authModule.getKey().equals("DefaultSAML2IdPAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), JaasAuthModuleConf.class)
                && authModule.getKey().equals("DefaultJaasAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), StaticAuthModuleConf.class)
                && authModule.getKey().equals("DefaultStaticAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SyncopeAuthModuleConf.class)
                && authModule.getKey().equals("DefaultSyncopeAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), OAuth20AuthModuleConf.class)
                && authModule.getKey().equals("DefaultOAuth20AuthModule")));
    }

    @Test
    public void getLDAPAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultLDAPAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), LDAPAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), JDBCAuthModuleConf.class));
    }

    @Test
    public void getJDBCAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultJDBCAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), JDBCAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), GoogleMfaAuthModuleConf.class));
    }

    @Test
    public void getGoogleMfaAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultGoogleMfaAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), GoogleMfaAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), OIDCAuthModuleConf.class));
    }

    @Test
    public void getDuoMfaAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultDuoMfaAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), DuoMfaAuthModuleConf.class));
    }

    @Test
    public void getOIDCAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultOIDCAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), OIDCAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), SAML2IdPAuthModuleConf.class));
    }

    @Test
    public void getOAuth20AuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultOAuth20AuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), OAuth20AuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), SAML2IdPAuthModuleConf.class));
    }

    @Test
    public void getSAML2IdPAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultSAML2IdPAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), SAML2IdPAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), JaasAuthModuleConf.class));
    }

    @Test
    public void getJaasAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultJaasAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), JaasAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), StaticAuthModuleConf.class));
    }

    @Test
    public void getStaticAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultStaticAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), StaticAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), SyncopeAuthModuleConf.class));
    }

    @Test
    public void getSyncopeAuthModule() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read("DefaultSyncopeAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), SyncopeAuthModuleConf.class));
    }

    @Test
    public void create() {
        EnumSet.allOf(AuthModuleSupportedType.class).forEach(type -> {
            AuthModuleTO authModuleTO = createAuthModule(buildAuthModuleTO(type));
            assertNotNull(authModuleTO);
            assertTrue(authModuleTO.getDescription().contains("A test " + type + " Authentication Module"));
            assertEquals(2, authModuleTO.getItems().size());
        });
    }

    @Test
    public void updateGoogleMfaAuthModule() {
        AuthModuleTO googleMfaAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultGoogleMfaAuthModule");
        assertNotNull(googleMfaAuthModuleTO);

        AuthModuleTO newGoogleMfaAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.GOOGLE_MFA);
        newGoogleMfaAuthModuleTO = createAuthModule(newGoogleMfaAuthModuleTO);
        assertNotNull(newGoogleMfaAuthModuleTO);

        AuthModuleConf conf = googleMfaAuthModuleTO.getConf();
        assertNotNull(conf);
        GoogleMfaAuthModuleConf.class.cast(conf).setLabel("newLabel");
        newGoogleMfaAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newGoogleMfaAuthModuleTO);
        newGoogleMfaAuthModuleTO = AUTH_MODULE_SERVICE.read(newGoogleMfaAuthModuleTO.getKey());
        assertNotNull(newGoogleMfaAuthModuleTO);

        conf = newGoogleMfaAuthModuleTO.getConf();
        assertEquals("newLabel", GoogleMfaAuthModuleConf.class.cast(conf).getLabel());
    }

    @Test
    public void updateDuoMfaAuthModule() {
        AuthModuleTO duoMfaAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultDuoMfaAuthModule");
        assertNotNull(duoMfaAuthModuleTO);

        AuthModuleTO newDuoMfaAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.DUO);
        newDuoMfaAuthModuleTO = createAuthModule(newDuoMfaAuthModuleTO);
        assertNotNull(newDuoMfaAuthModuleTO);

        AuthModuleConf conf = duoMfaAuthModuleTO.getConf();
        assertNotNull(conf);
        String secretKey = UUID.randomUUID().toString();
        DuoMfaAuthModuleConf.class.cast(conf).setSecretKey(secretKey);
        newDuoMfaAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newDuoMfaAuthModuleTO);
        newDuoMfaAuthModuleTO = AUTH_MODULE_SERVICE.read(newDuoMfaAuthModuleTO.getKey());
        assertNotNull(newDuoMfaAuthModuleTO);

        conf = newDuoMfaAuthModuleTO.getConf();
        assertEquals(secretKey, DuoMfaAuthModuleConf.class.cast(conf).getSecretKey());
    }

    @Test
    public void updateLDAPAuthModule() {
        AuthModuleTO ldapAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultLDAPAuthModule");
        assertNotNull(ldapAuthModuleTO);

        AuthModuleTO newLdapAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.LDAP);
        newLdapAuthModuleTO = createAuthModule(newLdapAuthModuleTO);
        assertNotNull(newLdapAuthModuleTO);

        AuthModuleConf conf = ldapAuthModuleTO.getConf();
        assertNotNull(conf);
        LDAPAuthModuleConf.class.cast(conf).setSubtreeSearch(false);
        newLdapAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newLdapAuthModuleTO);
        newLdapAuthModuleTO = AUTH_MODULE_SERVICE.read(newLdapAuthModuleTO.getKey());
        assertNotNull(newLdapAuthModuleTO);

        conf = newLdapAuthModuleTO.getConf();
        assertFalse(LDAPAuthModuleConf.class.cast(conf).isSubtreeSearch());
    }

    @Test
    public void updateSAML2IdPAuthModule() {
        AuthModuleTO saml2IdpAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultSAML2IdPAuthModule");
        assertNotNull(saml2IdpAuthModuleTO);

        AuthModuleTO newsaml2IdpAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.SAML2_IDP);
        newsaml2IdpAuthModuleTO = createAuthModule(newsaml2IdpAuthModuleTO);
        assertNotNull(newsaml2IdpAuthModuleTO);

        AuthModuleConf conf = saml2IdpAuthModuleTO.getConf();
        assertNotNull(conf);
        SAML2IdPAuthModuleConf.class.cast(conf).setServiceProviderEntityId("newEntityId");
        newsaml2IdpAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newsaml2IdpAuthModuleTO);
        newsaml2IdpAuthModuleTO = AUTH_MODULE_SERVICE.read(newsaml2IdpAuthModuleTO.getKey());
        assertNotNull(newsaml2IdpAuthModuleTO);

        conf = newsaml2IdpAuthModuleTO.getConf();
        assertEquals("newEntityId", SAML2IdPAuthModuleConf.class.cast(conf).getServiceProviderEntityId());
    }

    @Test
    public void updateOIDCAuthModule() {
        AuthModuleTO oidcAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultOIDCAuthModule");
        assertNotNull(oidcAuthModuleTO);

        AuthModuleTO newOIDCAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.OIDC);
        newOIDCAuthModuleTO = createAuthModule(newOIDCAuthModuleTO);
        assertNotNull(newOIDCAuthModuleTO);

        AuthModuleConf conf = oidcAuthModuleTO.getConf();
        assertNotNull(conf);
        OIDCAuthModuleConf.class.cast(conf).setResponseType("newCode");
        newOIDCAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newOIDCAuthModuleTO);
        newOIDCAuthModuleTO = AUTH_MODULE_SERVICE.read(newOIDCAuthModuleTO.getKey());
        assertNotNull(newOIDCAuthModuleTO);

        conf = newOIDCAuthModuleTO.getConf();
        assertEquals("newCode", OIDCAuthModuleConf.class.cast(conf).getResponseType());
    }

    @Test
    public void updateOAuth20AuthModule() {
        AuthModuleTO oauth20AuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultOAuth20AuthModule");
        assertNotNull(oauth20AuthModuleTO);

        AuthModuleTO newoauth20AuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.OAUTH20);
        newoauth20AuthModuleTO = createAuthModule(newoauth20AuthModuleTO);
        assertNotNull(newoauth20AuthModuleTO);

        AuthModuleConf conf = oauth20AuthModuleTO.getConf();
        assertNotNull(conf);
        OAuth20AuthModuleConf.class.cast(conf).setClientName("OAUTH APP");
        newoauth20AuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newoauth20AuthModuleTO);
        newoauth20AuthModuleTO = AUTH_MODULE_SERVICE.read(newoauth20AuthModuleTO.getKey());
        assertNotNull(newoauth20AuthModuleTO);

        conf = newoauth20AuthModuleTO.getConf();
        assertEquals("OAUTH APP", OAuth20AuthModuleConf.class.cast(conf).getClientName());
    }

    @Test
    public void updateJDBCAuthModule() {
        AuthModuleTO jdbcAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultJDBCAuthModule");
        assertNotNull(jdbcAuthModuleTO);

        AuthModuleTO newJDBCAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.JDBC);
        newJDBCAuthModuleTO = createAuthModule(newJDBCAuthModuleTO);
        assertNotNull(newJDBCAuthModuleTO);

        AuthModuleConf conf = jdbcAuthModuleTO.getConf();
        assertNotNull(conf);
        JDBCAuthModuleConf.class.cast(conf).setFieldPassword("uPassword");
        newJDBCAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newJDBCAuthModuleTO);
        newJDBCAuthModuleTO = AUTH_MODULE_SERVICE.read(newJDBCAuthModuleTO.getKey());
        assertNotNull(newJDBCAuthModuleTO);

        conf = newJDBCAuthModuleTO.getConf();
        assertEquals("uPassword", JDBCAuthModuleConf.class.cast(conf).getFieldPassword());
    }

    @Test
    public void updateJaasAuthModule() {
        AuthModuleTO jaasAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultJaasAuthModule");
        assertNotNull(jaasAuthModuleTO);

        AuthModuleTO newJaasAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.JAAS);
        newJaasAuthModuleTO = createAuthModule(newJaasAuthModuleTO);
        assertNotNull(newJaasAuthModuleTO);

        AuthModuleConf conf = jaasAuthModuleTO.getConf();
        assertNotNull(conf);
        JaasAuthModuleConf.class.cast(conf).setRealm("SYNCOPE_NEW");
        newJaasAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newJaasAuthModuleTO);
        newJaasAuthModuleTO = AUTH_MODULE_SERVICE.read(newJaasAuthModuleTO.getKey());
        assertNotNull(newJaasAuthModuleTO);

        conf = newJaasAuthModuleTO.getConf();
        assertEquals("SYNCOPE_NEW", JaasAuthModuleConf.class.cast(conf).getRealm());
    }

    @Test
    public void updateStaticAuthModule() {
        AuthModuleTO staticAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultStaticAuthModule");
        assertNotNull(staticAuthModuleTO);

        AuthModuleTO newStaticAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.STATIC);
        newStaticAuthModuleTO = createAuthModule(newStaticAuthModuleTO);
        assertNotNull(newStaticAuthModuleTO);

        AuthModuleConf conf = staticAuthModuleTO.getConf();
        assertNotNull(conf);
        assertEquals(1, StaticAuthModuleConf.class.cast(conf).getUsers().size());
        StaticAuthModuleConf.class.cast(conf).getUsers().put("user3", "user3Password123");
        newStaticAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newStaticAuthModuleTO);
        newStaticAuthModuleTO = AUTH_MODULE_SERVICE.read(newStaticAuthModuleTO.getKey());
        assertNotNull(newStaticAuthModuleTO);

        conf = newStaticAuthModuleTO.getConf();
        assertEquals(2, StaticAuthModuleConf.class.cast(conf).getUsers().size());
    }

    @Test
    public void updateSyncopeAuthModule() {
        AuthModuleTO syncopeAuthModuleTO = AUTH_MODULE_SERVICE.read("DefaultSyncopeAuthModule");
        assertNotNull(syncopeAuthModuleTO);

        AuthModuleTO newSyncopeAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.SYNCOPE);
        newSyncopeAuthModuleTO = createAuthModule(newSyncopeAuthModuleTO);
        assertNotNull(newSyncopeAuthModuleTO);

        AuthModuleConf conf = syncopeAuthModuleTO.getConf();
        assertNotNull(conf);
        SyncopeAuthModuleConf.class.cast(conf).setDomain("Two");
        newSyncopeAuthModuleTO.setConf(conf);

        // update new auth module
        AUTH_MODULE_SERVICE.update(newSyncopeAuthModuleTO);
        newSyncopeAuthModuleTO = AUTH_MODULE_SERVICE.read(newSyncopeAuthModuleTO.getKey());
        assertNotNull(newSyncopeAuthModuleTO);

        conf = newSyncopeAuthModuleTO.getConf();
        assertEquals("Two", SyncopeAuthModuleConf.class.cast(conf).getDomain());
    }

    @Test
    public void delete() throws IOException {
        EnumSet.allOf(AuthModuleSupportedType.class).forEach(type -> {
            AuthModuleTO read = createAuthModule(buildAuthModuleTO(type));
            assertNotNull(read);

            AUTH_MODULE_SERVICE.delete(read.getKey());

            try {
                AUTH_MODULE_SERVICE.read(read.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertNotNull(e);
            }
        });
    }
}
