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
package org.apache.syncope.common.lib.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.junit.jupiter.api.Test;

class AuthModuleConfTest {

    private final AuthModuleTO authModule = new AuthModuleTO();

    private final AuthModuleConf.Mapper mapper = mock(AuthModuleConf.Mapper.class);

    @Test
    void simple() {
        StaticAuthModuleConf staticConf = new StaticAuthModuleConf();
        Map<String, Object> staticMap = Map.of("static", staticConf);
        when(mapper.map(authModule, staticConf)).thenReturn(staticMap);
        assertSame(staticMap, staticConf.map(authModule, mapper));

        JDBCAuthModuleConf jdbcConf = new JDBCAuthModuleConf();
        Map<String, Object> jdbcMap = Map.of("jdbc", jdbcConf);
        when(mapper.map(authModule, jdbcConf)).thenReturn(jdbcMap);
        assertSame(jdbcMap, jdbcConf.map(authModule, mapper));

        JaasAuthModuleConf jaasConf = new JaasAuthModuleConf();
        Map<String, Object> jaasMap = Map.of("jaas", jaasConf);
        when(mapper.map(authModule, jaasConf)).thenReturn(jaasMap);
        assertSame(jaasMap, jaasConf.map(authModule, mapper));

        SyncopeAuthModuleConf syncopeConf = new SyncopeAuthModuleConf();
        Map<String, Object> syncopeMap = Map.of("syncope", syncopeConf);
        when(mapper.map(authModule, syncopeConf)).thenReturn(syncopeMap);
        assertSame(syncopeMap, syncopeConf.map(authModule, mapper));

        OktaAuthModuleConf oktaConf = new OktaAuthModuleConf();
        Map<String, Object> oktaMap = Map.of("okta", oktaConf);
        when(mapper.map(authModule, oktaConf)).thenReturn(oktaMap);
        assertSame(oktaMap, oktaConf.map(authModule, mapper));

        DuoMfaAuthModuleConf duoConf = new DuoMfaAuthModuleConf();
        Map<String, Object> duoMap = Map.of("duo", duoConf);
        when(mapper.map(authModule, duoConf)).thenReturn(duoMap);
        assertSame(duoMap, duoConf.map(authModule, mapper));

        SimpleMfaAuthModuleConf simpleConf = new SimpleMfaAuthModuleConf();
        Map<String, Object> simpleMap = Map.of("simple", simpleConf);
        when(mapper.map(authModule, simpleConf)).thenReturn(simpleMap);
        assertSame(simpleMap, simpleConf.map(authModule, mapper));

        AzureActiveDirectoryAuthModuleConf azureConf = new AzureActiveDirectoryAuthModuleConf();
        Map<String, Object> azureMap = Map.of("azure", azureConf);
        when(mapper.map(authModule, azureConf)).thenReturn(azureMap);
        assertSame(azureMap, azureConf.map(authModule, mapper));

        verify(mapper).map(authModule, staticConf);
        verify(mapper).map(authModule, azureConf);
    }

    @Test
    void oidc() {
        OAuth20AuthModuleConf oauth20Conf = new OAuth20AuthModuleConf();
        Map<String, Object> oauth20Map = Map.of("oauth20", oauth20Conf);
        when(mapper.map(authModule, oauth20Conf)).thenReturn(oauth20Map);
        assertSame(oauth20Map, oauth20Conf.map(authModule, mapper));

        OIDCAuthModuleConf oidcConf = new OIDCAuthModuleConf();
        Map<String, Object> oidcMap = Map.of("oidc", oidcConf);
        when(mapper.map(authModule, oidcConf)).thenReturn(oidcMap);
        assertSame(oidcMap, oidcConf.map(authModule, mapper));

        AzureOIDCAuthModuleConf azureConf = new AzureOIDCAuthModuleConf();
        Map<String, Object> azureMap = Map.of("azure-oidc", azureConf);
        when(mapper.map(authModule, azureConf)).thenReturn(azureMap);
        assertSame(azureMap, azureConf.map(authModule, mapper));

        GoogleOIDCAuthModuleConf googleConf = new GoogleOIDCAuthModuleConf();
        Map<String, Object> googleMap = Map.of("google-oidc", googleConf);
        when(mapper.map(authModule, googleConf)).thenReturn(googleMap);
        assertSame(googleMap, googleConf.map(authModule, mapper));

        KeycloakOIDCAuthModuleConf keycloakConf = new KeycloakOIDCAuthModuleConf();
        Map<String, Object> keycloakMap = Map.of("keycloak-oidc", keycloakConf);
        when(mapper.map(authModule, keycloakConf)).thenReturn(keycloakMap);
        assertSame(keycloakMap, keycloakConf.map(authModule, mapper));

        AppleOIDCAuthModuleConf appleConf = new AppleOIDCAuthModuleConf();
        Map<String, Object> appleMap = Map.of("apple-oidc", appleConf);
        when(mapper.map(authModule, appleConf)).thenReturn(appleMap);
        assertSame(appleMap, appleConf.map(authModule, mapper));
    }

