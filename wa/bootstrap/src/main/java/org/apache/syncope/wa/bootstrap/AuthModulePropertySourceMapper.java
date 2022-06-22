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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.generic.AcceptAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jaas.JaasAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.JdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.authn.QueryJdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
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
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.model.TriStateBoolean;

public class AuthModulePropertySourceMapper extends PropertySourceMapper implements AuthModuleConf.Mapper {

    protected final String syncopeClientAddress;

    protected final AuthModuleTO authModuleTO;

    public AuthModulePropertySourceMapper(final String syncopeClientAddress, final AuthModuleTO attrRepoTO) {
        this.syncopeClientAddress = syncopeClientAddress;
        this.authModuleTO = attrRepoTO;
    }

    @Override
    public Map<String, Object> map(final StaticAuthModuleConf conf) {
        AcceptAuthenticationProperties props = new AcceptAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final LDAPAuthModuleConf conf) {
        LdapAuthenticationProperties props = new LdapAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final JDBCAuthModuleConf conf) {
        QueryJdbcAuthenticationProperties props = new QueryJdbcAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final JaasAuthModuleConf conf) {
        JaasAuthenticationProperties props = new JaasAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final OIDCAuthModuleConf conf) {
        Pac4jGenericOidcClientProperties props = new Pac4jGenericOidcClientProperties();
        props.setId(conf.getId());
        props.setCustomParams(conf.getCustomParams());
        props.setDiscoveryUri(conf.getDiscoveryUri());
        props.setMaxClockSkew(conf.getMaxClockSkew());
        props.setClientName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final SAML2IdPAuthModuleConf conf) {
        Pac4jSamlClientProperties props = new Pac4jSamlClientProperties();
        props.setClientName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final SyncopeAuthModuleConf conf) {
        SyncopeAuthenticationProperties props = new SyncopeAuthenticationProperties();
        props.setName(authModuleTO.getKey());
        props.setDomain(conf.getDomain());
        props.setUrl(StringUtils.substringBefore(syncopeClientAddress, "/rest"));

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

    @Override
    public Map<String, Object> map(final GoogleMfaAuthModuleConf conf) {
        GoogleAuthenticatorMultifactorProperties props = new GoogleAuthenticatorMultifactorProperties();
        props.setName(authModuleTO.getKey());
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

    @SuppressWarnings("deprecation")
    @Override
    public Map<String, Object> map(final DuoMfaAuthModuleConf conf) {
        DuoSecurityMultifactorAuthenticationProperties props = new DuoSecurityMultifactorAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final U2FAuthModuleConf conf) {
        U2FMultifactorAuthenticationProperties props = new U2FMultifactorAuthenticationProperties();
        props.setName(authModuleTO.getKey());
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

    @Override
    public Map<String, Object> map(final SimpleMfaAuthModuleConf conf) {
        CasSimpleMultifactorAuthenticationProperties props = new CasSimpleMultifactorAuthenticationProperties();

        props.setName(authModuleTO.getKey());
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
}
