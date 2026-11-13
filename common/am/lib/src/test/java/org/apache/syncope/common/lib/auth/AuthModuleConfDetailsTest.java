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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.TriStateBoolean;
import org.apache.syncope.common.lib.types.X509PolicySetting;
import org.apache.syncope.common.lib.types.X509PrincipalType;
import org.apache.syncope.common.lib.types.X509RevocationCheckerType;
import org.apache.syncope.common.lib.types.X509RevocationFetcherType;
import org.apache.syncope.common.lib.types.X509SubjectDnFormat;
import org.junit.jupiter.api.Test;

class AuthModuleConfDetailsTest {

    @Test
    void x509() {
        X509AuthModuleConf conf = new X509AuthModuleConf();
        X509AuthModuleConf.LDAP ldap = new X509AuthModuleConf.LDAP();
        ldap.setCertificateAttribute("certificate");

        conf.setName("name");
        conf.setOrder(1);
        conf.setRevocationPolicyThreshold(2);
        conf.setCheckAll(true);
        conf.setRefreshIntervalSeconds(3);
        conf.setThrowOnFetchFailure(true);
        conf.setPrincipalType(X509PrincipalType.SUBJECT_ALT_NAME);
        conf.setPrincipalAlternateAttribute("alternate");
        conf.setPrincipalTypeSubjectDnFormat(X509SubjectDnFormat.RFC2253);
        conf.setPrincipalTypeSerialNoDnSerialNumberPrefix("prefix");
        conf.setPrincipalTypeSerialNoDnValueDelimiter("delimiter");
        conf.setPrincipalTypeSerialNoSNRadix(16);
        conf.setPrincipalTypeSerialNoHexSNZeroPadding(true);
        conf.setRevocationChecker(X509RevocationCheckerType.CRL);
        conf.setCrlFetcher(X509RevocationFetcherType.LDAP);
        conf.getCrlResources().add("resource");
        conf.setCacheMaxElementsInMemory(4);
        conf.setMixedMode(false);
        conf.setCacheTimeToLiveSeconds("PT1H");
        conf.setCrlResourceUnavailablePolicy(X509PolicySetting.ALLOW);
        conf.setCrlResourceExpiredPolicy(X509PolicySetting.THRESHOLD);
        conf.setCrlUnavailablePolicy(X509PolicySetting.ALLOW);
        conf.setCrlExpiredPolicy(X509PolicySetting.THRESHOLD);
        conf.setRegExTrustedIssuerDnPattern("trusted");
        conf.setMaxPathLength(5);
        conf.setMaxPathLengthAllowUnspecified(true);
        conf.setCheckKeyUsage(true);
        conf.setRequireKeyUsage(true);
        conf.setRegExSubjectDnPattern("subject");
        conf.setExtractCert(true);
        conf.setSslHeaderName("header");
        conf.setLdap(ldap);

        assertEquals("name", conf.getName());
        assertEquals(1, conf.getOrder());
        assertEquals(2, conf.getRevocationPolicyThreshold());
        assertTrue(conf.isCheckAll());
        assertEquals(3, conf.getRefreshIntervalSeconds());
        assertTrue(conf.isThrowOnFetchFailure());
        assertEquals(X509PrincipalType.SUBJECT_ALT_NAME, conf.getPrincipalType());
        assertEquals("alternate", conf.getPrincipalAlternateAttribute());
        assertEquals(X509SubjectDnFormat.RFC2253, conf.getPrincipalTypeSubjectDnFormat());
        assertEquals("prefix", conf.getPrincipalTypeSerialNoDnSerialNumberPrefix());
        assertEquals("delimiter", conf.getPrincipalTypeSerialNoDnValueDelimiter());
        assertEquals(16, conf.getPrincipalTypeSerialNoSNRadix());
        assertTrue(conf.isPrincipalTypeSerialNoHexSNZeroPadding());
        assertEquals(X509RevocationCheckerType.CRL, conf.getRevocationChecker());
        assertEquals(X509RevocationFetcherType.LDAP, conf.getCrlFetcher());
        assertEquals(List.of("resource"), conf.getCrlResources());
        assertEquals(4, conf.getCacheMaxElementsInMemory());
        assertFalse(conf.isMixedMode());
        assertEquals("PT1H", conf.getCacheTimeToLiveSeconds());
        assertEquals(X509PolicySetting.ALLOW, conf.getCrlResourceUnavailablePolicy());
        assertEquals(X509PolicySetting.THRESHOLD, conf.getCrlResourceExpiredPolicy());
        assertEquals(X509PolicySetting.ALLOW, conf.getCrlUnavailablePolicy());
        assertEquals(X509PolicySetting.THRESHOLD, conf.getCrlExpiredPolicy());
        assertEquals("trusted", conf.getRegExTrustedIssuerDnPattern());
        assertEquals(5, conf.getMaxPathLength());
        assertTrue(conf.isMaxPathLengthAllowUnspecified());
        assertTrue(conf.isCheckKeyUsage());
        assertTrue(conf.isRequireKeyUsage());
        assertEquals("subject", conf.getRegExSubjectDnPattern());
        assertTrue(conf.isExtractCert());
        assertEquals("header", conf.getSslHeaderName());
        assertSame(ldap, conf.getLdap());
        assertEquals("certificate", ldap.getCertificateAttribute());
    }

