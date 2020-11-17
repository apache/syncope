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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
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
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.ItemTO;

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
        U2F,
        RADIUS,
        OIDC;

    };

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
                LDAPAuthModuleConf.class.cast(conf).setUserIdAttribute("uid");
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
                DuoMfaAuthModuleConf.class.cast(conf).setApplicationKey("u4IHCaREMB7Cb0S6QMISAgHycpj6lPBkDGfWt99I");
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
                JDBCAuthModuleConf.class.cast(conf).getPrincipalAttributeList().addAll(
                        List.of("sn", "cn:commonName", "givenName"));
                break;

            case OIDC:
                conf = new OIDCAuthModuleConf();
                OIDCAuthModuleConf.class.cast(conf).setId("OIDCTestId");
                OIDCAuthModuleConf.class.cast(conf).setDiscoveryUri("www.testurl.com");
                OIDCAuthModuleConf.class.cast(conf).setUserIdAttribute("username");
                OIDCAuthModuleConf.class.cast(conf).setResponseType("code");
                OIDCAuthModuleConf.class.cast(conf).setScope("openid email profile");
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

            case U2F:
                conf = new U2FAuthModuleConf();
                U2FAuthModuleConf.class.cast(conf).setExpireDevices(50);
                break;

            case RADIUS:
                conf = new RadiusAuthModuleConf();
                RadiusAuthModuleConf.class.cast(conf).setProtocol("MSCHAPv2");
                RadiusAuthModuleConf.class.cast(conf).setInetAddress("1.2.3.4");
                RadiusAuthModuleConf.class.cast(conf).setSharedSecret("xyz");
                RadiusAuthModuleConf.class.cast(conf).setSocketTimeout(40);
                break;

            case STATIC:
            default:
                conf = new StaticAuthModuleConf();
                StaticAuthModuleConf.class.cast(conf).getUsers().put("user1", UUID.randomUUID().toString());
                StaticAuthModuleConf.class.cast(conf).getUsers().put("user2", "user2Password123");
                break;
        }
        authModuleTO.setConf(conf);

        ItemTO keyMapping = new ItemTO();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        authModuleTO.add(keyMapping);

        ItemTO fullnameMapping = new ItemTO();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        authModuleTO.add(fullnameMapping);

        return authModuleTO;
    }

    private static boolean isSpecificConf(final AuthModuleConf conf, final Class<? extends AuthModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Test
    public void list() {
        List<AuthModuleTO> authModuleTOs = authModuleService.list();
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
                authModule -> isSpecificConf(authModule.getConf(), U2FAuthModuleConf.class)
                && authModule.getKey().equals("DefaultU2FAuthModule")));
        assertTrue(authModuleTOs.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), RadiusAuthModuleConf.class)
                && authModule.getKey().equals("DefaultRadiusAuthModule")));
    }

    @Test
    public void getLDAPAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultLDAPAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), LDAPAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), JDBCAuthModuleConf.class));
    }

    @Test
    public void getJDBCAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultJDBCAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), JDBCAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), GoogleMfaAuthModuleConf.class));
    }

    @Test
    public void getGoogleMfaAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultGoogleMfaAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), GoogleMfaAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), OIDCAuthModuleConf.class));
    }

    @Test
    public void getDuoMfaAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultDuoMfaAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), DuoMfaAuthModuleConf.class));
    }

    @Test
    public void getOIDCAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultOIDCAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), OIDCAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), SAML2IdPAuthModuleConf.class));
    }

    @Test
    public void getSAML2IdPAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultSAML2IdPAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), SAML2IdPAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), JaasAuthModuleConf.class));
    }

    @Test
    public void getJaasAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultJaasAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), JaasAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), StaticAuthModuleConf.class));
    }

    @Test
    public void getStaticAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultStaticAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), StaticAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), SyncopeAuthModuleConf.class));
    }

    @Test
    public void getSyncopeAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultSyncopeAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), SyncopeAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), RadiusAuthModuleConf.class));
    }

    @Test
    public void getRadiusAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultRadiusAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), RadiusAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), U2FAuthModuleConf.class));
    }

    @Test
    public void getU2FAuthModule() {
        AuthModuleTO authModuleTO = authModuleService.read("DefaultU2FAuthModule");

        assertNotNull(authModuleTO);
        assertTrue(StringUtils.isNotBlank(authModuleTO.getDescription()));
        assertTrue(isSpecificConf(authModuleTO.getConf(), U2FAuthModuleConf.class));
        assertFalse(isSpecificConf(authModuleTO.getConf(), LDAPAuthModuleConf.class));
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
        AuthModuleTO googleMfaAuthModuleTO = authModuleService.read("DefaultGoogleMfaAuthModule");
        assertNotNull(googleMfaAuthModuleTO);

        AuthModuleTO newGoogleMfaAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.GOOGLE_MFA);
        newGoogleMfaAuthModuleTO = createAuthModule(newGoogleMfaAuthModuleTO);
        assertNotNull(newGoogleMfaAuthModuleTO);

        AuthModuleConf conf = googleMfaAuthModuleTO.getConf();
        assertNotNull(conf);
        GoogleMfaAuthModuleConf.class.cast(conf).setLabel("newLabel");
        newGoogleMfaAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newGoogleMfaAuthModuleTO);
        newGoogleMfaAuthModuleTO = authModuleService.read(newGoogleMfaAuthModuleTO.getKey());
        assertNotNull(newGoogleMfaAuthModuleTO);

        conf = newGoogleMfaAuthModuleTO.getConf();
        assertEquals("newLabel", GoogleMfaAuthModuleConf.class.cast(conf).getLabel());
    }

    @Test
    public void updateDuoMfaAuthModule() {
        AuthModuleTO duoMfaAuthModuleTO = authModuleService.read("DefaultDuoMfaAuthModule");
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
        authModuleService.update(newDuoMfaAuthModuleTO);
        newDuoMfaAuthModuleTO = authModuleService.read(newDuoMfaAuthModuleTO.getKey());
        assertNotNull(newDuoMfaAuthModuleTO);

        conf = newDuoMfaAuthModuleTO.getConf();
        assertEquals(secretKey, DuoMfaAuthModuleConf.class.cast(conf).getSecretKey());
    }

    @Test
    public void updateLDAPAuthModule() {
        AuthModuleTO ldapAuthModuleTO = authModuleService.read("DefaultLDAPAuthModule");
        assertNotNull(ldapAuthModuleTO);

        AuthModuleTO newLdapAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.LDAP);
        newLdapAuthModuleTO = createAuthModule(newLdapAuthModuleTO);
        assertNotNull(newLdapAuthModuleTO);

        AuthModuleConf conf = ldapAuthModuleTO.getConf();
        assertNotNull(conf);
        LDAPAuthModuleConf.class.cast(conf).setSubtreeSearch(false);
        newLdapAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newLdapAuthModuleTO);
        newLdapAuthModuleTO = authModuleService.read(newLdapAuthModuleTO.getKey());
        assertNotNull(newLdapAuthModuleTO);

        conf = newLdapAuthModuleTO.getConf();
        assertFalse(LDAPAuthModuleConf.class.cast(conf).isSubtreeSearch());
    }

    @Test
    public void updateSAML2IdPAuthModule() {
        AuthModuleTO saml2IdpAuthModuleTO = authModuleService.read("DefaultSAML2IdPAuthModule");
        assertNotNull(saml2IdpAuthModuleTO);

        AuthModuleTO newsaml2IdpAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.SAML2_IDP);
        newsaml2IdpAuthModuleTO = createAuthModule(newsaml2IdpAuthModuleTO);
        assertNotNull(newsaml2IdpAuthModuleTO);

        AuthModuleConf conf = saml2IdpAuthModuleTO.getConf();
        assertNotNull(conf);
        SAML2IdPAuthModuleConf.class.cast(conf).setServiceProviderEntityId("newEntityId");
        newsaml2IdpAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newsaml2IdpAuthModuleTO);
        newsaml2IdpAuthModuleTO = authModuleService.read(newsaml2IdpAuthModuleTO.getKey());
        assertNotNull(newsaml2IdpAuthModuleTO);

        conf = newsaml2IdpAuthModuleTO.getConf();
        assertEquals("newEntityId", SAML2IdPAuthModuleConf.class.cast(conf).getServiceProviderEntityId());
    }

    @Test
    public void updateOIDCAuthModule() {
        AuthModuleTO oidcAuthModuleTO = authModuleService.read("DefaultOIDCAuthModule");
        assertNotNull(oidcAuthModuleTO);

        AuthModuleTO newOIDCAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.OIDC);
        newOIDCAuthModuleTO = createAuthModule(newOIDCAuthModuleTO);
        assertNotNull(newOIDCAuthModuleTO);

        AuthModuleConf conf = oidcAuthModuleTO.getConf();
        assertNotNull(conf);
        OIDCAuthModuleConf.class.cast(conf).setResponseType("newCode");
        newOIDCAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newOIDCAuthModuleTO);
        newOIDCAuthModuleTO = authModuleService.read(newOIDCAuthModuleTO.getKey());
        assertNotNull(newOIDCAuthModuleTO);

        conf = newOIDCAuthModuleTO.getConf();
        assertEquals("newCode", OIDCAuthModuleConf.class.cast(conf).getResponseType());
    }

    @Test
    public void updateJDBCAuthModule() {
        AuthModuleTO jdbcAuthModuleTO = authModuleService.read("DefaultJDBCAuthModule");
        assertNotNull(jdbcAuthModuleTO);

        AuthModuleTO newJDBCAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.JDBC);
        newJDBCAuthModuleTO = createAuthModule(newJDBCAuthModuleTO);
        assertNotNull(newJDBCAuthModuleTO);

        AuthModuleConf conf = jdbcAuthModuleTO.getConf();
        assertNotNull(conf);
        JDBCAuthModuleConf.class.cast(conf).setFieldPassword("uPassword");
        newJDBCAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newJDBCAuthModuleTO);
        newJDBCAuthModuleTO = authModuleService.read(newJDBCAuthModuleTO.getKey());
        assertNotNull(newJDBCAuthModuleTO);

        conf = newJDBCAuthModuleTO.getConf();
        assertEquals("uPassword", JDBCAuthModuleConf.class.cast(conf).getFieldPassword());
    }

    @Test
    public void updateJaasAuthModule() {
        AuthModuleTO jaasAuthModuleTO = authModuleService.read("DefaultJaasAuthModule");
        assertNotNull(jaasAuthModuleTO);

        AuthModuleTO newJaasAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.JAAS);
        newJaasAuthModuleTO = createAuthModule(newJaasAuthModuleTO);
        assertNotNull(newJaasAuthModuleTO);

        AuthModuleConf conf = jaasAuthModuleTO.getConf();
        assertNotNull(conf);
        JaasAuthModuleConf.class.cast(conf).setRealm("SYNCOPE_NEW");
        newJaasAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newJaasAuthModuleTO);
        newJaasAuthModuleTO = authModuleService.read(newJaasAuthModuleTO.getKey());
        assertNotNull(newJaasAuthModuleTO);

        conf = newJaasAuthModuleTO.getConf();
        assertEquals("SYNCOPE_NEW", JaasAuthModuleConf.class.cast(conf).getRealm());
    }

    @Test
    public void updateStaticAuthModule() {
        AuthModuleTO staticAuthModuleTO = authModuleService.read("DefaultStaticAuthModule");
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
        authModuleService.update(newStaticAuthModuleTO);
        newStaticAuthModuleTO = authModuleService.read(newStaticAuthModuleTO.getKey());
        assertNotNull(newStaticAuthModuleTO);

        conf = newStaticAuthModuleTO.getConf();
        assertEquals(2, StaticAuthModuleConf.class.cast(conf).getUsers().size());
    }

    @Test
    public void updateRadiusAuthModule() {
        AuthModuleTO radiusAuthModuleTO = authModuleService.read("DefaultRadiusAuthModule");
        assertNotNull(radiusAuthModuleTO);

        AuthModuleTO newRadiusAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.RADIUS);
        newRadiusAuthModuleTO = createAuthModule(newRadiusAuthModuleTO);
        assertNotNull(newRadiusAuthModuleTO);

        AuthModuleConf conf = radiusAuthModuleTO.getConf();
        assertNotNull(conf);
        RadiusAuthModuleConf.class.cast(conf).setSocketTimeout(45);
        newRadiusAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newRadiusAuthModuleTO);
        newRadiusAuthModuleTO = authModuleService.read(newRadiusAuthModuleTO.getKey());
        assertNotNull(newRadiusAuthModuleTO);

        conf = newRadiusAuthModuleTO.getConf();
        assertEquals(45, RadiusAuthModuleConf.class.cast(conf).getSocketTimeout());
    }

    @Test
    public void updateU2fAuthModule() {
        AuthModuleTO u2fAuthModuleTO = authModuleService.read("DefaultU2FAuthModule");
        assertNotNull(u2fAuthModuleTO);

        AuthModuleTO newU2fAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.U2F);
        newU2fAuthModuleTO = createAuthModule(newU2fAuthModuleTO);
        assertNotNull(newU2fAuthModuleTO);

        AuthModuleConf conf = u2fAuthModuleTO.getConf();
        assertNotNull(conf);
        U2FAuthModuleConf.class.cast(conf).setExpireDevices(24);
        newU2fAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newU2fAuthModuleTO);
        newU2fAuthModuleTO = authModuleService.read(newU2fAuthModuleTO.getKey());
        assertNotNull(newU2fAuthModuleTO);

        conf = newU2fAuthModuleTO.getConf();
        assertEquals(24, U2FAuthModuleConf.class.cast(conf).getExpireDevices());
    }

    @Test
    public void updateSyncopeAuthModule() {
        AuthModuleTO syncopeAuthModuleTO = authModuleService.read("DefaultSyncopeAuthModule");
        assertNotNull(syncopeAuthModuleTO);

        AuthModuleTO newSyncopeAuthModuleTO = buildAuthModuleTO(AuthModuleSupportedType.SYNCOPE);
        newSyncopeAuthModuleTO = createAuthModule(newSyncopeAuthModuleTO);
        assertNotNull(newSyncopeAuthModuleTO);

        AuthModuleConf conf = syncopeAuthModuleTO.getConf();
        assertNotNull(conf);
        SyncopeAuthModuleConf.class.cast(conf).setDomain("Two");
        newSyncopeAuthModuleTO.setConf(conf);

        // update new auth module
        authModuleService.update(newSyncopeAuthModuleTO);
        newSyncopeAuthModuleTO = authModuleService.read(newSyncopeAuthModuleTO.getKey());
        assertNotNull(newSyncopeAuthModuleTO);

        conf = newSyncopeAuthModuleTO.getConf();
        assertEquals("Two", SyncopeAuthModuleConf.class.cast(conf).getDomain());
    }

    @Test
    public void delete() throws IOException {
        EnumSet.allOf(AuthModuleSupportedType.class).forEach(type -> {
            AuthModuleTO read = createAuthModule(buildAuthModuleTO(type));
            assertNotNull(read);

            authModuleService.delete(read.getKey());

            try {
                authModuleService.read(read.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertNotNull(e);
            }
        });
    }
}
