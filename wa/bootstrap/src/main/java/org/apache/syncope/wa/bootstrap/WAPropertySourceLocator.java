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
package org.apache.syncope.wa.bootstrap;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.attr.JDBCAttrRepoConf;
import org.apache.syncope.common.lib.attr.LDAPAttrRepoConf;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.attr.SyncopeAttrRepoConf;
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
import org.apache.syncope.common.lib.auth.U2FAuthModuleConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.core.authentication.AttributeRepositoryStates;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.core.authentication.PrincipalAttributesProperties;
import org.apereo.cas.configuration.model.core.authentication.StubPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.generic.AcceptAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jaas.JaasAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.JdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.JdbcPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.jdbc.authn.QueryJdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapPrincipalAttributesProperties;
import org.apereo.cas.configuration.model.support.mfa.DuoSecurityMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.GoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.LdapGoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.simple.CasSimpleMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.Pac4jDelegatedAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jGenericOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.saml.Pac4jSamlClientProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopeAuthenticationProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopePrincipalAttributesProperties;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.model.TriStateBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Order
public class WAPropertySourceLocator implements PropertySourceLocator {

    protected static final Logger LOG = LoggerFactory.getLogger(WAPropertySourceLocator.class);

    protected final WARestClient waRestClient;

    public WAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    protected SimpleFilterProvider getParentCasFilterProvider() {
        return new SimpleFilterProvider().
                setFailOnUnknownId(false).
                addFilter(CasConfigurationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        CasConfigurationProperties.class,
                                        CasConfigurationProperties::getAuthn)));
    }

    protected Map<String, Object> filterCasProperties(
            final CasConfigurationProperties casProperties,
            final SimpleFilterProvider filters) {

        return CasCoreConfigurationUtils.asMap(casProperties.withHolder(), filters);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final SyncopeAuthModuleConf conf,
            final String address) {

        SyncopeAuthenticationProperties props = new SyncopeAuthenticationProperties();
        props.setName(authModule);
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(address, "/rest"));

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().setSyncope(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getSyncope)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final StaticAuthModuleConf conf) {

        AcceptAuthenticationProperties props = new AcceptAuthenticationProperties();
        props.setName(authModule);
        String users = conf.getUsers().entrySet().stream().
                map(entry -> entry.getKey() + "::" + entry.getValue()).
                collect(Collectors.joining(","));
        props.setUsers(users);

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().setAccept(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getAccept)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final LDAPAuthModuleConf conf) {

        LdapAuthenticationProperties props = new LdapAuthenticationProperties();
        props.setName(authModule);
        props.setLdapUrl(conf.getLdapUrl());
        props.setBaseDn(conf.getBaseDn());
        props.setSearchFilter(conf.getSearchFilter());
        props.setBindDn(conf.getBindDn());
        props.setBindCredential(conf.getBindCredential());
        if (StringUtils.isNotBlank(conf.getBindDn()) && StringUtils.isNotBlank(conf.getBindCredential())) {
            props.setType(AbstractLdapAuthenticationProperties.AuthenticationTypes.AUTHENTICATED);
        }
        props.setPrincipalAttributeId(conf.getUserIdAttribute());
        props.setSubtreeSearch(conf.isSubtreeSearch());
        props.setPrincipalAttributeList(conf.getPrincipalAttributeList());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getLdap().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(
                AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getLdap)));
        return filterCasProperties(casProperties, filterProvider);
    }

    @SuppressWarnings("deprecation")
    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final DuoMfaAuthModuleConf conf) {

        DuoSecurityMultifactorAuthenticationProperties props = new DuoSecurityMultifactorAuthenticationProperties();
        props.setName(authModule);
        props.setDuoApiHost(conf.getApiHost());
        props.setDuoApplicationKey(conf.getApplicationKey());
        props.setDuoIntegrationKey(conf.getIntegrationKey());
        props.setDuoSecretKey(conf.getSecretKey());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getMfa().setDuo(List.of(props));

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getMfa))).
                addFilter(MultifactorAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        MultifactorAuthenticationProperties.class,
                                        MultifactorAuthenticationProperties::getDuo)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final SimpleMfaAuthModuleConf conf) {

        CasSimpleMultifactorAuthenticationProperties props =
                new CasSimpleMultifactorAuthenticationProperties();

        props.setName(authModule);
        props.setTokenLength(conf.getTokenLength());
        props.setTimeToKillInSeconds(conf.getTimeToKillInSeconds());
        props.getMail().setAttributeName(conf.getEmailAttribute());
        props.getMail().setFrom(conf.getEmailFrom());
        props.getMail().setSubject(conf.getEmailSubject());
        props.getMail().setText(conf.getEmailText());

        if (StringUtils.isNotBlank(conf.getBypassGroovyScript())) {
            try {
                props.getBypass().getGroovy().setLocation(ResourceUtils.getResourceFrom(conf.getBypassGroovyScript()));
            } catch (Exception e) {
                LOG.error("Unable to load groovy script for bypass", e);
                throw new IllegalArgumentException(e);
            }
        }
        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getMfa().setSimple(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getMfa))).
                addFilter(MultifactorAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        MultifactorAuthenticationProperties.class,
                                        MultifactorAuthenticationProperties::getSimple)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final GoogleMfaAuthModuleConf conf) {

        GoogleAuthenticatorMultifactorProperties props = new GoogleAuthenticatorMultifactorProperties();
        props.setName(authModule);
        props.getCore().setIssuer(conf.getIssuer());
        props.getCore().setCodeDigits(conf.getCodeDigits());
        props.getCore().setLabel(conf.getLabel());
        props.getCore().setTimeStepSize(conf.getTimeStepSize());
        props.getCore().setWindowSize(conf.getWindowSize());

        if (conf.getLdap() != null) {
            LdapGoogleAuthenticatorMultifactorProperties ldapProps = new LdapGoogleAuthenticatorMultifactorProperties();
            ldapProps.setAccountAttributeName(conf.getLdap().getAccountAttributeName());
            ldapProps.setBaseDn(conf.getLdap().getBaseDn());
            ldapProps.setBindCredential(conf.getLdap().getBindCredential());
            ldapProps.setBindDn(conf.getLdap().getBindDn());
            ldapProps.setSearchFilter(conf.getLdap().getSearchFilter());
            ldapProps.setLdapUrl(conf.getLdap().getUrl());
            props.setLdap(ldapProps);
        }

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getMfa().setGauth(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(
                AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getMfa))).
                addFilter(MultifactorAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        MultifactorAuthenticationProperties.class,
                                        MultifactorAuthenticationProperties::getGauth)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final U2FAuthModuleConf conf) {

        U2FMultifactorAuthenticationProperties props = new U2FMultifactorAuthenticationProperties();
        props.setName(authModule);
        props.getCore().setExpireDevices(conf.getExpireDevices());
        props.getCore().setExpireDevicesTimeUnit(TimeUnit.valueOf(conf.getExpireDevicesTimeUnit()));
        props.getCore().setExpireRegistrations(conf.getExpireRegistrations());
        props.getCore().setExpireRegistrationsTimeUnit(TimeUnit.valueOf(conf.getExpireRegistrationsTimeUnit()));

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getMfa().setU2f(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getMfa))).
                addFilter(MultifactorAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        MultifactorAuthenticationProperties.class,
                                        MultifactorAuthenticationProperties::getU2f)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final JaasAuthModuleConf conf) {

        JaasAuthenticationProperties props = new JaasAuthenticationProperties();
        props.setName(authModule);
        props.setLoginConfigType(conf.getLoginConfigType());
        props.setKerberosKdcSystemProperty(conf.getKerberosKdcSystemProperty());
        props.setKerberosRealmSystemProperty(conf.getKerberosRealmSystemProperty());
        props.setLoginConfigType(conf.getLoginConfigurationFile());
        props.setRealm(conf.getRealm());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getJaas().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getJaas)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final JDBCAuthModuleConf conf) {

        QueryJdbcAuthenticationProperties props = new QueryJdbcAuthenticationProperties();
        props.setName(authModule);
        props.setSql(conf.getSql());
        props.setFieldDisabled(conf.getFieldDisabled());
        props.setFieldExpired(conf.getFieldExpired());
        props.setFieldPassword(conf.getFieldPassword());
        props.setDialect(conf.getDialect());
        props.setDriverClass(conf.getDriverClass());
        props.setPassword(conf.getPassword());
        props.setUrl(conf.getUrl());
        props.setUser(conf.getUser());
        props.setPrincipalAttributeList(conf.getPrincipalAttributeList());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getJdbc().getQuery().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getJdbc))).
                addFilter(MultifactorAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        JdbcAuthenticationProperties.class,
                                        JdbcAuthenticationProperties::getQuery)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final OIDCAuthModuleConf conf) {

        Pac4jGenericOidcClientProperties props = new Pac4jGenericOidcClientProperties();
        props.setId(conf.getId());
        props.setCustomParams(conf.getCustomParams());
        props.setDiscoveryUri(conf.getDiscoveryUri());
        props.setMaxClockSkew(conf.getMaxClockSkew());
        props.setClientName(authModule);
        props.setPreferredJwsAlgorithm(conf.getPreferredJwsAlgorithm());
        props.setResponseMode(conf.getResponseMode());
        props.setResponseType(conf.getResponseType());
        props.setScope(conf.getScope());
        props.setSecret(conf.getSecret());
        props.setPrincipalAttributeId(conf.getUserIdAttribute());
        Pac4jOidcClientProperties client = new Pac4jOidcClientProperties();
        client.setGeneric(props);

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getPac4j().getOidc().add(client);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getPac4j))).
                addFilter(Pac4jDelegatedAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        Pac4jDelegatedAuthenticationProperties.class,
                                        Pac4jDelegatedAuthenticationProperties::getOidc)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAuthModule(
            final String authModule,
            final SAML2IdPAuthModuleConf conf) {

        Pac4jSamlClientProperties props = new Pac4jSamlClientProperties();
        props.setClientName(authModule);
        props.setAcceptedSkew(conf.getAcceptedSkew());
        props.setAssertionConsumerServiceIndex(conf.getAssertionConsumerServiceIndex());
        props.setAttributeConsumingServiceIndex(conf.getAttributeConsumingServiceIndex());
        props.setAuthnContextClassRef(conf.getAuthnContextClassRefs());
        props.setAuthnContextComparisonType(conf.getAuthnContextComparisonType());
        props.setBlockedSignatureSigningAlgorithms(conf.getBlockedSignatureSigningAlgorithms());
        props.setDestinationBinding(conf.getDestinationBinding().getUri());
        props.setIdentityProviderMetadataPath(conf.getIdentityProviderMetadataPath());
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

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getPac4j().getSaml().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(AuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        AuthenticationProperties.class,
                                        AuthenticationProperties::getPac4j))).
                addFilter(Pac4jDelegatedAuthenticationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        Pac4jDelegatedAuthenticationProperties.class,
                                        Pac4jDelegatedAuthenticationProperties::getSaml)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAttrRepo(
            final AttrRepoTO attrRepoTO,
            final StubAttrRepoConf conf) {

        StubPrincipalAttributesProperties props = new StubPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setAttributes(conf.getAttributes());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getAttributeRepository().setStub(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(
                PrincipalAttributesProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                PrincipalAttributesProperties.class,
                                PrincipalAttributesProperties::getStub)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAttrRepo(
            final AttrRepoTO attrRepoTO,
            final LDAPAttrRepoConf conf) {

        LdapPrincipalAttributesProperties props = new LdapPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setLdapUrl(conf.getLdapUrl());
        props.setBaseDn(conf.getBaseDn());
        props.setSearchFilter(conf.getSearchFilter());
        props.setBindDn(conf.getBindDn());
        props.setBindCredential(conf.getBindCredential());
        props.setSubtreeSearch(conf.isSubtreeSearch());
        props.setAttributes(conf.getAttributes());
        props.setUseAllQueryAttributes(conf.isUseAllQueryAttributes());
        props.setQueryAttributes(conf.getQueryAttributes());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getAttributeRepository().getLdap().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(
                PrincipalAttributesProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                PrincipalAttributesProperties.class,
                                PrincipalAttributesProperties::getLdap)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAttrRepo(
            final AttrRepoTO attrRepoTO,
            final JDBCAttrRepoConf conf) {

        JdbcPrincipalAttributesProperties props = new JdbcPrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setSql(conf.getSql());
        props.setDialect(conf.getDialect());
        props.setDriverClass(conf.getDriverClass());
        props.setPassword(conf.getPassword());
        props.setUrl(conf.getUrl());
        props.setUser(conf.getUser());
        props.setSingleRow(conf.isSingleRow());
        props.setRequireAllAttributes(conf.isRequireAllAttributes());
        props.setCaseCanonicalization(conf.getCaseCanonicalization().name());
        props.setQueryType(conf.getQueryType().name());
        props.setColumnMappings(conf.getColumnMappings());
        props.setUsername(conf.getUsername());
        props.setAttributes(conf.getAttributes());
        props.setCaseInsensitiveQueryAttributes(conf.getCaseInsensitiveQueryAttributes());
        props.setQueryAttributes(conf.getQueryAttributes());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getAttributeRepository().getJdbc().add(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.
                addFilter(PrincipalAttributesProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        PrincipalAttributesProperties.class,
                                        PrincipalAttributesProperties::getJdbc)));
        return filterCasProperties(casProperties, filterProvider);
    }

    protected Map<String, Object> mapAttrRepo(
            final AttrRepoTO attrRepoTO,
            final SyncopeAttrRepoConf conf,
            final String address) {

        SyncopePrincipalAttributesProperties props = new SyncopePrincipalAttributesProperties();
        props.setId(attrRepoTO.getKey());
        props.setState(AttributeRepositoryStates.valueOf(attrRepoTO.getState().name()));
        props.setOrder(attrRepoTO.getOrder());
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(address, "/rest"));
        props.setSearchFilter(conf.getSearchFilter());
        props.setBasicAuthUsername(conf.getBasicAuthUsername());
        props.setBasicAuthPassword(conf.getBasicAuthPassword());
        props.setHeaders(props.getHeaders());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getAttributeRepository().setSyncope(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(PrincipalAttributesProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                PrincipalAttributesProperties.class,
                                PrincipalAttributesProperties::getSyncope)));
        return filterCasProperties(casProperties, filterProvider);
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        }

        LOG.info("Bootstrapping WA configuration");
        Map<String, Object> properties = new TreeMap<>();

        syncopeClient.getService(AuthModuleService.class).list().forEach(authModuleTO -> {
            AuthModuleConf conf = authModuleTO.getConf();
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            if (conf instanceof StaticAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (StaticAuthModuleConf) conf));
            } else if (conf instanceof LDAPAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (LDAPAuthModuleConf) conf));
            } else if (conf instanceof SyncopeAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), 
                        (SyncopeAuthModuleConf) conf, syncopeClient.getAddress()));
            } else if (conf instanceof GoogleMfaAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (GoogleMfaAuthModuleConf) conf));
            } else if (conf instanceof SimpleMfaAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (SimpleMfaAuthModuleConf) conf));
            } else if (conf instanceof DuoMfaAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (DuoMfaAuthModuleConf) conf));
            } else if (conf instanceof JaasAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (JaasAuthModuleConf) conf));
            } else if (conf instanceof JDBCAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (JDBCAuthModuleConf) conf));
            } else if (conf instanceof OIDCAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (OIDCAuthModuleConf) conf));
            } else if (conf instanceof SAML2IdPAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (SAML2IdPAuthModuleConf) conf));
            } else if (conf instanceof U2FAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (U2FAuthModuleConf) conf));
            }
        });

        syncopeClient.getService(AttrRepoService.class).list().forEach(attrRepoTO -> {
            AttrRepoConf conf = attrRepoTO.getConf();
            LOG.debug("Mapping attr repo {} ", attrRepoTO.getKey());

            if (conf instanceof StubAttrRepoConf) {
                properties.putAll(mapAttrRepo(attrRepoTO, (StubAttrRepoConf) conf));
            } else if (conf instanceof LDAPAttrRepoConf) {
                properties.putAll(mapAttrRepo(attrRepoTO, (LDAPAttrRepoConf) conf));
            } else if (conf instanceof JDBCAttrRepoConf) {
                properties.putAll(mapAttrRepo(attrRepoTO, (JDBCAttrRepoConf) conf));
            } else if (conf instanceof SyncopeAttrRepoConf) {
                properties.putAll(mapAttrRepo(attrRepoTO, (SyncopeAttrRepoConf) conf, syncopeClient.getAddress()));
            }
        });

        syncopeClient.getService(WAConfigService.class).list().forEach(attr -> properties.put(
                attr.getSchema(), attr.getValues().stream().collect(Collectors.joining(","))));

        LOG.debug("Collected WA properties: {}", properties);
        return new MapPropertySource(getClass().getName(), properties);
    }
}
