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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasCoreConfigurationUtils;
import org.apereo.cas.configuration.model.support.generic.AcceptAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jaas.JaasAuthenticationProperties;
import org.apereo.cas.configuration.model.support.jdbc.authn.QueryJdbcAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.GoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FMultifactorProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jGenericOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.oidc.Pac4jOidcClientProperties;
import org.apereo.cas.configuration.model.support.pac4j.saml.Pac4jSamlClientProperties;
import org.apereo.cas.configuration.model.support.radius.RadiusProperties;
import org.apereo.cas.configuration.model.support.syncope.SyncopeAuthenticationProperties;
import org.apereo.cas.util.model.TriStateBoolean;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
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
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

@Order
public class SyncopeWAPropertySourceLocator implements PropertySourceLocator {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWABootstrapConfiguration.class);

    private final WARestClient waRestClient;

    public SyncopeWAPropertySourceLocator(final WARestClient waRestClient) {
        this.waRestClient = waRestClient;
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final SyncopeAuthModuleConf conf,
            final String address) {

        SyncopeAuthenticationProperties syncopeProps = new SyncopeAuthenticationProperties();
        syncopeProps.setName(authModule);
        syncopeProps.setDomain(conf.getDomain());
        syncopeProps.setUrl(StringUtils.substringBefore(address, "/rest"));

        casProperties.getAuthn().setSyncope(syncopeProps);
        return "cas.authn.syncope.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final StaticAuthModuleConf conf) {

        AcceptAuthenticationProperties staticProps = new AcceptAuthenticationProperties();
        staticProps.setName(authModule);
        String users = conf.getUsers().entrySet().stream().
                map(entry -> entry.getKey() + "::" + entry.getValue()).
                collect(Collectors.joining(","));
        staticProps.setUsers(users);

        casProperties.getAuthn().setAccept(staticProps);
        return "cas.authn.accept.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final LDAPAuthModuleConf conf) {

        LdapAuthenticationProperties ldapProps = new LdapAuthenticationProperties();
        ldapProps.setName(authModule);
        ldapProps.setBaseDn(conf.getBaseDn());
        ldapProps.setBindCredential(conf.getBindCredential());
        ldapProps.setSearchFilter(conf.getSearchFilter());
        ldapProps.setPrincipalAttributeId(conf.getUserIdAttribute());
        ldapProps.setLdapUrl(conf.getLdapUrl());
        ldapProps.setSubtreeSearch(conf.isSubtreeSearch());
        ldapProps.setPrincipalAttributeList(conf.getPrincipalAttributeList());

        casProperties.getAuthn().getLdap().add(ldapProps);
        return "cas.authn.ldap.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final GoogleMfaAuthModuleConf conf) {

        GoogleAuthenticatorMultifactorProperties props = new GoogleAuthenticatorMultifactorProperties();
        props.setName(authModule);
        props.setIssuer(conf.getIssuer());
        props.setCodeDigits(conf.getCodeDigits());
        props.setLabel(conf.getLabel());
        props.setTimeStepSize(conf.getTimeStepSize());
        props.setWindowSize(conf.getWindowSize());

        casProperties.getAuthn().getMfa().setGauth(props);
        return "cas.authn.mfa.gauth.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final U2FAuthModuleConf conf) {

        U2FMultifactorProperties props = new U2FMultifactorProperties();
        props.setName(authModule);
        props.setExpireDevices(conf.getExpireDevices());
        props.setExpireDevicesTimeUnit(TimeUnit.valueOf(conf.getExpireDevicesTimeUnit()));
        props.setExpireRegistrations(conf.getExpireRegistrations());
        props.setExpireRegistrationsTimeUnit(TimeUnit.valueOf(conf.getExpireRegistrationsTimeUnit()));

        casProperties.getAuthn().getMfa().setU2f(props);
        return "cas.authn.mfa.u2f.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
            final String authModule,
            final JaasAuthModuleConf conf) {

        JaasAuthenticationProperties props = new JaasAuthenticationProperties();
        props.setName(authModule);
        props.setLoginConfigType(conf.getLoginConfigType());
        props.setKerberosKdcSystemProperty(conf.getKerberosKdcSystemProperty());
        props.setKerberosRealmSystemProperty(conf.getKerberosRealmSystemProperty());
        props.setLoginConfigType(conf.getLoginConfigurationFile());
        props.setRealm(conf.getRealm());

        casProperties.getAuthn().getJaas().add(props);
        return "cas.authn.jaas.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
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

        casProperties.getAuthn().getJdbc().getQuery().add(props);
        return "cas.authn.jdbc.query.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
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

        casProperties.getAuthn().getPac4j().getOidc().add(client);
        return "cas.authn.pac4j.oidc.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
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

        casProperties.getAuthn().setRadius(props);
        return "cas.authn.radius.";
    }

    private static String mapAuthModule(
            final CasConfigurationProperties casProperties,
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
        props.setDestinationBinding(conf.getDestinationBinding());
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

        casProperties.getAuthn().getPac4j().getSaml().add(props);
        return "cas.authn.pac4j.saml.";
    }

    @Override
    public PropertySource<?> locate(final Environment environment) {
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        if (syncopeClient == null) {
            LOG.warn("Application context is not ready to bootstrap WA configuration");
            return null;
        }

        LOG.info("Bootstrapping WA configuration");

        CasConfigurationProperties casProperties = new CasConfigurationProperties();
        List<String> filters = new ArrayList<>();

        syncopeClient.getService(AuthModuleService.class).list().forEach(authModuleTO -> {
            AuthModuleConf authConf = authModuleTO.getConf();
            LOG.debug("Mapping auth module {} ", authModuleTO.getKey());

            if (authConf instanceof LDAPAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (LDAPAuthModuleConf) authConf));
            } else if (authConf instanceof StaticAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (StaticAuthModuleConf) authConf));
            } else if (authConf instanceof SyncopeAuthModuleConf) {
                filters.add(mapAuthModule(
                        casProperties,
                        authModuleTO.getKey(),
                        (SyncopeAuthModuleConf) authConf,
                        waRestClient.getSyncopeClient().getAddress()));
            } else if (authConf instanceof GoogleMfaAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (GoogleMfaAuthModuleConf) authConf));
            } else if (authConf instanceof JaasAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (JaasAuthModuleConf) authConf));
            } else if (authConf instanceof JDBCAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (JDBCAuthModuleConf) authConf));
            } else if (authConf instanceof OIDCAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (OIDCAuthModuleConf) authConf));
            } else if (authConf instanceof RadiusAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (RadiusAuthModuleConf) authConf));
            } else if (authConf instanceof SAML2IdPAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (SAML2IdPAuthModuleConf) authConf));
            } else if (authConf instanceof U2FAuthModuleConf) {
                filters.add(mapAuthModule(casProperties, authModuleTO.getKey(), (U2FAuthModuleConf) authConf));
            }
        });

        Map<String, Object> properties = CasCoreConfigurationUtils.asMap(casProperties.withHolder()).
                entrySet().stream().
                filter(entry -> filters.stream().filter(Objects::nonNull).
                anyMatch(prefix -> entry.getKey().startsWith(prefix))).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        LOG.debug("Collected WA properties: {}", properties);

        return new MapPropertySource(getClass().getName(), properties);
    }
}
