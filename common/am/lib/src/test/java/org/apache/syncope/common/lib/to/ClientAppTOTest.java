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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.clientapps.AnonymousUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
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
import org.junit.jupiter.api.Test;

class ClientAppTOTest {

    private void fill(final ClientAppTO clientApp) {
        UsernameAttributeProviderConf provider = new AnonymousUsernameAttributeProviderConf();
        clientApp.setKey("key");
        clientApp.setRealm("realm");
        clientApp.setName("name");
        clientApp.setClientAppId(1L);
        clientApp.setEvaluationOrder(2);
        clientApp.setDescription("description");
        clientApp.setLogo("logo");
        clientApp.setTheme("theme");
        clientApp.setInformationUrl("informationUrl");
        clientApp.setPrivacyUrl("privacyUrl");
        clientApp.setUsernameAttributeProviderConf(provider);
        clientApp.setAuthPolicy("authPolicy");
        clientApp.setAccessPolicy("accessPolicy");
        clientApp.setAttrReleasePolicy("attrReleasePolicy");
        clientApp.setTicketExpirationPolicy("ticketExpirationPolicy");
        clientApp.setLogoutType(LogoutType.FRONT_CHANNEL);
        Attr property = new Attr();
        property.setSchema("schema");
        clientApp.getProperties().add(property);
    }

    private void assertFilled(final ClientAppTO clientApp) {
        assertEquals("key", clientApp.getKey());
        assertEquals("realm", clientApp.getRealm());
        assertEquals("name", clientApp.getName());
        assertEquals(1L, clientApp.getClientAppId());
        assertEquals(2, clientApp.getEvaluationOrder());
        assertEquals("description", clientApp.getDescription());
        assertEquals("logo", clientApp.getLogo());
        assertEquals("theme", clientApp.getTheme());
        assertEquals("informationUrl", clientApp.getInformationUrl());
        assertEquals("privacyUrl", clientApp.getPrivacyUrl());
        assertTrue(clientApp.getUsernameAttributeProviderConf() instanceof AnonymousUsernameAttributeProviderConf);
        assertEquals("authPolicy", clientApp.getAuthPolicy());
        assertEquals("accessPolicy", clientApp.getAccessPolicy());
        assertEquals("attrReleasePolicy", clientApp.getAttrReleasePolicy());
        assertEquals("ticketExpirationPolicy", clientApp.getTicketExpirationPolicy());
        assertEquals(1, clientApp.getProperties().size());
        assertEquals(LogoutType.FRONT_CHANNEL, clientApp.getLogoutType());
        assertEquals(clientApp.getClass().getName(), clientApp.getDiscriminator());
    }

    @Test
    void casSP() {
        CASSPClientAppTO clientApp = new CASSPClientAppTO();
        fill(clientApp);
        clientApp.setServiceId("serviceId");
        clientApp.setDiscriminator("ignored");

        assertFilled(clientApp);
        assertEquals("serviceId", clientApp.getServiceId());
        assertEquals(clientApp, clientApp);
        assertNotEquals(clientApp, null);
        assertNotEquals(clientApp, "string");
        assertNotEquals(clientApp, new SAML2SPClientAppTO());
        assertTrue(clientApp.hashCode() != 0);
    }

