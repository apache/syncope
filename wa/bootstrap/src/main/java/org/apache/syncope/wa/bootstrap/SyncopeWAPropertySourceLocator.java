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
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.DuoMfaAuthModuleConf;
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
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
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
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FMultifactorAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.Pac4jDelegatedAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jGenericOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.saml.Pac4jSamlClientProperties;
import org.apereo.cas.configuration.model.support.radius.RadiusProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopeAuthenticationProperties;
import org.apereo.cas.util.model.TriStateBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Order
public class SyncopeWAPropertySourceLocator implements PropertySourceLocator {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAPropertySourceLocator.class);

    private final WARestClient waRestClient;

    public SyncopeWAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    private static SimpleFilterProvider getParentCasFilterProvider() {
        return new SimpleFilterProvider().
                setFailOnUnknownId(false).
                addFilter(CasConfigurationProperties.class.getSimpleName(),
                        SimpleBeanPropertyFilter.filterOutAllExcept(
                                CasCoreConfigurationUtils.getPropertyName(
                                        CasConfigurationProperties.class,
                                        CasConfigurationProperties::getAuthn)));
    }

    private static Map<String, Object> filterCasProperties(
            final CasConfigurationProperties casProperties,
            final SimpleFilterProvider filters) {

        return CasCoreConfigurationUtils.asMap(casProperties.withHolder(), filters);
    }

    private static Map<String, Object> mapAuthModule(
            final String authModule,
            final SyncopeAuthModuleConf conf,
            final String address) {

        SyncopeAuthenticationProperties syncopeProps = new SyncopeAuthenticationProperties();
        syncopeProps.setName(authModule);
        syncopeProps.setDomain(conf.getDomain());
        syncopeProps.setUrl(StringUtils.substringBefore(address, "/rest"));

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().setSyncope(syncopeProps);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getSyncope)));
        return filterCasProperties(casProperties, filterProvider);
    }

    private static Map<String, Object> mapAuthModule(
            final String authModule,
            final StaticAuthModuleConf conf) {

        AcceptAuthenticationProperties staticProps = new AcceptAuthenticationProperties();
        staticProps.setName(authModule);
        String users = conf.getUsers().entrySet().stream().
                map(entry -> entry.getKey() + "::" + entry.getValue()).
                collect(Collectors.joining(","));
        staticProps.setUsers(users);

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().setAccept(staticProps);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getAccept)));
        return filterCasProperties(casProperties, filterProvider);
    }

    private static Map<String, Object> mapAuthModule(
            final String authModule,
            final LDAPAuthModuleConf conf) {

        LdapAuthenticationProperties ldapProps = new LdapAuthenticationProperties();
        ldapProps.setName(authModule);
        ldapProps.setLdapUrl(conf.getLdapUrl());
        ldapProps.setBaseDn(conf.getBaseDn());
        ldapProps.setSearchFilter(conf.getSearchFilter());
        ldapProps.setBindDn(conf.getBindDn());
        ldapProps.setBindCredential(conf.getBindCredential());
        if (StringUtils.isNotBlank(conf.getBindDn()) && StringUtils.isNotBlank(conf.getBindCredential())) {
            ldapProps.setType(AbstractLdapAuthenticationProperties.AuthenticationTypes.AUTHENTICATED);
        }
        ldapProps.setPrincipalAttributeId(conf.getUserIdAttribute());
        ldapProps.setSubtreeSearch(conf.isSubtreeSearch());
        ldapProps.setPrincipalAttributeList(conf.getPrincipalAttributeList());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().getLdap().add(ldapProps);

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
    private static Map<String, Object> mapAuthModule(
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

    private static Map<String, Object> mapAuthModule(
            final String authModule,
            final GoogleMfaAuthModuleConf conf) {

        GoogleAuthenticatorMultifactorProperties props =
                new GoogleAuthenticatorMultifactorProperties();
        props.setName(authModule);
        props.getCore().setIssuer(conf.getIssuer());
        props.getCore().setCodeDigits(conf.getCodeDigits());
        props.getCore().setLabel(conf.getLabel());
        props.getCore().setTimeStepSize(conf.getTimeStepSize());
        props.getCore().setWindowSize(conf.getWindowSize());

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

    private static Map<String, Object> mapAuthModule(
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

    private static Map<String, Object> mapAuthModule(
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

    private static Map<String, Object> mapAuthModule(
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

    private static Map<String, Object> mapAuthModule(
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

    private static Map<String, Object> mapAuthModule(
            final String authModule,
            final RadiusAuthModuleConf conf) {

        RadiusProperties props = new RadiusProperties();
        props.setName(authModule);

        props.getClient().setAccountingPort(conf.getAccountingPort());
        props.getClient().setAuthenticationPort(conf.getAuthenticationPort());
        props.getClient().setInetAddress(conf.getInetAddress());
        props.getClient().setSharedSecret(conf.getSharedSecret());
        props.getClient().setSocketTimeout(conf.getSocketTimeout());

        props.getServer().setNasIdentifier(conf.getNasIdentifier());
        props.getServer().setNasIpAddress(conf.getNasIpAddress());
        props.getServer().setNasIpv6Address(conf.getNasIpv6Address());
        props.getServer().setNasPort(conf.getNasPort());
        props.getServer().setNasPortId(conf.getNasPortId());
        props.getServer().setNasPortType(conf.getNasPortType());
        props.getServer().setNasRealPort(conf.getNasRealPort());
        props.getServer().setProtocol(conf.getProtocol());
        props.getServer().setRetries(conf.getRetries());

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        casProperties.getAuthn().setRadius(props);

        SimpleFilterProvider filterProvider = getParentCasFilterProvider();
        filterProvider.addFilter(
                AuthenticationProperties.class.getSimpleName(),
                SimpleBeanPropertyFilter.filterOutAllExcept(
                        CasCoreConfigurationUtils.getPropertyName(
                                AuthenticationProperties.class,
                                AuthenticationProperties::getRadius)));
        return filterCasProperties(casProperties, filterProvider);
    }

    private static Map<String, Object> mapAuthModule(
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
            AuthModuleConf authConf = authModuleTO.getConf();
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            if (authConf instanceof LDAPAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (LDAPAuthModuleConf) authConf));
            } else if (authConf instanceof StaticAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (StaticAuthModuleConf) authConf));
            } else if (authConf instanceof SyncopeAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(),
                        (SyncopeAuthModuleConf) authConf, syncopeClient.getAddress()));
            } else if (authConf instanceof GoogleMfaAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (GoogleMfaAuthModuleConf) authConf));
            } else if (authConf instanceof DuoMfaAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (DuoMfaAuthModuleConf) authConf));
            } else if (authConf instanceof JaasAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (JaasAuthModuleConf) authConf));
            } else if (authConf instanceof JDBCAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (JDBCAuthModuleConf) authConf));
            } else if (authConf instanceof OIDCAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (OIDCAuthModuleConf) authConf));
            } else if (authConf instanceof RadiusAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (RadiusAuthModuleConf) authConf));
            } else if (authConf instanceof SAML2IdPAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (SAML2IdPAuthModuleConf) authConf));
            } else if (authConf instanceof U2FAuthModuleConf) {
                properties.putAll(mapAuthModule(authModuleTO.getKey(), (U2FAuthModuleConf) authConf));
            }
        });

        syncopeClient.getService(WAConfigService.class).list().
                forEach(attr -> properties.put(attr.getSchema(), attr.getValues()));
        LOG.debug("Collected WA properties: {}", properties);
        return new MapPropertySource(getClass().getName(), properties);
    }
}
