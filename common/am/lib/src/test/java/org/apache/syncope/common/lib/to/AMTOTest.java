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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.password.SyncopePasswordManagementConf;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.junit.jupiter.api.Test;

class AMTOTest {

    @Test
    void authModuleTO() {
        StaticAuthModuleConf conf = new StaticAuthModuleConf();
        AuthModuleTO module = new AuthModuleTO();
        module.setKey("key");
        module.setDescription("description");
        module.setState(AuthModuleState.STANDBY);
        module.setOrder(3);
        module.setConf(conf);
        Item item = new Item();
        item.setIntAttrName("username");
        module.getItems().add(item);

        assertEquals("key", module.getKey());
        assertEquals("description", module.getDescription());
        assertEquals(AuthModuleState.STANDBY, module.getState());
        assertEquals(3, module.getOrder());
        assertSame(conf, module.getConf());
        assertEquals(1, module.getItems().size());

        AuthModuleTO same = new AuthModuleTO();
        same.setKey("key");
        same.setDescription("description");
        same.setState(AuthModuleState.STANDBY);
        same.setOrder(3);
        same.setConf(conf);
        same.getItems().add(item);

        AuthModuleTO other = new AuthModuleTO();
        other.setKey("other");

        assertEquals(module, module);
        assertNotEquals(module, null);
        assertNotEquals(module, "string");
        assertEquals(module, same);
        assertEquals(module.hashCode(), same.hashCode());
        assertNotEquals(module, other);
    }

    @Test
    void attrRepoTO() {
        StubAttrRepoConf conf = new StubAttrRepoConf();
        AttrRepoTO attrRepo = new AttrRepoTO();
        attrRepo.setKey("key");
        attrRepo.setDescription("description");
        attrRepo.setState(AttrRepoState.DISABLED);
        attrRepo.setOrder(3);
        attrRepo.setConf(conf);

        assertEquals("key", attrRepo.getKey());
        assertEquals("description", attrRepo.getDescription());
        assertEquals(AttrRepoState.DISABLED, attrRepo.getState());
        assertEquals(3, attrRepo.getOrder());
        assertSame(conf, attrRepo.getConf());

        AttrRepoTO same = new AttrRepoTO();
        same.setKey("key");
        same.setDescription("description");
        same.setState(AttrRepoState.DISABLED);
        same.setOrder(3);
        same.setConf(conf);

        AttrRepoTO other = new AttrRepoTO();
        other.setKey("other");

        assertEquals(attrRepo, attrRepo);
        assertNotEquals(attrRepo, null);
        assertNotEquals(attrRepo, "string");
        assertEquals(attrRepo, same);
        assertEquals(attrRepo.hashCode(), same.hashCode());
        assertNotEquals(attrRepo, other);
    }

    @Test
    void passwordManagementTO() {
        SyncopePasswordManagementConf conf = new SyncopePasswordManagementConf();
        PasswordManagementTO passwordManagement = new PasswordManagementTO();
        passwordManagement.setKey("key");
        passwordManagement.setDescription("description");
        passwordManagement.setEnabled(true);
        passwordManagement.setConf(conf);

        assertEquals("key", passwordManagement.getKey());
        assertEquals("description", passwordManagement.getDescription());
        assertTrue(passwordManagement.isEnabled());
        assertSame(conf, passwordManagement.getConf());

        PasswordManagementTO same = new PasswordManagementTO();
        same.setKey("key");
        same.setDescription("description");
        same.setEnabled(true);
        same.setConf(conf);

        PasswordManagementTO other = new PasswordManagementTO();
        other.setKey("other");

        assertEquals(passwordManagement, passwordManagement);
        assertNotEquals(passwordManagement, null);
        assertNotEquals(passwordManagement, "string");
        assertEquals(passwordManagement, same);
        assertEquals(passwordManagement.hashCode(), same.hashCode());
        assertNotEquals(passwordManagement, other);
    }