    @Test
    void oidcRP() {
        OIDCRPClientAppTO clientApp = new OIDCRPClientAppTO();
        fill(clientApp);

        clientApp.setClientId("clientId");
        clientApp.setClientSecret("clientSecret");
        clientApp.setClientSecretExpiration(1L);
        clientApp.setIdTokenIssuer("issuer");
        clientApp.setSignIdToken(false);
        clientApp.setIdTokenSigningAlg(OIDCTokenSigningAlg.RS256);
        clientApp.setEncryptIdToken(true);
        clientApp.setIdTokenEncryptionAlg(OIDCTokenEncryptionAlg.RSA_OAEP);
        clientApp.setIdTokenEncryptionEncoding(OIDCTokenEncryptionEncoding.A256CBC_HS512);
        clientApp.setUserInfoSigningAlg(OIDCTokenSigningAlg.ES256);
        clientApp.setUserInfoEncryptedResponseAlg(OIDCTokenEncryptionAlg.A128KW);
        clientApp.setUserInfoEncryptedResponseEncoding(OIDCTokenEncryptionEncoding.A128GCM);
        clientApp.setJwtAccessToken(true);
        clientApp.setBypassApprovalPrompt(false);
        clientApp.setGenerateRefreshToken(false);
        clientApp.setSubjectType(OIDCSubjectType.PAIRWISE);
        clientApp.setApplicationType(OIDCApplicationType.NATIVE);
        clientApp.setJwks("jwks");
        clientApp.setJwksUri("jwksUri");
        clientApp.setTokenEndpointAuthenticationMethod(OIDCClientAuthenticationMethod.private_key_jwt);
        clientApp.setLogoutUri("logoutUri");
        clientApp.setAccessTokenMaxTimeToLive("accessTokenMaxTimeToLive");
        clientApp.setAccessTokenTimeToKill("accessTokenTimeToKill");
        clientApp.setAccessTokenMaxActiveTokens(3L);
        clientApp.setRefreshTokenTimeToKill("refreshTokenTimeToKill");
        clientApp.setRefreshTokenMaxActiveTokens(4L);
        clientApp.setDeviceTokenTimeToKill("deviceTokenTimeToKill");
        clientApp.setJwtAccessTokenSigningAlg(OIDCTokenSigningAlg.HS256);
        clientApp.setRenewRefreshToken(true);
        clientApp.setIdTokenTimeToKill("idTokenTimeToKill");
        clientApp.setAuthorizationCodeTimeToLive("authorizationCodeTimeToLive");
        clientApp.getRedirectUris().add("redirectUri");
        clientApp.getSupportedGrantTypes().add(OIDCGrantType.authorization_code);
        clientApp.getSupportedResponseTypes().add(OIDCResponseType.CODE);
        clientApp.getScopes().add("openid");
        clientApp.getAudience().add("audience");

        assertFilled(clientApp);
        assertEquals("clientId", clientApp.getClientId());
        assertEquals("clientSecret", clientApp.getClientSecret());
        assertEquals(1L, clientApp.getClientSecretExpiration());
        assertEquals("issuer", clientApp.getIdTokenIssuer());
        assertFalse(clientApp.isSignIdToken());
        assertEquals(OIDCTokenSigningAlg.RS256, clientApp.getIdTokenSigningAlg());
        assertTrue(clientApp.isEncryptIdToken());
        assertEquals(OIDCTokenEncryptionAlg.RSA_OAEP, clientApp.getIdTokenEncryptionAlg());
        assertEquals(OIDCTokenEncryptionEncoding.A256CBC_HS512, clientApp.getIdTokenEncryptionEncoding());
        assertEquals(OIDCTokenSigningAlg.ES256, clientApp.getUserInfoSigningAlg());
        assertEquals(OIDCTokenEncryptionAlg.A128KW, clientApp.getUserInfoEncryptedResponseAlg());
        assertEquals(OIDCTokenEncryptionEncoding.A128GCM, clientApp.getUserInfoEncryptedResponseEncoding());
        assertTrue(clientApp.isJwtAccessToken());
        assertFalse(clientApp.isBypassApprovalPrompt());
        assertFalse(clientApp.isGenerateRefreshToken());
        assertEquals(OIDCSubjectType.PAIRWISE, clientApp.getSubjectType());
        assertEquals(OIDCApplicationType.NATIVE, clientApp.getApplicationType());
        assertEquals("jwks", clientApp.getJwks());
        assertEquals("jwksUri", clientApp.getJwksUri());
        assertEquals(OIDCClientAuthenticationMethod.private_key_jwt,
                clientApp.getTokenEndpointAuthenticationMethod());
        assertEquals("logoutUri", clientApp.getLogoutUri());
        assertEquals("accessTokenMaxTimeToLive", clientApp.getAccessTokenMaxTimeToLive());
        assertEquals("accessTokenTimeToKill", clientApp.getAccessTokenTimeToKill());
        assertEquals(3L, clientApp.getAccessTokenMaxActiveTokens());
        assertEquals("refreshTokenTimeToKill", clientApp.getRefreshTokenTimeToKill());
        assertEquals(4L, clientApp.getRefreshTokenMaxActiveTokens());
        assertEquals("deviceTokenTimeToKill", clientApp.getDeviceTokenTimeToKill());
        assertEquals(OIDCTokenSigningAlg.HS256, clientApp.getJwtAccessTokenSigningAlg());
        assertTrue(clientApp.getRenewRefreshToken());
        assertEquals("idTokenTimeToKill", clientApp.getIdTokenTimeToKill());
        assertEquals("authorizationCodeTimeToLive", clientApp.getAuthorizationCodeTimeToLive());
        assertEquals(List.of("redirectUri"), clientApp.getRedirectUris());
        assertEquals(List.of(OIDCGrantType.authorization_code), clientApp.getSupportedGrantTypes());
        assertEquals(List.of(OIDCResponseType.CODE), clientApp.getSupportedResponseTypes());
        assertEquals(List.of("openid"), clientApp.getScopes());
        assertEquals(List.of("audience"), clientApp.getAudience());

        assertEquals(clientApp, clientApp);
        assertNotEquals(clientApp, null);
        assertNotEquals(clientApp, "string");
        assertNotEquals(clientApp, new SAML2SPClientAppTO());
        assertTrue(clientApp.hashCode() != 0);
    }

