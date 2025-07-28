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
package org.apache.syncope.wa.bootstrap.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.auth.AbstractOIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.AppleOIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.AzureActiveDirectoryAuthModuleConf;
import org.apache.syncope.common.lib.auth.AzureOIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.GoogleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.GoogleOIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.JDBCAuthModuleConf;
import org.apache.syncope.common.lib.auth.JaasAuthModuleConf;
import org.apache.syncope.common.lib.auth.KeycloakOIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OAuth20AuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.OktaAuthModuleConf;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.common.lib.auth.SimpleMfaAuthModuleConf;
import org.apache.syncope.common.lib.auth.SpnegoAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.auth.U2FAuthModuleConf;
import org.apache.syncope.common.lib.auth.X509AuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationHandlerStates;
import org.apereo.cas.configuration.model.support.azuread.AzureActiveDirectoryAuthenticationProperties;
import org.apereo.cas.configuration.model.support.generic.AcceptAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jaas.JaasAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.authn.QueryJdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapAuthenticationProperties.AuthenticationTypes;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.DuoSecurityMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.GoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.LdapGoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.simple.CasSimpleMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.okta.OktaAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.oauth.Pac4jOAuth20ClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.BasePac4jOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jAppleOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jAzureOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jGenericOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jGoogleOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jKeyCloakOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.saml.Pac4jSamlClientProperties;
import org.apereo.cas.configuration.model.support.spnego.SpnegoAuthenticationProperties;
import org.apereo.cas.configuration.model.support.spnego.SpnegoLdapProperties;
import org.apereo.cas.configuration.model.support.spnego.SpnegoProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopeAuthenticationProperties;
import org.apereo.cas.configuration.model.support.x509.SubjectDnPrincipalResolverProperties.SubjectDnFormat;
import org.apereo.cas.configuration.model.support.x509.X509LdapProperties;
import org.apereo.cas.configuration.model.support.x509.X509Properties;
import org.apereo.cas.configuration.model.support.x509.X509Properties.PrincipalTypes;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.model.TriStateBoolean;

public class AuthModulePropertySourceMapper extends PropertySourceMapper implements AuthModuleConf.Mapper {

    protected final WARestClient waRestClient;