    @Test
    void authProfileTO() {
        LocalDateTime createdDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        AuthProfileTO profile = new AuthProfileTO.Builder().
                key("key").
                owner("owner").
                impersonationAccount(new ImpersonationAccount.Builder().impersonated("one").build()).
                impersonationAccounts(new ImpersonationAccount.Builder().impersonated("two").build()).
                impersonationAccounts(List.of(new ImpersonationAccount.Builder().impersonated("three").build())).
                googleMfaAuthToken(new GoogleMfaAuthToken.Builder().token(1).build()).
                googleMfaAuthTokens(new GoogleMfaAuthToken.Builder().token(2).build()).
                googleMfaAuthTokens(List.of(new GoogleMfaAuthToken.Builder().token(3).build())).
                googleMfaAuthAccount(new GoogleMfaAuthAccount.Builder().name("one").build()).
                googleMfaAuthAccounts(new GoogleMfaAuthAccount.Builder().name("two").build()).
                googleMfaAuthAccounts(List.of(new GoogleMfaAuthAccount.Builder().name("three").build())).
                mfaTrustedDevice(new MfaTrustedDevice()).
                mfaTrustedDevices(new MfaTrustedDevice()).
                mfaTrustedDevices(List.of(new MfaTrustedDevice())).
                webAuthnDeviceCredential(new WebAuthnDeviceCredential.Builder().identifier("one").build()).
                webAuthnDeviceCredentials(new WebAuthnDeviceCredential.Builder().identifier("two").build()).
                webAuthnDeviceCredentials(
                        List.of(new WebAuthnDeviceCredential.Builder().identifier("three").build())).
                consentDecision(new WAConsentDecision.Builder(1L, "principal", "service", createdDate).build()).
                consentDecisions(new WAConsentDecision.Builder(2L, "principal", "service", createdDate).build()).
                consentDecisions(
                        List.of(new WAConsentDecision.Builder(3L, "principal", "service", createdDate).build())).
                build();

        assertEquals("key", profile.getKey());
        assertEquals("owner", profile.getOwner());
        assertEquals("owner", profile.getName());
        assertThrows(UnsupportedOperationException.class, () -> profile.setName("name"));
        assertEquals(3, profile.getImpersonationAccounts().size());
        assertEquals(3, profile.getGoogleMfaAuthTokens().size());
        assertEquals(3, profile.getGoogleMfaAuthAccounts().size());
        assertEquals(3, profile.getMfaTrustedDevices().size());
        assertEquals(3, profile.getWebAuthnDeviceCredentials().size());
        assertEquals(3, profile.getConsentDecisions().size());

        AuthProfileTO same = new AuthProfileTO();
        same.setKey("key");
        same.setOwner("owner");
        same.getImpersonationAccounts().addAll(profile.getImpersonationAccounts());
        same.getGoogleMfaAuthTokens().addAll(profile.getGoogleMfaAuthTokens());
        same.getGoogleMfaAuthAccounts().addAll(profile.getGoogleMfaAuthAccounts());
        same.getMfaTrustedDevices().addAll(profile.getMfaTrustedDevices());
        same.getWebAuthnDeviceCredentials().addAll(profile.getWebAuthnDeviceCredentials());
        same.getConsentDecisions().addAll(profile.getConsentDecisions());

        AuthProfileTO other = new AuthProfileTO();
        other.setKey("other");
        other.setOwner("owner");

        assertEquals(profile, profile);
        assertNotEquals(profile, null);
        assertNotEquals(profile, "string");
        assertEquals(profile, same);
        assertEquals(profile.hashCode(), same.hashCode());
        assertNotEquals(profile, other);
    }

    @Test
    void oidcOpEntityTO() {
        OIDCOpEntityTO entity = new OIDCOpEntityTO();
        entity.setKey("key");
        entity.setJWKS("jwks");
        entity.getCustomScopes().put("scope", Set.of("value"));

        assertEquals("key", entity.getKey());
        assertEquals("jwks", entity.getJWKS());
        assertEquals(Set.of("value"), entity.getCustomScopes().get("scope"));
        assertTrue(entity.toString().contains("jwks"));

        OIDCOpEntityTO same = new OIDCOpEntityTO();
        same.setKey("key");
        same.setJWKS("jwks");
        same.getCustomScopes().put("scope", Set.of("value"));

        OIDCOpEntityTO other = new OIDCOpEntityTO();
        other.setKey("other");

        assertEquals(entity, entity);
        assertNotEquals(entity, null);
        assertNotEquals(entity, "string");
        assertNotEquals(entity, same);
        assertNotEquals(entity, other);
    }

    @Test
    void saml2IdPEntityTO() {
        SAML2IdPEntityTO entity = new SAML2IdPEntityTO.Builder().
                key("key").
                metadata("metadata").
                signingCertificate("signingCertificate").
                signingKey("signingKey").
                encryptionCertificate("encryptionCertificate").
                encryptionKey("encryptionKey").
                build();

        assertEquals("key", entity.getKey());
        assertEquals("metadata", entity.getMetadata());
        assertEquals("signingCertificate", entity.getSigningCertificate());
        assertEquals("signingKey", entity.getSigningKey());
        assertEquals("encryptionCertificate", entity.getEncryptionCertificate());
        assertEquals("encryptionKey", entity.getEncryptionKey());
        assertTrue(entity.toString().contains("key"));

        SAML2IdPEntityTO same = new SAML2IdPEntityTO.Builder().
                key("key").
                metadata("metadata").
                signingCertificate("signingCertificate").
                signingKey("signingKey").
                encryptionCertificate("encryptionCertificate").
                encryptionKey("encryptionKey").
                build();

        SAML2IdPEntityTO other = new SAML2IdPEntityTO.Builder().
                key("other").
                build();

        assertEquals(entity, entity);
        assertNotEquals(entity, null);
        assertNotEquals(entity, "string");
        assertEquals(entity, same);
        assertEquals(entity.hashCode(), same.hashCode());
        assertNotEquals(entity, other);
    }
}