    @Test
    void saml2IdP() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();

        conf.setUserIdAttribute("userIdAttribute");
        conf.setDestinationBinding(SAML2BindingType.POST);
        conf.setKeystorePassword("keystorePassword");
        conf.setPrivateKeyPassword("privateKeyPassword");
        conf.setKeystore("keystore");
        conf.setServiceProviderMetadata("serviceProviderMetadata");
        conf.setIdentityProviderMetadataPath("identityProviderMetadataPath");
        conf.setMaximumAuthenticationLifetime("PT1H");
        conf.setAcceptedSkew("PT1M");
        conf.setServiceProviderEntityId("serviceProviderEntityId");
        conf.setForceAuth(true);
        conf.setPassive(true);
        conf.setNameIdPolicyAllowCreate(TriStateBoolean.FALSE);
        conf.getAuthnContextClassRefs().add("authnContextClassRef");
        conf.setAuthnContextComparisonType("exact");
        conf.setNameIdPolicyFormat("nameIdPolicyFormat");
        conf.setResponsesSigned(true);
        conf.setWantsAssertionsSigned(true);
        conf.setAttributeConsumingServiceIndex(1);
        conf.setAssertionConsumerServiceIndex(2);
        conf.setUseNameQualifier(false);
        conf.setSignServiceProviderMetadata(true);
        conf.setSignAuthnRequest(true);
        conf.setSignServiceProviderLogoutRequest(true);
        conf.getBlockedSignatureSigningAlgorithms().add("blocked");
        conf.getSignatureAlgorithms().add("signature");
        conf.getSignatureReferenceDigestMethods().add("digest");
        conf.setSignatureCanonicalizationAlgorithm("canonicalization");
        conf.setProviderName("providerName");

