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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.clientapps.DefaultUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.LogoutType;
import org.apache.syncope.common.lib.types.MetadataCriteriaDirection;
import org.apache.syncope.common.lib.types.OIDCApplicationType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SigningCredentialType;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ClientAppDataBinderTest extends AbstractDataBinderTest {

    private static final String AUTH_POLICY_KEY = "b912a0d4-a890-416f-9ab8-84ab077eb028";

    private static final String ACCESS_POLICY_KEY = "419935c7-deb3-40b3-8a9a-683037e523a2";

    private static final String ATTR_RELEASE_POLICY_KEY = "319935c7-deb3-40b3-8a9a-683037e523a2";

    private static final String ACCOUNT_POLICY_KEY = "20ab5a8c-4b0c-432c-b957-f7fb9784d9f7";

    @Autowired
    private ClientAppDataBinder binder;

    @Autowired
    private SAML2SPClientAppDAO saml2SPClientAppDAO;

    @Autowired
    private OIDCRPClientAppDAO oidcrpClientAppDAO;

    @Autowired
    private CASSPClientAppDAO casspClientAppDAO;

    @Autowired
    private OIDCOpEntityDAO oidcOpEntityDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private EntityFactory entityFactory;

    protected String ticketExpirationPolicy() {
        TicketExpirationPolicy policy = entityFactory.newEntity(TicketExpirationPolicy.class);
        policy.setName("test ticket expiration policy");
        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(600);
        conf.setTgtConf(tgtConf);
        policy.setConf(conf);
        return policyDAO.save(policy).getKey();
    }

    protected SAML2SPClientAppTO saml2spTO(final String name, final long clientAppId) {
        SAML2SPClientAppTO clientAppTO = new SAML2SPClientAppTO();
        clientAppTO.setName(name);
        clientAppTO.setClientAppId(clientAppId);
        clientAppTO.setEntityId(name + "-entity-id");
        clientAppTO.setMetadataLocation("https://idp.example.com/" + name + "/metadata");
        clientAppTO.setMetadataSignatureLocation("https://idp.example.com/" + name + "/signature");
        clientAppTO.setMetadataCriteriaPattern(".*");
        clientAppTO.setMetadataCriteriaDirection(MetadataCriteriaDirection.INCLUDE);
        clientAppTO.setSubjectLocality("localhost");
        clientAppTO.setSigningCredentialType(SigningCredentialType.X509);
        clientAppTO.setLogoutResponseBinding(SAML2BindingType.POST);
        clientAppTO.setSignAssertions(true);
        clientAppTO.setSignResponses(true);
        clientAppTO.setEncryptionOptional(true);
        clientAppTO.setEncryptAssertions(true);
        clientAppTO.setRequireSignedRoot(true);
        clientAppTO.setLogoutResponseEnabled(true);
        clientAppTO.setEncryptAttributes(true);
        clientAppTO.setSkipGeneratingAssertionNameId(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationInResponseTo(true);
        clientAppTO.setSkipGeneratingResponseInResponseTo(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationNotOnOrAfter(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationRecipient(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationAddress(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationNotBefore(true);
        clientAppTO.setSkipGeneratingSubjectConfirmationNameId(true);
        clientAppTO.setSkipGeneratingNameIdQualifiers(true);
        clientAppTO.setSkipGeneratingTransientNameId(true);
        clientAppTO.setSkipValidatingAuthnRequest(true);
        clientAppTO.setSkipGeneratingServiceProviderNameIdQualifier(true);
        clientAppTO.setSkipGeneratingAuthenticatingAuthority(true);
        clientAppTO.setSkipGeneratingNameIdQualifier(true);
        clientAppTO.setSkipGeneratingSessionNotOnOrAfter(true);
        clientAppTO.setValidateMetadataCertificates(true);
        clientAppTO.setRequiredAuthenticationContextClass("urn:test:context");
        clientAppTO.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        clientAppTO.setSkewAllowance("30");
        clientAppTO.setValidityUntil("2030-01-01");
        clientAppTO.setNameIdQualifier("nameIdQualifier");
        clientAppTO.setServiceProviderNameIdQualifier("serviceProviderNameIdQualifier");
        clientAppTO.getAssertionAudiences().add("audience1");
        clientAppTO.getAssertionAudiences().add("audience2");

        clientAppTO.getSigningSignatureAlgorithms().add(XmlSecAlgorithm.AES_128);
        clientAppTO.getSigningSignatureReferenceDigestMethods().add(XmlSecAlgorithm.AES_192);
        clientAppTO.getEncryptionKeyAlgorithms().add(XmlSecAlgorithm.AES_256);
        clientAppTO.getEncryptionDataAlgorithms().add(XmlSecAlgorithm.TRIPLEDES);
        clientAppTO.getSigningSignatureBlackListedAlgorithms().add(XmlSecAlgorithm.DIFFIE_HELLMAN);
        clientAppTO.getEncryptionBlackListedAlgorithms().add(XmlSecAlgorithm.SEED_128);

        return clientAppTO;
    }

    protected OIDCRPClientAppTO oidcrpTO(final String name, final long clientAppId) {
        OIDCRPClientAppTO clientAppTO = new OIDCRPClientAppTO();
        clientAppTO.setName(name);
        clientAppTO.setClientAppId(clientAppId);
        clientAppTO.setClientId(name + "-client-id");
        clientAppTO.setClientSecret("clientSecret");
        clientAppTO.setClientSecretExpiration(1234567L);
        clientAppTO.setIdTokenIssuer("https://issuer.example.com");
        clientAppTO.setSignIdToken(true);
        clientAppTO.setIdTokenSigningAlg(OIDCTokenSigningAlg.RS256);
        clientAppTO.setEncryptIdToken(true);
        clientAppTO.setIdTokenEncryptionAlg(OIDCTokenEncryptionAlg.RSA_OAEP);
        clientAppTO.setIdTokenEncryptionEncoding(OIDCTokenEncryptionEncoding.A128GCM);
        clientAppTO.setUserInfoSigningAlg(OIDCTokenSigningAlg.RS384);
        clientAppTO.setUserInfoEncryptedResponseAlg(OIDCTokenEncryptionAlg.ECDH_ES);
        clientAppTO.setUserInfoEncryptedResponseEncoding(OIDCTokenEncryptionEncoding.A256GCM);
        clientAppTO.setJwtAccessToken(true);
        clientAppTO.setBypassApprovalPrompt(false);
        clientAppTO.setGenerateRefreshToken(false);
        clientAppTO.setSubjectType(OIDCSubjectType.PAIRWISE);
        clientAppTO.setApplicationType(OIDCApplicationType.NATIVE);
        clientAppTO.getRedirectUris().add("https://client.example.com/callback");
        clientAppTO.getSupportedGrantTypes().add(OIDCGrantType.authorization_code);
        clientAppTO.getSupportedGrantTypes().add(OIDCGrantType.refresh_token);
        clientAppTO.getSupportedResponseTypes().add(OIDCResponseType.CODE);
        clientAppTO.getScopes().add("openid");
        clientAppTO.getScopes().add("email");
        clientAppTO.setLogoutUri("https://client.example.com/logout");
        clientAppTO.setJwks("{}");
        clientAppTO.setJwksUri("https://client.example.com/jwks");
        clientAppTO.setTokenEndpointAuthenticationMethod(OIDCClientAuthenticationMethod.client_secret_basic);
        clientAppTO.setAccessTokenMaxActiveTokens(5L);
        clientAppTO.setAccessTokenMaxTimeToLive("PT1H");
        clientAppTO.setAccessTokenTimeToKill("PT30M");
        clientAppTO.setRefreshTokenMaxActiveTokens(3L);
        clientAppTO.setRefreshTokenTimeToKill("PT2H");
        clientAppTO.setDeviceTokenTimeToKill("PT10M");
        clientAppTO.setJwtAccessTokenSigningAlg(OIDCTokenSigningAlg.RS512);
        clientAppTO.setRenewRefreshToken(true);
        clientAppTO.setIdTokenTimeToKill("PT1H");
        clientAppTO.setAuthorizationCodeTimeToLive("PT5M");
        clientAppTO.getAudience().add("audience1");
        clientAppTO.getAudience().add("audience2");
        return clientAppTO;
    }

    @Test
    void createGetUpdateSAML2SP() {
        SAML2SPClientAppTO clientAppTO = saml2spTO("saml2sp", 1L);
        clientAppTO.setRealm("/even/two");
        clientAppTO.setEvaluationOrder(2);
        clientAppTO.setDescription("description");
        clientAppTO.setLogo("logo");
        clientAppTO.setTheme("theme");
        clientAppTO.setInformationUrl("https://example.com/info");
        clientAppTO.setPrivacyUrl("https://example.com/privacy");
        DefaultUsernameAttributeProviderConf usernameConf = new DefaultUsernameAttributeProviderConf();
        clientAppTO.setUsernameAttributeProviderConf(usernameConf);
        clientAppTO.setAuthPolicy(AUTH_POLICY_KEY);
        clientAppTO.setAccessPolicy(ACCESS_POLICY_KEY);
        clientAppTO.setAttrReleasePolicy(ATTR_RELEASE_POLICY_KEY);
        String ticketExpirationPolicy = ticketExpirationPolicy();
        clientAppTO.setTicketExpirationPolicy(ticketExpirationPolicy);
        clientAppTO.getProperties().add(new Attr.Builder("key").value("value").build());
        clientAppTO.setLogoutType(LogoutType.FRONT_CHANNEL);
        clientAppTO.setIdp("test-idp");

        SAML2SPClientApp created = saml2SPClientAppDAO.save(binder.create(clientAppTO));
        entityManager.flush();

        SAML2SPClientAppTO actual = binder.getClientAppTO(
                saml2SPClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals(created.getKey(), actual.getKey());
        assertEquals("/even/two", actual.getRealm());
        assertEquals("saml2sp", actual.getName());
        assertEquals(1L, actual.getClientAppId());
        assertEquals(2, actual.getEvaluationOrder());
        assertEquals("description", actual.getDescription());
        assertEquals("logo", actual.getLogo());
        assertEquals("theme", actual.getTheme());
        assertEquals("https://example.com/info", actual.getInformationUrl());
        assertEquals("https://example.com/privacy", actual.getPrivacyUrl());
        assertEquals(usernameConf, actual.getUsernameAttributeProviderConf());
        assertEquals(AUTH_POLICY_KEY, actual.getAuthPolicy());
        assertEquals(ACCESS_POLICY_KEY, actual.getAccessPolicy());
        assertEquals(ATTR_RELEASE_POLICY_KEY, actual.getAttrReleasePolicy());
        assertEquals(ticketExpirationPolicy, actual.getTicketExpirationPolicy());
        assertEquals(1, actual.getProperties().size());
        assertEquals(LogoutType.FRONT_CHANNEL, actual.getLogoutType());

        assertEquals("test-idp", actual.getIdp());
        assertEquals("saml2sp-entity-id", actual.getEntityId());
        assertEquals("https://idp.example.com/saml2sp/metadata", actual.getMetadataLocation());
        assertEquals("https://idp.example.com/saml2sp/signature", actual.getMetadataSignatureLocation());
        assertEquals(".*", actual.getMetadataCriteriaPattern());
        assertEquals("localhost", actual.getSubjectLocality());
        assertEquals(MetadataCriteriaDirection.INCLUDE, actual.getMetadataCriteriaDirection());
        assertEquals(SigningCredentialType.X509, actual.getSigningCredentialType());
        assertEquals(SAML2BindingType.POST, actual.getLogoutResponseBinding());
        assertTrue(actual.isSignAssertions());
        assertTrue(actual.isSignResponses());
        assertTrue(actual.isEncryptionOptional());
        assertTrue(actual.isEncryptAssertions());
        assertTrue(actual.isRequireSignedRoot());
        assertTrue(actual.isLogoutResponseEnabled());
        assertTrue(actual.isEncryptAttributes());
        assertTrue(actual.isSkipGeneratingAssertionNameId());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationInResponseTo());
        assertTrue(actual.isSkipGeneratingResponseInResponseTo());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationNotOnOrAfter());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationRecipient());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationAddress());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationNotBefore());
        assertTrue(actual.isSkipGeneratingSubjectConfirmationNameId());
        assertTrue(actual.isSkipGeneratingNameIdQualifiers());
        assertTrue(actual.isSkipGeneratingTransientNameId());
        assertTrue(actual.isSkipValidatingAuthnRequest());
        assertTrue(actual.isSkipGeneratingServiceProviderNameIdQualifier());
        assertTrue(actual.isSkipGeneratingAuthenticatingAuthority());
        assertTrue(actual.isSkipGeneratingNameIdQualifier());
        assertTrue(actual.isSkipGeneratingSessionNotOnOrAfter());
        assertTrue(actual.isValidateMetadataCertificates());
        assertEquals("urn:test:context", actual.getRequiredAuthenticationContextClass());
        assertEquals(SAML2SPNameId.EMAIL_ADDRESS, actual.getRequiredNameIdFormat());
        assertEquals("30", actual.getSkewAllowance());
        assertEquals("2030-01-01", actual.getValidityUntil());
        assertEquals("nameIdQualifier", actual.getNameIdQualifier());
        assertEquals("serviceProviderNameIdQualifier", actual.getServiceProviderNameIdQualifier());
        assertEquals(Set.of("audience1", "audience2"), Set.copyOf(actual.getAssertionAudiences()));
        assertEquals(List.of(XmlSecAlgorithm.AES_128), actual.getSigningSignatureAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.AES_192), actual.getSigningSignatureReferenceDigestMethods());
        assertEquals(List.of(XmlSecAlgorithm.AES_256), actual.getEncryptionKeyAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.TRIPLEDES), actual.getEncryptionDataAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.DIFFIE_HELLMAN), actual.getSigningSignatureBlackListedAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.SEED_128), actual.getEncryptionBlackListedAlgorithms());

        actual.setDescription("updated");
        actual.setAuthPolicy(null);
        actual.setLogoutType(LogoutType.NONE);
        actual.setIdp("Syncope");
        binder.update(created, actual);
        saml2SPClientAppDAO.save(created);
        entityManager.flush();

        SAML2SPClientAppTO updated = binder.getClientAppTO(
                saml2SPClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals(created.getKey(), updated.getKey());
        assertEquals("updated", updated.getDescription());
        assertNull(updated.getAuthPolicy());
        assertEquals(LogoutType.NONE, updated.getLogoutType());
        // default owner is not overwritten
        assertEquals("test-idp", updated.getIdp());
    }

    @Test
    void invalidPolicies() {
        SAML2SPClientAppTO auth = saml2spTO("invalid-auth", 10L);
        auth.setAuthPolicy(ACCOUNT_POLICY_KEY);
        assertThrows(SyncopeClientException.class, () -> binder.create(auth));

        SAML2SPClientAppTO access = saml2spTO("invalid-access", 11L);
        access.setAccessPolicy(ACCOUNT_POLICY_KEY);
        assertThrows(SyncopeClientException.class, () -> binder.create(access));

        SAML2SPClientAppTO attrRelease = saml2spTO("invalid-attr-release", 12L);
        attrRelease.setAttrReleasePolicy(ACCOUNT_POLICY_KEY);
        assertThrows(SyncopeClientException.class, () -> binder.create(attrRelease));

        SAML2SPClientAppTO ticket = saml2spTO("invalid-ticket", 13L);
        ticket.setTicketExpirationPolicy(ACCOUNT_POLICY_KEY);
        assertThrows(SyncopeClientException.class, () -> binder.create(ticket));
    }

    @Test
    void createGetUpdateOIDCRP() {
        OIDCOpEntity oidcOpEntity = entityFactory.newEntity(OIDCOpEntity.class);
        oidcOpEntity.setJWKS("jwks".getBytes(StandardCharsets.UTF_8));
        oidcOpEntity.getCustomScopes().put("custom-scope", Set.of("custom"));
        oidcOpEntityDAO.save(oidcOpEntity);
        entityManager.flush();

        OIDCRPClientAppTO clientAppTO = oidcrpTO("oidcrp", 2L);
        clientAppTO.getScopes().add("custom-scope");
        clientAppTO.setRealm("/even");

        OIDCRPClientApp created = oidcrpClientAppDAO.save(binder.create(clientAppTO));
        entityManager.flush();

        OIDCRPClientAppTO actual = binder.getClientAppTO(
                oidcrpClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals(created.getKey(), actual.getKey());
        assertEquals("/even", actual.getRealm());
        assertEquals("oidcrp", actual.getName());
        assertEquals(2L, actual.getClientAppId());
        assertEquals("oidcrp-client-id", actual.getClientId());
        assertEquals("clientSecret", actual.getClientSecret());
        assertEquals(1234567L, actual.getClientSecretExpiration());
        assertEquals("https://issuer.example.com", actual.getIdTokenIssuer());
        assertTrue(actual.isSignIdToken());
        assertEquals(OIDCTokenSigningAlg.RS256, actual.getIdTokenSigningAlg());
        assertTrue(actual.isEncryptIdToken());
        assertEquals(OIDCTokenEncryptionAlg.RSA_OAEP, actual.getIdTokenEncryptionAlg());
        assertEquals(OIDCTokenEncryptionEncoding.A128GCM, actual.getIdTokenEncryptionEncoding());
        assertEquals(OIDCTokenSigningAlg.RS384, actual.getUserInfoSigningAlg());
        assertEquals(OIDCTokenEncryptionAlg.ECDH_ES, actual.getUserInfoEncryptedResponseAlg());
        assertEquals(OIDCTokenEncryptionEncoding.A256GCM, actual.getUserInfoEncryptedResponseEncoding());
        assertTrue(actual.isJwtAccessToken());
        assertFalse(actual.isBypassApprovalPrompt());
        assertFalse(actual.isGenerateRefreshToken());
        assertEquals(OIDCSubjectType.PAIRWISE, actual.getSubjectType());
        assertEquals(OIDCApplicationType.NATIVE, actual.getApplicationType());
        assertEquals(List.of("https://client.example.com/callback"), actual.getRedirectUris());
        assertEquals(
                Set.of(OIDCGrantType.authorization_code, OIDCGrantType.refresh_token),
                Set.copyOf(actual.getSupportedGrantTypes()));
        assertEquals(List.of(OIDCResponseType.CODE), actual.getSupportedResponseTypes());
        assertEquals(Set.of("openid", "email", "custom-scope"), Set.copyOf(actual.getScopes()));
        assertEquals("https://client.example.com/logout", actual.getLogoutUri());
        assertEquals("{}", actual.getJwks());
        assertEquals("https://client.example.com/jwks", actual.getJwksUri());
        assertEquals(OIDCClientAuthenticationMethod.client_secret_basic,
                actual.getTokenEndpointAuthenticationMethod());
        assertEquals(5L, actual.getAccessTokenMaxActiveTokens());
        assertEquals("PT1H", actual.getAccessTokenMaxTimeToLive());
        assertEquals("PT30M", actual.getAccessTokenTimeToKill());
        assertEquals(3L, actual.getRefreshTokenMaxActiveTokens());
        assertEquals("PT2H", actual.getRefreshTokenTimeToKill());
        assertEquals("PT10M", actual.getDeviceTokenTimeToKill());
        assertEquals(OIDCTokenSigningAlg.RS512, actual.getJwtAccessTokenSigningAlg());
        assertTrue(actual.getRenewRefreshToken());
        assertEquals("PT1H", actual.getIdTokenTimeToKill());
        assertEquals("PT5M", actual.getAuthorizationCodeTimeToLive());
        assertEquals(Set.of("audience1", "audience2"), Set.copyOf(actual.getAudience()));

        actual.getScopes().clear();
        actual.getScopes().add("profile");
        actual.setClientSecret(null);
        actual.setRealm(null);
        binder.update(created, actual);
        oidcrpClientAppDAO.save(created);
        entityManager.flush();

        OIDCRPClientAppTO updated = binder.getClientAppTO(
                oidcrpClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals(Set.of("profile"), Set.copyOf(updated.getScopes()));
        assertNull(updated.getClientSecret());
        assertNull(updated.getRealm());
    }

    @Test
    void undefinedOIDCScope() {
        OIDCRPClientAppTO clientAppTO = oidcrpTO("invalid-scope", 3L);
        clientAppTO.getScopes().add("not-a-scope");

        assertThrows(SyncopeClientException.class, () -> binder.create(clientAppTO));
    }

    @Test
    void createGetUpdateCASSP() {
        CASSPClientAppTO clientAppTO = new CASSPClientAppTO();
        clientAppTO.setName("cassp");
        clientAppTO.setClientAppId(4L);
        clientAppTO.setServiceId("https://service.example.com");
        clientAppTO.setRealm("/even/two");
        clientAppTO.setAuthPolicy(AUTH_POLICY_KEY);

        CASSPClientApp created = casspClientAppDAO.save(binder.create(clientAppTO));
        entityManager.flush();

        CASSPClientAppTO actual = binder.getClientAppTO(
                casspClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals(created.getKey(), actual.getKey());
        assertEquals("cassp", actual.getName());
        assertEquals(4L, actual.getClientAppId());
        assertEquals("https://service.example.com", actual.getServiceId());
        assertEquals("/even/two", actual.getRealm());
        assertEquals(AUTH_POLICY_KEY, actual.getAuthPolicy());

        actual.setServiceId("https://service2.example.com");
        binder.update(created, actual);
        casspClientAppDAO.save(created);
        entityManager.flush();

        CASSPClientAppTO updated = binder.getClientAppTO(
                casspClientAppDAO.findById(created.getKey()).orElseThrow());
        assertEquals("https://service2.example.com", updated.getServiceId());
    }

    @Test
    void unsupportedClientApp() {
        ClientAppTO unsupportedTO = new ClientAppTO() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getDiscriminator() {
                return "unsupported";
            }
        };

        assertThrows(IllegalArgumentException.class, () -> binder.create(unsupportedTO));

        ClientApp clientApp = mock(ClientApp.class);
        assertThrows(IllegalArgumentException.class, () -> binder.update(clientApp, unsupportedTO));
        assertThrows(IllegalArgumentException.class, () -> binder.getClientAppTO(clientApp));
    }

    @Test
    void saml2spWithoutIdp() {
        SAML2SPClientAppTO clientAppTO = saml2spTO("saml2sp-no-idp", 5L);
        clientAppTO.setIdp(null);

        SAML2SPClientApp created = saml2SPClientAppDAO.save(binder.create(clientAppTO));
        entityManager.flush();

        SAML2SPClientAppTO actual = binder.getClientAppTO(
                saml2SPClientAppDAO.findById(created.getKey()).orElseThrow());
        assertNull(actual.getIdp());
        assertNotNull(actual.getKey());

        // default owner is not stored
        actual.setIdp("Syncope");
        binder.update(created, actual);
        saml2SPClientAppDAO.save(created);
        entityManager.flush();

        assertNull(binder.<SAML2SPClientAppTO>getClientAppTO(
                saml2SPClientAppDAO.findById(created.getKey()).orElseThrow()).getIdp());
    }
}
