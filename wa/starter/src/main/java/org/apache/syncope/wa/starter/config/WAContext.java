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
package org.apache.syncope.wa.starter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.wa.bootstrap.WAProperties;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.wa.starter.actuate.SyncopeWAInfoContributor;
import org.apache.syncope.wa.starter.audit.WAAuditTrailManager;
import org.apache.syncope.wa.starter.events.WAEventRepository;
import org.apache.syncope.wa.starter.gauth.WAGoogleMfaAuthCredentialRepository;
import org.apache.syncope.wa.starter.gauth.WAGoogleMfaAuthTokenRepository;
import org.apache.syncope.wa.starter.mapping.AccessMapFor;
import org.apache.syncope.wa.starter.mapping.AccessMapper;
import org.apache.syncope.wa.starter.mapping.AttrReleaseMapFor;
import org.apache.syncope.wa.starter.mapping.AttrReleaseMapper;
import org.apache.syncope.wa.starter.mapping.AuthMapFor;
import org.apache.syncope.wa.starter.mapping.AuthMapper;
import org.apache.syncope.wa.starter.mapping.CASSPClientAppTOMapper;
import org.apache.syncope.wa.starter.mapping.ClientAppMapFor;
import org.apache.syncope.wa.starter.mapping.ClientAppMapper;
import org.apache.syncope.wa.starter.mapping.DefaultAccessMapper;
import org.apache.syncope.wa.starter.mapping.DefaultAttrReleaseMapper;
import org.apache.syncope.wa.starter.mapping.DefaultAuthMapper;
import org.apache.syncope.wa.starter.mapping.OIDCRPClientAppTOMapper;
import org.apache.syncope.wa.starter.mapping.RegisteredServiceMapper;
import org.apache.syncope.wa.starter.mapping.SAML2SPClientAppTOMapper;
import org.apache.syncope.wa.starter.oidc.WAOIDCJWKSGeneratorService;
import org.apache.syncope.wa.starter.pac4j.saml.WASAML2ClientCustomizer;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataGenerator;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataLocator;
import org.apache.syncope.wa.starter.services.WAServiceRegistry;
import org.apache.syncope.wa.starter.surrogate.WASurrogateAuthenticationService;
import org.apache.syncope.wa.starter.u2f.WAU2FDeviceRepository;
import org.apache.syncope.wa.starter.webauthn.WAWebAuthnCredentialRepository;
import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRepository;
import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.LdapGoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FCoreMultifactorAuthenticationProperties;
import org.apereo.cas.gauth.credential.LdapGoogleAuthenticatorTokenCredentialRepository;
import org.apereo.cas.oidc.jwks.generator.OidcJsonWebKeystoreGeneratorService;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.events.CasEventRepository;
import org.apereo.cas.support.events.CasEventRepositoryFilter;
import org.apereo.cas.support.pac4j.authentication.clients.DelegatedClientFactoryCustomizer;
import org.apereo.cas.support.pac4j.authentication.handler.support.DelegatedClientAuthenticationHandler;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import org.ldaptive.ConnectionFactory;
import org.pac4j.core.client.Client;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

@Configuration(proxyBeanMethods = false)
public class WAContext {

    public static final String CUSTOM_GOOGLE_AUTHENTICATOR_ACCOUNT_REGISTRY =
            "customGoogleAuthenticatorAccountRegistry";