    @Test
    void saml2SP() {
        SAML2SPClientAppTO clientApp = new SAML2SPClientAppTO();
        fill(clientApp);

        clientApp.setEntityId("entityId");
        clientApp.setIdp("idp");
        clientApp.setMetadataLocation("metadataLocation");
        clientApp.setMetadataSignatureLocation("metadataSignatureLocation");
        clientApp.setMetadataCriteriaPattern("metadataCriteriaPattern");
        clientApp.setMetadataCriteriaDirection(MetadataCriteriaDirection.EXCLUDE);
        clientApp.setSubjectLocality("subjectLocality");
        clientApp.setSigningCredentialType(SigningCredentialType.BASIC);
        clientApp.setLogoutResponseBinding(SAML2BindingType.POST);
        clientApp.setSignAssertions(true);
        clientApp.setSignResponses(true);
        clientApp.setEncryptionOptional(true);
        clientApp.setEncryptAssertions(true);
        clientApp.setRequireSignedRoot(false);
        clientApp.setLogoutResponseEnabled(false);
        clientApp.setEncryptAttributes(true);
        clientApp.setSkipGeneratingAssertionNameId(true);
        clientApp.setSkipGeneratingSubjectConfirmationInResponseTo(true);
        clientApp.setSkipGeneratingResponseInResponseTo(true);
        clientApp.setSkipGeneratingSubjectConfirmationNotOnOrAfter(true);
        clientApp.setSkipGeneratingSubjectConfirmationRecipient(true);
        clientApp.setSkipGeneratingSubjectConfirmationAddress(true);
        clientApp.setSkipGeneratingSubjectConfirmationNotBefore(false);
        clientApp.setSkipGeneratingSubjectConfirmationNameId(false);
        clientApp.setSkipGeneratingNameIdQualifiers(true);
        clientApp.setSkipGeneratingTransientNameId(true);
        clientApp.setSkipValidatingAuthnRequest(true);
        clientApp.setSkipGeneratingServiceProviderNameIdQualifier(true);
        clientApp.setSkipGeneratingAuthenticatingAuthority(true);
        clientApp.setSkipGeneratingNameIdQualifier(true);
        clientApp.setSkipGeneratingSessionNotOnOrAfter(true);
        clientApp.setValidateMetadataCertificates(true);
        clientApp.setRequiredAuthenticationContextClass("requiredAuthenticationContextClass");
        clientApp.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        clientApp.setSkewAllowance("skewAllowance");
        clientApp.setValidityUntil("validityUntil");
        clientApp.setNameIdQualifier("nameIdQualifier");
        clientApp.setServiceProviderNameIdQualifier("serviceProviderNameIdQualifier");
        clientApp.getAssertionAudiences().add("audience");
        clientApp.getSigningSignatureAlgorithms().add(XmlSecAlgorithm.SHA256);
        clientApp.getSigningSignatureReferenceDigestMethods().add(XmlSecAlgorithm.SHA1);
        clientApp.getEncryptionDataAlgorithms().add(XmlSecAlgorithm.AES_128_GCM);
        clientApp.getEncryptionKeyAlgorithms().add(XmlSecAlgorithm.AES_128_KeyWrap);
        clientApp.getSigningSignatureBlackListedAlgorithms().add(XmlSecAlgorithm.SHA512);
        clientApp.getEncryptionBlackListedAlgorithms().add(XmlSecAlgorithm.AES_256_GCM);

        assertFilled(clientApp);
        assertEquals("entityId", clientApp.getEntityId());
        assertEquals("idp", clientApp.getIdp());
        assertEquals("metadataLocation", clientApp.getMetadataLocation());
        assertEquals("metadataSignatureLocation", clientApp.getMetadataSignatureLocation());
        assertEquals("metadataCriteriaPattern", clientApp.getMetadataCriteriaPattern());
        assertEquals(MetadataCriteriaDirection.EXCLUDE, clientApp.getMetadataCriteriaDirection());
        assertEquals("subjectLocality", clientApp.getSubjectLocality());
        assertEquals(SigningCredentialType.BASIC, clientApp.getSigningCredentialType());
        assertEquals(SAML2BindingType.POST, clientApp.getLogoutResponseBinding());
        assertTrue(clientApp.isSignAssertions());
        assertTrue(clientApp.isSignResponses());
        assertTrue(clientApp.isEncryptionOptional());
        assertTrue(clientApp.isEncryptAssertions());
        assertFalse(clientApp.isRequireSignedRoot());
        assertFalse(clientApp.isLogoutResponseEnabled());
        assertTrue(clientApp.isEncryptAttributes());
        assertTrue(clientApp.isSkipGeneratingAssertionNameId());
        assertTrue(clientApp.isSkipGeneratingSubjectConfirmationInResponseTo());
        assertTrue(clientApp.isSkipGeneratingResponseInResponseTo());
        assertTrue(clientApp.isSkipGeneratingSubjectConfirmationNotOnOrAfter());
        assertTrue(clientApp.isSkipGeneratingSubjectConfirmationRecipient());
        assertTrue(clientApp.isSkipGeneratingSubjectConfirmationAddress());
        assertFalse(clientApp.isSkipGeneratingSubjectConfirmationNotBefore());
        assertFalse(clientApp.isSkipGeneratingSubjectConfirmationNameId());
        assertTrue(clientApp.isSkipGeneratingNameIdQualifiers());
        assertTrue(clientApp.isSkipGeneratingTransientNameId());
        assertTrue(clientApp.isSkipValidatingAuthnRequest());
        assertTrue(clientApp.isSkipGeneratingServiceProviderNameIdQualifier());
        assertTrue(clientApp.isSkipGeneratingAuthenticatingAuthority());
        assertTrue(clientApp.isSkipGeneratingNameIdQualifier());
        assertTrue(clientApp.isSkipGeneratingSessionNotOnOrAfter());
        assertTrue(clientApp.isValidateMetadataCertificates());
        assertEquals("requiredAuthenticationContextClass", clientApp.getRequiredAuthenticationContextClass());
        assertEquals(SAML2SPNameId.EMAIL_ADDRESS, clientApp.getRequiredNameIdFormat());
        assertEquals("skewAllowance", clientApp.getSkewAllowance());
        assertEquals("validityUntil", clientApp.getValidityUntil());
        assertEquals("nameIdQualifier", clientApp.getNameIdQualifier());
        assertEquals("serviceProviderNameIdQualifier", clientApp.getServiceProviderNameIdQualifier());
        assertEquals(List.of("audience"), clientApp.getAssertionAudiences());
        assertEquals(List.of(XmlSecAlgorithm.SHA256), clientApp.getSigningSignatureAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.SHA1), clientApp.getSigningSignatureReferenceDigestMethods());
        assertEquals(List.of(XmlSecAlgorithm.AES_128_GCM), clientApp.getEncryptionDataAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.AES_128_KeyWrap), clientApp.getEncryptionKeyAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.SHA512), clientApp.getSigningSignatureBlackListedAlgorithms());
        assertEquals(List.of(XmlSecAlgorithm.AES_256_GCM), clientApp.getEncryptionBlackListedAlgorithms());

        assertEquals(clientApp, clientApp);
        assertNotEquals(clientApp, null);
        assertNotEquals(clientApp, "string");
        assertNotEquals(clientApp, new OIDCRPClientAppTO());
        assertTrue(clientApp.hashCode() != 0);
    }

    @Test
    void propertyHolder() {
        ClientAppTO clientApp = new CASSPClientAppTO();
        Attr attr = new Attr();
        attr.setSchema("schema");
        attr.getValues().add("value");
        clientApp.getProperties().add(attr);

        assertSame(attr, clientApp.getProperties().get(0));
        assertEquals("value", clientApp.getProperties().get(0).getValues().get(0));
    }
}