    @Test
    void others() {
        LDAPAuthModuleConf ldapConf = new LDAPAuthModuleConf();
        Map<String, Object> ldapMap = Map.of("ldap", ldapConf);
        when(mapper.map(authModule, ldapConf)).thenReturn(ldapMap);
        assertSame(ldapMap, ldapConf.map(authModule, mapper));

        SAML2IdPAuthModuleConf saml2Conf = new SAML2IdPAuthModuleConf();
        Map<String, Object> saml2Map = Map.of("saml2", saml2Conf);
        when(mapper.map(authModule, saml2Conf)).thenReturn(saml2Map);
        assertSame(saml2Map, saml2Conf.map(authModule, mapper));

        X509AuthModuleConf x509Conf = new X509AuthModuleConf();
        Map<String, Object> x509Map = Map.of("x509", x509Conf);
        when(mapper.map(authModule, x509Conf)).thenReturn(x509Map);
        assertSame(x509Map, x509Conf.map(authModule, mapper));

        GoogleMfaAuthModuleConf googleConf = new GoogleMfaAuthModuleConf();
        Map<String, Object> googleMap = Map.of("google-mfa", googleConf);
        when(mapper.map(authModule, googleConf)).thenReturn(googleMap);
        assertSame(googleMap, googleConf.map(authModule, mapper));

        SpnegoAuthModuleConf spnegoConf = new SpnegoAuthModuleConf();
        Map<String, Object> spnegoMap = Map.of("spnego", spnegoConf);
        when(mapper.map(authModule, spnegoConf)).thenReturn(spnegoMap);
        assertSame(spnegoMap, spnegoConf.map(authModule, mapper));
    }

    @Test
    void ldapDependant() {
        assertInstanceOf(GoogleMfaAuthModuleConf.LDAP.class, new GoogleMfaAuthModuleConf().ldapInstance());
        assertInstanceOf(X509AuthModuleConf.LDAP.class, new X509AuthModuleConf().ldapInstance());
        assertInstanceOf(SpnegoAuthModuleConf.LDAP.class, new SpnegoAuthModuleConf().ldapInstance());
    }

    @Test
    void friendlyNames() {
        assertEquals("Google Authenticator", new GoogleMfaAuthModuleConf().getFriendlyName());
        assertEquals("Duo Security", new DuoMfaAuthModuleConf().getFriendlyName());
        assertEquals("CAS Simple Multifactor Authentication", new SimpleMfaAuthModuleConf().getFriendlyName());
    }

    @Test
    void ldapConfiguration() {
        GoogleMfaAuthModuleConf.LDAP googleLdap = new GoogleMfaAuthModuleConf.LDAP();
        googleLdap.setAccountAttributeName("account");
        assertEquals("account", googleLdap.getAccountAttributeName());

        X509AuthModuleConf.LDAP x509Ldap = new X509AuthModuleConf.LDAP();
        x509Ldap.setCertificateAttribute("certificate");
        assertEquals("certificate", x509Ldap.getCertificateAttribute());

        X509AuthModuleConf x509 = new X509AuthModuleConf();
        x509.setName("x509");
        x509.setOrder(1);
        x509.setLdap(x509Ldap);
        assertEquals("x509", x509.getName());
        assertEquals(1, x509.getOrder());
        assertSame(x509Ldap, x509.getLdap());
    }
}