    private static String version(final ConfigurableApplicationContext ctx) {
        return ctx.getEnvironment().getProperty("version");
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenAPI casSwaggerOpenApi(final ConfigurableApplicationContext ctx) {
        return new OpenAPI().
                info(new Info().
                        title("Apache Syncope").
                        description("Apache Syncope " + version(ctx)).
                        contact(new Contact().
                                name("The Apache Syncope community").
                                email("dev@syncope.apache.org").
                                url("https://syncope.apache.org")).
                        version(version(ctx))).
                schemaRequirement("BasicAuthentication",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")).
                schemaRequirement("Bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    }

    @ConditionalOnMissingBean(name = "accessMapper")
    @Bean
    public AccessMapper accessMapper() {
        return new DefaultAccessMapper();
    }

    @ConditionalOnMissingBean(name = "attrReleaseMapper")
    @Bean
    public AttrReleaseMapper attrReleaseMapper() {
        return new DefaultAttrReleaseMapper();
    }

    @ConditionalOnMissingBean(name = "authMapper")
    @Bean
    public AuthMapper authMapper() {
        return new DefaultAuthMapper();
    }

    @ConditionalOnMissingBean(name = "casSPClientAppTOMapper")
    @Bean
    public ClientAppMapper casSPClientAppTOMapper() {
        return new CASSPClientAppTOMapper();
    }

    @ConditionalOnMissingBean(name = "oidcRPClientAppTOMapper")
    @Bean
    public ClientAppMapper oidcRPClientAppTOMapper() {
        return new OIDCRPClientAppTOMapper();
    }

    @ConditionalOnMissingBean(name = "saml2SPClientAppTOMapper")
    @Bean
    public ClientAppMapper saml2SPClientAppTOMapper() {
        return new SAML2SPClientAppTOMapper();
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean
    @Bean
    public RegisteredServiceMapper registeredServiceMapper(
            final ConfigurableApplicationContext ctx,
            final CasConfigurationProperties casProperties,
            final ObjectProvider<AuthenticationEventExecutionPlan> authenticationEventExecutionPlan) {

        Map<String, AuthMapper> authPolicyConfMappers = new HashMap<>();
        ctx.getBeansOfType(AuthMapper.class).forEach((name, bean) -> {
            AuthMapFor authMapFor = ctx.findAnnotationOnBean(name, AuthMapFor.class);
            if (authMapFor != null) {
                authPolicyConfMappers.put(authMapFor.authPolicyConfClass().getName(), bean);
            }
        });

        Map<String, AccessMapper> accessPolicyConfMappers = new HashMap<>();
        ctx.getBeansOfType(AccessMapper.class).forEach((name, bean) -> {
            AccessMapFor accessMapFor = ctx.findAnnotationOnBean(name, AccessMapFor.class);
            if (accessMapFor != null) {
                accessPolicyConfMappers.put(accessMapFor.accessPolicyConfClass().getName(), bean);
            }
        });

        Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers = new HashMap<>();
        ctx.getBeansOfType(AttrReleaseMapper.class).forEach((name, bean) -> {
            AttrReleaseMapFor attrReleaseMapFor =
                    ctx.findAnnotationOnBean(name, AttrReleaseMapFor.class);
            if (attrReleaseMapFor != null) {
                attrReleasePolicyConfMappers.put(attrReleaseMapFor.attrReleasePolicyConfClass().getName(), bean);
            }
        });

        Map<String, ClientAppMapper> clientAppTOMappers = new HashMap<>();
        ctx.getBeansOfType(ClientAppMapper.class).forEach((name, bean) -> {
            ClientAppMapFor clientAppMapFor = ctx.findAnnotationOnBean(name, ClientAppMapFor.class);
            if (clientAppMapFor != null) {
                clientAppTOMappers.put(clientAppMapFor.clientAppClass().getName(), bean);
            }
        });

        return new RegisteredServiceMapper(
                ctx,
                Optional.ofNullable(casProperties.getAuthn().getPac4j().getCore().getName()).
                        orElse(DelegatedClientAuthenticationHandler.class.getSimpleName()),
                authenticationEventExecutionPlan,
                authPolicyConfMappers,
                accessPolicyConfMappers,
                attrReleasePolicyConfMappers,
                clientAppTOMappers);
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean
    @Bean
    public ServiceRegistryExecutionPlanConfigurer syncopeServiceRegistryConfigurer(
            final ConfigurableApplicationContext ctx,
            final WARestClient restClient,
            final RegisteredServiceMapper registeredServiceMapper,
            @Qualifier("serviceRegistryListeners")
            final ObjectProvider<List<ServiceRegistryListener>> serviceRegistryListeners) {

        WAServiceRegistry registry = new WAServiceRegistry(
                restClient, registeredServiceMapper, ctx,
                Optional.ofNullable(serviceRegistryListeners.getIfAvailable()).orElseGet(ArrayList::new));
        return plan -> plan.registerServiceRegistry(registry);
    }

    @Bean
    public SamlIdPMetadataGenerator samlIdPMetadataGenerator(
            final WARestClient restClient,
            final SamlIdPMetadataGeneratorConfigurationContext context) {

        return new RestfulSamlIdPMetadataGenerator(context, restClient);
    }

    @Bean
    public SamlIdPMetadataLocator samlIdPMetadataLocator(final WARestClient restClient) {
        return new RestfulSamlIdPMetadataLocator(
                CipherExecutor.noOpOfStringToString(),
                Caffeine.newBuilder().build(),
                restClient);
    }

    @Bean
    public AuditTrailExecutionPlanConfigurer auditConfigurer(final WARestClient restClient) {
        return plan -> plan.registerAuditTrailManager(new WAAuditTrailManager(restClient));
    }

    @ConditionalOnMissingBean(name = "syncopeWAEventRepositoryFilter")
    @Bean
    public CasEventRepositoryFilter syncopeWAEventRepositoryFilter() {
        return CasEventRepositoryFilter.noOp();
    }

    @Bean
    public CasEventRepository casEventRepository(
            final WARestClient restClient,
            @Qualifier("syncopeWAEventRepositoryFilter")
            final CasEventRepositoryFilter syncopeWAEventRepositoryFilter) {

        return new WAEventRepository(syncopeWAEventRepositoryFilter, restClient);
    }

    @Bean
    public DelegatedClientFactoryCustomizer<Client> delegatedClientCustomizer(final WARestClient restClient) {
        return new WASAML2ClientCustomizer(restClient);
    }

    @Bean
    public WAGoogleMfaAuthTokenRepository oneTimeTokenAuthenticatorTokenRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {

        return new WAGoogleMfaAuthTokenRepository(
                restClient, casProperties.getAuthn().getMfa().getGauth().getCore().getTimeStepSize());
    }

    @ConditionalOnMissingBean(name = CUSTOM_GOOGLE_AUTHENTICATOR_ACCOUNT_REGISTRY)
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public OneTimeTokenCredentialRepository googleAuthenticatorAccountRegistry(
            final CasConfigurationProperties casProperties,
            @Qualifier("googleAuthenticatorAccountCipherExecutor")
            final CipherExecutor<String, String> googleAuthenticatorAccountCipherExecutor,
            @Qualifier("googleAuthenticatorScratchCodesCipherExecutor")
            final CipherExecutor<Number, Number> googleAuthenticatorScratchCodesCipherExecutor,
            final IGoogleAuthenticator googleAuthenticatorInstance,
            final WARestClient restClient) {

        /*
         * Declaring the LDAP-based repository as a Spring bean that would be conditionally activated
         * via properties using annotations is not possible; conditionally-created spring beans cannot be
         * refreshed, which means the settings ever change and the context is refreshed, the repository
         * option can not be re-created. This could be revisited later in CAS 6.6.x using the {@code BeanSupplier}
         * API construct to recreate the same bean in a more conventional way.
         */
        LdapGoogleAuthenticatorMultifactorProperties ldap = casProperties.getAuthn().getMfa().getGauth().getLdap();
        if (StringUtils.isNotBlank(ldap.getBaseDn())
                && StringUtils.isNotBlank(ldap.getLdapUrl())
                && StringUtils.isNotBlank(ldap.getSearchFilter())) {

            ConnectionFactory connectionFactory = LdapUtils.newLdaptiveConnectionFactory(ldap);
            return new LdapGoogleAuthenticatorTokenCredentialRepository(
                    googleAuthenticatorAccountCipherExecutor,
                    googleAuthenticatorScratchCodesCipherExecutor,
                    googleAuthenticatorInstance,
                    connectionFactory,
                    ldap);
        }
        return new WAGoogleMfaAuthCredentialRepository(restClient, googleAuthenticatorInstance);
    }

    @Bean
    public OidcJsonWebKeystoreGeneratorService oidcJsonWebKeystoreGeneratorService(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {

        return new WAOIDCJWKSGeneratorService(
                restClient,
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksKeyId(),
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksType(),
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksKeySize());
    }

    @Bean
    public WebAuthnCredentialRepository webAuthnCredentialRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {

        return new WAWebAuthnCredentialRepository(casProperties, restClient);
    }

    @Bean
    public U2FDeviceRepository u2fDeviceRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {

        U2FCoreMultifactorAuthenticationProperties u2f = casProperties.getAuthn().getMfa().getU2f().getCore();
        OffsetDateTime expirationDate = OffsetDateTime.now().
                minus(u2f.getExpireDevices(), DateTimeUtils.toChronoUnit(u2f.getExpireDevicesTimeUnit()));
        LoadingCache<String, String> requestStorage = Caffeine.newBuilder().
                expireAfterWrite(u2f.getExpireRegistrations(), u2f.getExpireRegistrationsTimeUnit()).
                build(key -> StringUtils.EMPTY);
        return new WAU2FDeviceRepository(casProperties, requestStorage, restClient, expirationDate);
    }

    @Bean
    public SurrogateAuthenticationService surrogateAuthenticationService(final WARestClient restClient) {
        return new WASurrogateAuthenticationService(restClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(final WARestClient restClient) {
        return new SyncopeCoreHealthIndicator(restClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeWAInfoContributor syncopeWAInfoContributor(final WAProperties waProperties) {
        return new SyncopeWAInfoContributor(waProperties);
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.WA);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.WA);
    }
}