    public AuthModulePropertySourceMapper(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final StaticAuthModuleConf conf) {
        AcceptAuthenticationProperties props = new AcceptAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setOrder(authModuleTO.getOrder());
        props.setCredentialCriteria(conf.getCredentialCriteria());
        String users = conf.getUsers().entrySet().stream().
                map(entry -> entry.getKey() + "::" + entry.getValue()).
                collect(Collectors.joining(","));
        props.setUsers(users);

        return prefix("cas.authn.accept.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final LDAPAuthModuleConf conf) {
        LdapAuthenticationProperties props = new LdapAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setOrder(authModuleTO.getOrder());

        props.setType(AuthenticationTypes.valueOf(conf.getAuthenticationType().name()));
        props.setDnFormat(conf.getDnFormat());
        props.setEnhanceWithEntryResolver(conf.isEnhanceWithEntryResolver());
        props.setDerefAliases(Optional.ofNullable(conf.getDerefAliases()).
                map(LDAPAuthModuleConf.DerefAliasesType::name).orElse(null));
        props.setResolveFromAttribute(conf.getResolveFromAttribute());

        props.setPrincipalAttributeId(conf.getPrincipalAttributeId());
        props.setPrincipalDnAttributeName(conf.getPrincipalDnAttributeName());
        props.setPrincipalAttributeList(authModuleTO.getItems().stream().
                map(item -> item.getIntAttrName() + ":" + item.getExtAttrName()).collect(Collectors.toList()));
        props.setAllowMultiplePrincipalAttributeValues(conf.isAllowMultiplePrincipalAttributeValues());
        props.setAdditionalAttributes(conf.getAdditionalAttributes());
        props.setAllowMissingPrincipalAttributeValue(conf.isAllowMissingPrincipalAttributeValue());
        props.setCollectDnAttribute(conf.isCollectDnAttribute());
        props.setCredentialCriteria(conf.getCredentialCriteria());
        props.getPasswordPolicy().setType(AbstractLdapProperties.LdapType.valueOf(conf.getLdapType().name()));

        fill(props, conf);

        return prefix("cas.authn.ldap[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final JDBCAuthModuleConf conf) {
        QueryJdbcAuthenticationProperties props = new QueryJdbcAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setOrder(authModuleTO.getOrder());
        props.setSql(conf.getSql());
        props.setFieldDisabled(conf.getFieldDisabled());
        props.setFieldExpired(conf.getFieldExpired());
        props.setFieldPassword(conf.getFieldPassword());
        props.setPrincipalAttributeList(authModuleTO.getItems().stream().
                map(item -> item.getIntAttrName() + ":" + item.getExtAttrName()).collect(Collectors.toList()));
        props.setCredentialCriteria(conf.getCredentialCriteria());
        fill(props, conf);

        return prefix("cas.authn.jdbc.query[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final JaasAuthModuleConf conf) {
        JaasAuthenticationProperties props = new JaasAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setOrder(authModuleTO.getOrder());
        props.setLoginConfigType(conf.getLoginConfigType());
        props.setKerberosKdcSystemProperty(conf.getKerberosKdcSystemProperty());
        props.setKerberosRealmSystemProperty(conf.getKerberosRealmSystemProperty());
        props.setLoginConfigType(conf.getLoginConfigurationFile());
        props.setRealm(conf.getRealm());
        props.setCredentialCriteria(conf.getCredentialCriteria());

        return prefix("cas.authn.jaas[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final OAuth20AuthModuleConf conf) {
        Pac4jOAuth20ClientProperties props = new Pac4jOAuth20ClientProperties();
        props.setId(conf.getClientId());
        props.setSecret(conf.getClientSecret());
        props.setClientName(Optional.ofNullable(conf.getClientName()).orElse(authModuleTO.getKey()));
        props.setEnabled(authModuleTO.getState() == AuthModuleState.ACTIVE);
        props.setCustomParams(conf.getCustomParams());
        props.setAuthUrl(conf.getAuthUrl());
        props.setProfilePath(conf.getProfilePath());
        props.setProfileVerb(conf.getProfileVerb());
        props.setProfileUrl(conf.getProfileUrl());
        props.setTokenUrl(conf.getTokenUrl());
        props.setResponseType(conf.getResponseType());
        props.setScope(conf.getScope());
        props.setPrincipalAttributeId(conf.getUserIdAttribute());
        props.setWithState(conf.isWithState());
        props.setProfileAttrs(authModuleTO.getItems().stream().
                collect(Collectors.toMap(Item::getIntAttrName, Item::getExtAttrName)));

        return prefix("cas.authn.pac4j.oauth2[].", CasCoreConfigurationUtils.asMap(props));
    }

    protected void map(
            final AuthModuleTO authModuleTO,
            final BasePac4jOidcClientProperties props,
            final AbstractOIDCAuthModuleConf conf) {

        props.setId(conf.getClientId());
        props.setSecret(conf.getClientSecret());
        props.setClientName(Optional.ofNullable(conf.getClientName()).orElse(authModuleTO.getKey()));
        props.setEnabled(authModuleTO.getState() == AuthModuleState.ACTIVE);
        props.setCustomParams(conf.getCustomParams());
        props.setDiscoveryUri(conf.getDiscoveryUri());
        props.setMaxClockSkew(conf.getMaxClockSkew());
        props.setPreferredJwsAlgorithm(conf.getPreferredJwsAlgorithm());
        props.setResponseMode(conf.getResponseMode());
        props.setResponseType(conf.getResponseType());
        props.setScope(conf.getScope());
        props.setPrincipalAttributeId(conf.getUserIdAttribute());
        props.setExpireSessionWithToken(conf.isExpireSessionWithToken());
        props.setTokenExpirationAdvance(conf.getTokenExpirationAdvance());
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final OIDCAuthModuleConf conf) {
        Pac4jGenericOidcClientProperties props = new Pac4jGenericOidcClientProperties();
        map(authModuleTO, props, conf);

        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setGeneric(props);

        return prefix("cas.authn.pac4j.oidc[].generic.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final AzureOIDCAuthModuleConf conf) {
        Pac4jAzureOidcClientProperties props = new Pac4jAzureOidcClientProperties();
        map(authModuleTO, props, conf);
        props.setTenant(conf.getTenant());

        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setAzure(props);

        return prefix("cas.authn.pac4j.oidc[].azure.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final GoogleOIDCAuthModuleConf conf) {
        Pac4jGoogleOidcClientProperties props = new Pac4jGoogleOidcClientProperties();
        map(authModuleTO, props, conf);

        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setGoogle(props);

        return prefix("cas.authn.pac4j.oidc[].google.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final KeycloakOIDCAuthModuleConf conf) {
        Pac4jKeyCloakOidcClientProperties props = new Pac4jKeyCloakOidcClientProperties();
        map(authModuleTO, props, conf);
        props.setRealm(conf.getRealm());
        props.setBaseUri(conf.getBaseUri());

        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setKeycloak(props);

        return prefix("cas.authn.pac4j.oidc[].keycloak.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final AppleOIDCAuthModuleConf conf) {
        Pac4jAppleOidcClientProperties props = new Pac4jAppleOidcClientProperties();
        map(authModuleTO, props, conf);
        props.setTimeout(conf.getTimeout());
        props.setPrivateKey(conf.getPrivateKey());
        props.setPrivateKeyId(conf.getPrivateKeyId());
        props.setTeamId(conf.getTeamId());

        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setApple(props);

        return prefix("cas.authn.pac4j.oidc[].apple.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final SAML2IdPAuthModuleConf conf) {
        Pac4jSamlClientProperties props = new Pac4jSamlClientProperties();
        props.setClientName(Optional.ofNullable(conf.getClientName()).orElse(authModuleTO.getKey()));
        props.setEnabled(authModuleTO.getState() == AuthModuleState.ACTIVE);
        props.setAcceptedSkew(conf.getAcceptedSkew());
        props.setAssertionConsumerServiceIndex(conf.getAssertionConsumerServiceIndex());
        props.setAttributeConsumingServiceIndex(conf.getAttributeConsumingServiceIndex());
        props.setAuthnContextClassRef(conf.getAuthnContextClassRefs());
        props.setAuthnContextComparisonType(conf.getAuthnContextComparisonType());
        props.setBlockedSignatureSigningAlgorithms(conf.getBlockedSignatureSigningAlgorithms());
        props.setDestinationBinding(conf.getDestinationBinding().getUri());
        props.setIdentityProviderMetadataPath(conf.getIdentityProviderMetadataPath());
        props.setServiceProviderMetadataPath(conf.getServiceProviderMetadataPath());
        props.setKeystorePath(conf.getKeystorePath());
        props.setWantsAssertionsSigned(conf.isWantsAssertionsSigned());
        props.setWantsResponsesSigned(conf.isResponsesSigned());
        props.setKeystoreAlias(conf.getKeystoreAlias());
        props.setKeystorePassword(conf.getKeystorePassword());
        props.setMaximumAuthenticationLifetime(conf.getMaximumAuthenticationLifetime());
        props.setNameIdPolicyFormat(conf.getNameIdPolicyFormat());
        props.setPrivateKeyPassword(conf.getPrivateKeyPassword());
        props.setProviderName(conf.getProviderName());
        props.setServiceProviderEntityId(conf.getServiceProviderEntityId());
        props.setSignatureAlgorithms(conf.getSignatureAlgorithms());
        props.setSignatureCanonicalizationAlgorithm(conf.getSignatureCanonicalizationAlgorithm());
        props.setSignatureReferenceDigestMethods(conf.getSignatureReferenceDigestMethods());
        props.setPrincipalAttributeId(conf.getUserIdAttribute());
        props.setNameIdPolicyAllowCreate(StringUtils.isBlank(conf.getNameIdPolicyAllowCreate())
                ? TriStateBoolean.UNDEFINED
                : TriStateBoolean.valueOf(conf.getNameIdPolicyAllowCreate().toUpperCase()));

        props.setMappedAttributes(authModuleTO.getItems().stream().
                map(item -> item.getIntAttrName() + "->" + item.getExtAttrName()).
                collect(Collectors.toList()));

        return prefix("cas.authn.pac4j.saml[].", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final X509AuthModuleConf conf) {
        X509Properties props = new X509Properties();
        props.setName(conf.getName());
        props.setOrder(conf.getOrder());
        props.setCacheDiskOverflow(conf.isCacheDiskOverflow());
        props.setCacheDiskSize(conf.getCacheDiskSize());
        props.setCacheEternal(conf.isCacheEternal());
        props.setCacheMaxElementsInMemory(conf.getCacheMaxElementsInMemory());
        props.setCacheTimeToLiveSeconds(conf.getCacheTimeToLiveSeconds());
        props.setCheckAll(conf.isCheckAll());
        props.setCheckKeyUsage(conf.isCheckKeyUsage());
        props.setCrlExpiredPolicy(conf.getCrlExpiredPolicy().name());
        props.setCrlFetcher(conf.getCrlFetcher().name());
        props.setCrlResourceExpiredPolicy(conf.getCrlResourceExpiredPolicy().name());
        props.setCrlResourceUnavailablePolicy(conf.getCrlResourceUnavailablePolicy().name());
        props.setCrlResources(conf.getCrlResources());
        props.setCrlUnavailablePolicy(conf.getCrlUnavailablePolicy().name());
        props.setExtractCert(conf.isExtractCert());
        props.setMaxPathLength(conf.getMaxPathLength());
        props.setMaxPathLengthAllowUnspecified(conf.isMaxPathLengthAllowUnspecified());
        props.setMixedMode(conf.isMixedMode());
        props.setRefreshIntervalSeconds(conf.getRefreshIntervalSeconds());
        props.setRegExSubjectDnPattern(conf.getRegExSubjectDnPattern());
        props.setRegExTrustedIssuerDnPattern(conf.getRegExTrustedIssuerDnPattern());
        props.setRequireKeyUsage(conf.isRequireKeyUsage());
        props.setRevocationChecker(conf.getRevocationChecker().name());
        props.setRevocationPolicyThreshold(conf.getRevocationPolicyThreshold());
        props.setSslHeaderName(conf.getSslHeaderName());
        props.setThrowOnFetchFailure(conf.isThrowOnFetchFailure());

        props.setPrincipalType(PrincipalTypes.valueOf(conf.getPrincipalType().name()));
        if (StringUtils.isNotBlank(conf.getPrincipalAlternateAttribute())) {
            switch (props.getPrincipalType()) {
                case CN_EDIPI:
                    props.getCnEdipi().setAlternatePrincipalAttribute(conf.getPrincipalAlternateAttribute());
                    break;

                case RFC822_EMAIL:
                    props.getRfc822Email().setAlternatePrincipalAttribute(conf.getPrincipalAlternateAttribute());
                    break;

                case SUBJECT:
                    props.setPrincipalDescriptor(conf.getPrincipalAlternateAttribute());
                    break;

                case SUBJECT_ALT_NAME:
                    props.getSubjectAltName().setAlternatePrincipalAttribute(conf.getPrincipalAlternateAttribute());
                    break;

                case SUBJECT_DN:
                case SERIAL_NO_DN:
                case SERIAL_NO:
                default:
            }
        }
        props.getSubjectDn().setFormat(SubjectDnFormat.valueOf(conf.getPrincipalTypeSubjectDnFormat().name()));
        props.getSerialNoDn().setSerialNumberPrefix(conf.getPrincipalTypeSerialNoDnSerialNumberPrefix());
        props.getSerialNoDn().setValueDelimiter(conf.getPrincipalTypeSerialNoDnValueDelimiter());
        props.getSerialNo().setPrincipalHexSNZeroPadding(conf.isPrincipalTypeSerialNoHexSNZeroPadding());
        props.getSerialNo().setPrincipalSNRadix(conf.getPrincipalTypeSerialNoSNRadix());

        if (conf.getLdap() != null) {
            X509LdapProperties ldapProps = new X509LdapProperties();
            ldapProps.setCertificateAttribute(conf.getLdap().getCertificateAttribute());
            fill(ldapProps, conf.getLdap());
            props.setLdap(ldapProps);
        }

        return prefix("cas.authn.x509.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final SyncopeAuthModuleConf conf) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return Map.of();
        }

        SyncopeAuthenticationProperties props = new SyncopeAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(syncopeClient.getAddress(), "/rest"));
        props.setAttributeMappings(authModuleTO.getItems().stream().
                collect(Collectors.toMap(Item::getIntAttrName, Item::getExtAttrName)));
        props.setCredentialCriteria(conf.getCredentialCriteria());

        return prefix("cas.authn.syncope.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final AzureActiveDirectoryAuthModuleConf conf) {
        AzureActiveDirectoryAuthenticationProperties props = new AzureActiveDirectoryAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setClientId(conf.getClientId());
        props.setLoginUrl(conf.getLoginUrl());
        props.setResource(conf.getResource());
        props.setCredentialCriteria(conf.getCredentialCriteria());

        return prefix("cas.authn.azure-active-directory.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final OktaAuthModuleConf conf) {
        OktaAuthenticationProperties props = new OktaAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());
        props.setState(AuthenticationHandlerStates.valueOf(authModuleTO.getState().name()));
        props.setOrganizationUrl(conf.getOrganizationUrl());
        props.setCredentialCriteria(conf.getCredentialCriteria());

        return prefix("cas.authn.okta.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final GoogleMfaAuthModuleConf conf) {
        GoogleAuthenticatorMultifactorProperties props = new GoogleAuthenticatorMultifactorProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());

        props.getCore().setIssuer(conf.getIssuer());
        props.getCore().setCodeDigits(conf.getCodeDigits());
        props.getCore().setLabel(conf.getLabel());
        props.getCore().setTimeStepSize(conf.getTimeStepSize());
        props.getCore().setWindowSize(conf.getWindowSize());

        props.getCrypto().setEnabled(conf.isEnableCrypto());
        props.getCrypto().setAlg(conf.getCryptoAlgorithm().getExternalForm());
        props.getCrypto().setStrategyType(conf.getCryptoStrategy().name());
        props.getCrypto().getEncryption().setKeySize(conf.getEncryptionKeySize());
        props.getCrypto().getEncryption().setKey(conf.getEncryptionKey());
        props.getCrypto().getSigning().setKeySize(conf.getSigningKeySize());
        props.getCrypto().getSigning().setKey(conf.getSigningKey());

        if (conf.getLdap() != null) {
            LdapGoogleAuthenticatorMultifactorProperties ldapProps = new LdapGoogleAuthenticatorMultifactorProperties();
            ldapProps.setAccountAttributeName(conf.getLdap().getAccountAttributeName());
            fill(ldapProps, conf.getLdap());
            props.setLdap(ldapProps);
        }

        return prefix("cas.authn.mfa.gauth.", CasCoreConfigurationUtils.asMap(props));
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final DuoMfaAuthModuleConf conf) {
        DuoSecurityMultifactorAuthenticationProperties props = new DuoSecurityMultifactorAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());
        props.setDuoApiHost(conf.getApiHost());
        props.setDuoApplicationKey(conf.getApplicationKey());
        props.setDuoIntegrationKey(conf.getIntegrationKey());
        props.setDuoSecretKey(conf.getSecretKey());

        return prefix("cas.authn.mfa.duo.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final U2FAuthModuleConf conf) {
        U2FMultifactorAuthenticationProperties props = new U2FMultifactorAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());
        props.getCore().setExpireDevices(conf.getExpireDevices());
        props.getCore().setExpireDevicesTimeUnit(TimeUnit.valueOf(conf.getExpireDevicesTimeUnit()));
        props.getCore().setExpireRegistrations(conf.getExpireRegistrations());
        props.getCore().setExpireRegistrationsTimeUnit(TimeUnit.valueOf(conf.getExpireRegistrationsTimeUnit()));

        return prefix("cas.authn.mfa.u2f.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final SimpleMfaAuthModuleConf conf) {
        CasSimpleMultifactorAuthenticationProperties props = new CasSimpleMultifactorAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());

        props.getMail().setAttributeName(conf.getEmailAttribute());
        props.getMail().setFrom(conf.getEmailFrom());
        props.getMail().setSubject(conf.getEmailSubject());
        props.getMail().setText(conf.getEmailText());

        props.getToken().getCore().setTokenLength(conf.getTokenLength());
        props.getToken().getCore().setTimeToKillInSeconds(conf.getTimeToKillInSeconds());

        if (StringUtils.isNotBlank(conf.getBypassGroovyScript())) {
            try {
                props.getBypass().getGroovy().setLocation(ResourceUtils.getResourceFrom(conf.getBypassGroovyScript()));
            } catch (Exception e) {
                LOG.error("Unable to load groovy script for bypass", e);
                throw new IllegalArgumentException(e);
            }
        }

        return prefix("cas.authn.mfa.simple.", CasCoreConfigurationUtils.asMap(props));
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModuleTO, final SpnegoAuthModuleConf conf) {
        SpnegoProperties props = new SpnegoProperties();
        props.setName(authModuleTO.getKey());
        props.setOrder(authModuleTO.getOrder());

        SpnegoAuthenticationProperties jcifsProperties = new SpnegoAuthenticationProperties();
        jcifsProperties.setJcifsServicePrincipal(conf.getJcifsServicePrincipal());
        props.getProperties().add(jcifsProperties);

        props.setMixedModeAuthentication(conf.isMixedModeAuthentication());
        props.setIpsToCheckPattern(conf.getIpsToCheckPattern());
        props.setSend401OnAuthenticationFailure(conf.isSend401OnAuthenticationFailure());
        props.setAlternativeRemoteHostAttribute(conf.getAlternativeRemoteHostAttribute());
        props.setDnsTimeout(conf.getDnsTimeout());
        props.setHostNameClientActionStrategy(conf.getHostNameClientActionStrategy());
        props.setHostNamePatternString(conf.getHostNamePatternString());
        props.setNtlm(conf.isNtlm());
        props.setNtlmAllowed(conf.isNtlmAllowed());
        props.setPoolSize(conf.getPoolSize());
        props.setPoolTimeout(conf.getPoolTimeout());
        props.setPrincipalWithDomainName(conf.isPrincipalWithDomainName());
        props.setSpnegoAttributeName(conf.getSpnegoAttributeName());
        props.setSupportedBrowsers(conf.getSupportedBrowsers());

        props.getSystem().setUseSubjectCredsOnly(conf.isUseSubjectCredsOnly());
        props.getSystem().setLoginConf(conf.getLoginConf());
        props.getSystem().setKerberosKdc(conf.getKerberosKdc());
        props.getSystem().setKerberosRealm(conf.getKerberosRealm());
        props.getSystem().setKerberosConf(conf.getKerberosConf());
        props.getSystem().setKerberosDebug(BooleanUtils.toStringTrueFalse(conf.isKerberosDebug()));

        if (conf.getLdap() != null) {
            SpnegoLdapProperties ldapProps = new SpnegoLdapProperties();
            fill(ldapProps, conf.getLdap());
            props.setLdap(ldapProps);
        } else {
            props.setLdap(null);
        }

        props.getPrincipal().setActiveAttributeRepositoryIds(conf.getAttributeRepoId());

        return prefix("cas.authn.spnego.", CasCoreConfigurationUtils.asMap(props));
    }
}