        assertEquals("userIdAttribute", conf.getUserIdAttribute());
        assertEquals(SAML2BindingType.POST, conf.getDestinationBinding());
        assertEquals("keystorePassword", conf.getKeystorePassword());
        assertEquals("privateKeyPassword", conf.getPrivateKeyPassword());
        assertEquals("keystore", conf.getKeystore());
        assertEquals("serviceProviderMetadata", conf.getServiceProviderMetadata());
        assertEquals("identityProviderMetadataPath", conf.getIdentityProviderMetadataPath());
        assertEquals("PT1H", conf.getMaximumAuthenticationLifetime());
        assertEquals("PT1M", conf.getAcceptedSkew());
        assertEquals("serviceProviderEntityId", conf.getServiceProviderEntityId());
        assertTrue(conf.isForceAuth());
        assertTrue(conf.isPassive());
        assertEquals(TriStateBoolean.FALSE, conf.getNameIdPolicyAllowCreate());
        assertEquals(List.of("authnContextClassRef"), conf.getAuthnContextClassRefs());
        assertEquals("exact", conf.getAuthnContextComparisonType());
        assertEquals("nameIdPolicyFormat", conf.getNameIdPolicyFormat());
        assertTrue(conf.isResponsesSigned());
        assertTrue(conf.isWantsAssertionsSigned());
        assertEquals(1, conf.getAttributeConsumingServiceIndex());
        assertEquals(2, conf.getAssertionConsumerServiceIndex());
        assertFalse(conf.isUseNameQualifier());
        assertTrue(conf.isSignServiceProviderMetadata());
        assertTrue(conf.isSignAuthnRequest());
        assertTrue(conf.isSignServiceProviderLogoutRequest());
        assertEquals(List.of("blocked"), conf.getBlockedSignatureSigningAlgorithms());
        assertEquals(List.of("signature"), conf.getSignatureAlgorithms());
        assertEquals(List.of("digest"), conf.getSignatureReferenceDigestMethods());
        assertEquals("canonicalization", conf.getSignatureCanonicalizationAlgorithm());
        assertEquals("providerName", conf.getProviderName());
    }

    @Test
    void spnego() {
        SpnegoAuthModuleConf conf = new SpnegoAuthModuleConf();
        SpnegoAuthModuleConf.LDAP ldap = new SpnegoAuthModuleConf.LDAP();

        conf.setLoginConf("loginConf");
        conf.setKerberosConf("kerberosConf");
        conf.setKerberosKdc("kerberosKdc");
        conf.setJcifsServicePrincipal("jcifsServicePrincipal");
        conf.setKerberosRealm("kerberosRealm");
        conf.setKerberosDebug(true);
        conf.setUseSubjectCredsOnly(true);
        conf.setPrincipalWithDomainName(true);
        conf.setNtlmAllowed(false);
        conf.setSend401OnAuthenticationFailure(false);
        conf.setHostNameClientActionStrategy("hostNameClientActionStrategy");
        conf.setDnsTimeout("PT1S");
        conf.setHostNamePatternString("hostNamePattern");
        conf.setIpsToCheckPattern("ips");
        conf.setAlternativeRemoteHostAttribute("alternative");
        conf.setSpnegoAttributeName("spnego");
        conf.setMixedModeAuthentication(true);
        conf.setSupportedBrowsers("browsers");
        conf.setPoolSize(2);
        conf.setPoolTimeout("PT2S");
        conf.setAttributeRepoId("attributeRepoId");
        conf.setLdap(ldap);

        assertEquals("loginConf", conf.getLoginConf());
        assertEquals("kerberosConf", conf.getKerberosConf());
        assertEquals("kerberosKdc", conf.getKerberosKdc());
        assertEquals("jcifsServicePrincipal", conf.getJcifsServicePrincipal());
        assertEquals("kerberosRealm", conf.getKerberosRealm());
        assertTrue(conf.isKerberosDebug());
        assertTrue(conf.isUseSubjectCredsOnly());
        assertTrue(conf.isPrincipalWithDomainName());
        assertFalse(conf.isNtlmAllowed());
        assertFalse(conf.isSend401OnAuthenticationFailure());
        assertEquals("hostNameClientActionStrategy", conf.getHostNameClientActionStrategy());
        assertEquals("PT1S", conf.getDnsTimeout());
        assertEquals("hostNamePattern", conf.getHostNamePatternString());
        assertEquals("ips", conf.getIpsToCheckPattern());
        assertEquals("alternative", conf.getAlternativeRemoteHostAttribute());
        assertEquals("spnego", conf.getSpnegoAttributeName());
        assertTrue(conf.isMixedModeAuthentication());
        assertEquals("browsers", conf.getSupportedBrowsers());
        assertEquals(2, conf.getPoolSize());
        assertEquals("PT2S", conf.getPoolTimeout());
        assertEquals("attributeRepoId", conf.getAttributeRepoId());
        assertSame(ldap, conf.getLdap());
    }

    @Test
    void ldap() {
        LDAPAuthModuleConf conf = new LDAPAuthModuleConf();

        conf.setAuthenticationType(LDAPAuthModuleConf.AuthenticationType.DIRECT);
        conf.setDnFormat("dnFormat");
        conf.setEnhanceWithEntryResolver(false);
        conf.setDerefAliases(LDAPAuthModuleConf.DerefAliasesType.ALWAYS);
        conf.setResolveFromAttribute("resolveFromAttribute");
        conf.setPrincipalAttributeId("principalAttributeId");
        conf.setPrincipalDnAttributeName("principalDnAttributeName");
        conf.setAllowMultiplePrincipalAttributeValues(true);
        conf.getAdditionalAttributes().add("additional");
        conf.setAllowMissingPrincipalAttributeValue(false);
        conf.setCollectDnAttribute(true);
        conf.setCredentialCriteria("credentialCriteria");

        assertEquals(LDAPAuthModuleConf.AuthenticationType.DIRECT, conf.getAuthenticationType());
        assertEquals("dnFormat", conf.getDnFormat());
        assertFalse(conf.isEnhanceWithEntryResolver());
        assertEquals(LDAPAuthModuleConf.DerefAliasesType.ALWAYS, conf.getDerefAliases());
        assertEquals("resolveFromAttribute", conf.getResolveFromAttribute());
        assertEquals("principalAttributeId", conf.getPrincipalAttributeId());
        assertEquals("principalDnAttributeName", conf.getPrincipalDnAttributeName());
        assertTrue(conf.isAllowMultiplePrincipalAttributeValues());
        assertEquals(List.of("additional"), conf.getAdditionalAttributes());
        assertFalse(conf.isAllowMissingPrincipalAttributeValue());
        assertTrue(conf.isCollectDnAttribute());
        assertEquals("credentialCriteria", conf.getCredentialCriteria());
    }

    @Test
    void googleMfa() {
        GoogleMfaAuthModuleConf conf = new GoogleMfaAuthModuleConf();
        GoogleMfaAuthModuleConf.LDAP ldap = new GoogleMfaAuthModuleConf.LDAP();

        conf.setIssuer("issuer");
        conf.setLabel("label");
        conf.setCodeDigits(8);
        conf.setTimeStepSize(60L);
        conf.setWindowSize(4);
        conf.setEnableCrypto(false);
        conf.setCryptoAlgorithm(OIDCTokenEncryptionEncoding.A128GCM);
        conf.setCryptoStrategy(GoogleMfaAuthModuleConf.CryptoStrategy.SIGN_AND_ENCRYPT);
        conf.setSigningKeySize(1024);
        conf.setSigningKey("signingKey");
        conf.setEncryptionKeySize(2048);
        conf.setEncryptionKey("encryptionKey");
        conf.setLdap(ldap);

        assertEquals("issuer", conf.getIssuer());
        assertEquals("label", conf.getLabel());
        assertEquals(8, conf.getCodeDigits());
        assertEquals(60L, conf.getTimeStepSize());
        assertEquals(4, conf.getWindowSize());
        assertFalse(conf.isEnableCrypto());
        assertEquals(OIDCTokenEncryptionEncoding.A128GCM, conf.getCryptoAlgorithm());
        assertEquals(GoogleMfaAuthModuleConf.CryptoStrategy.SIGN_AND_ENCRYPT, conf.getCryptoStrategy());
        assertEquals(1024, conf.getSigningKeySize());
        assertEquals("signingKey", conf.getSigningKey());
        assertEquals(2048, conf.getEncryptionKeySize());
        assertEquals("encryptionKey", conf.getEncryptionKey());
        assertSame(ldap, conf.getLdap());
    }

    @Test
    void smallerConfs() {
        SimpleMfaAuthModuleConf simple = new SimpleMfaAuthModuleConf();
        simple.setTimeToKillInSeconds(10L);
        simple.setTokenLength(8);
        simple.setBypassGroovyScript("script");
        simple.setEmailAttribute("emailAttribute");
        simple.setEmailFrom("emailFrom");
        simple.setEmailSubject("emailSubject");
        simple.setEmailText("emailText");
        assertEquals(10L, simple.getTimeToKillInSeconds());
        assertEquals(8, simple.getTokenLength());
        assertEquals("script", simple.getBypassGroovyScript());
        assertEquals("emailAttribute", simple.getEmailAttribute());
        assertEquals("emailFrom", simple.getEmailFrom());
        assertEquals("emailSubject", simple.getEmailSubject());
        assertEquals("emailText", simple.getEmailText());

        JaasAuthModuleConf jaas = new JaasAuthModuleConf();
        jaas.setRealm("realm");
        jaas.setKerberosRealmSystemProperty("realmProperty");
        jaas.setKerberosKdcSystemProperty("kdcProperty");
        jaas.setLoginConfigType("configType");
        jaas.setLoginConfigurationFile("configurationFile");
        jaas.setCredentialCriteria("credentialCriteria");
        assertEquals("realm", jaas.getRealm());
        assertEquals("realmProperty", jaas.getKerberosRealmSystemProperty());
        assertEquals("kdcProperty", jaas.getKerberosKdcSystemProperty());
        assertEquals("configType", jaas.getLoginConfigType());
        assertEquals("configurationFile", jaas.getLoginConfigurationFile());
        assertEquals("credentialCriteria", jaas.getCredentialCriteria());

        OAuth20AuthModuleConf oauth20 = new OAuth20AuthModuleConf();
        oauth20.setAuthUrl("authUrl");
        oauth20.setProfileAttrs(Map.of("key", "value"));
        oauth20.setWithState(true);
        oauth20.setProfileUrl("profileUrl");
        oauth20.setProfileVerb("GET");
        assertEquals("authUrl", oauth20.getAuthUrl());
        assertEquals("value", oauth20.getProfileAttrs().get("key"));
        assertTrue(oauth20.isWithState());
        assertEquals("profileUrl", oauth20.getProfileUrl());
        assertEquals("GET", oauth20.getProfileVerb());

        AppleOIDCAuthModuleConf apple = new AppleOIDCAuthModuleConf();
        apple.setTimeout("PT1S");
        apple.setPrivateKey("privateKey");
        apple.setPrivateKeyId("privateKeyId");
        apple.setTeamId("teamId");
        assertEquals("PT1S", apple.getTimeout());
        assertEquals("privateKey", apple.getPrivateKey());
        assertEquals("privateKeyId", apple.getPrivateKeyId());
        assertEquals("teamId", apple.getTeamId());

        JDBCAuthModuleConf jdbc = new JDBCAuthModuleConf();
        jdbc.setFieldPassword("password");
        jdbc.setFieldExpired("expired");
        jdbc.setFieldDisabled("disabled");
        jdbc.setCredentialCriteria("credentialCriteria");
        assertEquals("password", jdbc.getFieldPassword());
        assertEquals("expired", jdbc.getFieldExpired());
        assertEquals("disabled", jdbc.getFieldDisabled());
        assertEquals("credentialCriteria", jdbc.getCredentialCriteria());

        SyncopeAuthModuleConf syncope = new SyncopeAuthModuleConf();
        syncope.setDomain("domain");
        syncope.setCredentialCriteria("credentialCriteria");
        syncope.setMaxRetryAttempts(2);
        assertEquals("domain", syncope.getDomain());
        assertEquals("credentialCriteria", syncope.getCredentialCriteria());
        assertEquals(2, syncope.getMaxRetryAttempts());

        DuoMfaAuthModuleConf duo = new DuoMfaAuthModuleConf();
        duo.setIntegrationKey("integrationKey");
        duo.setSecretKey("secretKey");
        duo.setApiHost("apiHost");
        assertEquals("integrationKey", duo.getIntegrationKey());
        assertEquals("secretKey", duo.getSecretKey());
        assertEquals("apiHost", duo.getApiHost());

        OktaAuthModuleConf okta = new OktaAuthModuleConf();
        okta.setCredentialCriteria("credentialCriteria");
        assertEquals("credentialCriteria", okta.getCredentialCriteria());
    }
}
