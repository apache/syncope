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

import com.github.benmanes.caffeine.cache.Cache;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.wa.bootstrap.WAProperties;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.bootstrap.mapping.AttrReleaseMapper;
import org.apache.syncope.wa.starter.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.wa.starter.actuate.SyncopeWAInfoContributor;
import org.apache.syncope.wa.starter.audit.WAAuditTrailManager;
import org.apache.syncope.wa.starter.events.WAEventRepository;
import org.apache.syncope.wa.starter.gauth.WAGoogleMfaAuthCredentialRepository;
import org.apache.syncope.wa.starter.gauth.WAGoogleMfaAuthTokenRepository;
import org.apache.syncope.wa.starter.mapping.AccessMapper;
import org.apache.syncope.wa.starter.mapping.AuthMapper;
import org.apache.syncope.wa.starter.mapping.CASSPClientAppTOMapper;
import org.apache.syncope.wa.starter.mapping.ClientAppMapper;
import org.apache.syncope.wa.starter.mapping.DefaultAccessMapper;
import org.apache.syncope.wa.starter.mapping.DefaultAuthMapper;
import org.apache.syncope.wa.starter.mapping.DefaultTicketExpirationMapper;
import org.apache.syncope.wa.starter.mapping.HttpRequestAccessMapper;
import org.apache.syncope.wa.starter.mapping.OIDCRPClientAppTOMapper;
import org.apache.syncope.wa.starter.mapping.OpenFGAAccessMapper;
import org.apache.syncope.wa.starter.mapping.RegisteredServiceMapper;
import org.apache.syncope.wa.starter.mapping.RemoteEndpointAccessMapper;
import org.apache.syncope.wa.starter.mapping.SAML2SPClientAppTOMapper;
import org.apache.syncope.wa.starter.mapping.TicketExpirationMapper;
import org.apache.syncope.wa.starter.mapping.TimeBasedAccessMapper;
import org.apache.syncope.wa.starter.mfa.WAMultifactorAuthenticationTrustStorage;
import org.apache.syncope.wa.starter.oidc.WAOIDCJWKSGeneratorService;
import org.apache.syncope.wa.starter.pac4j.saml.WASAML2ClientCustomizer;
import org.apache.syncope.wa.starter.saml.idp.metadata.WASamlIdPMetadataGenerator;
import org.apache.syncope.wa.starter.saml.idp.metadata.WASamlIdPMetadataLocator;
import org.apache.syncope.wa.starter.services.WAServiceRegistry;
import org.apache.syncope.wa.starter.surrogate.WASurrogateAuthenticationService;
import org.apache.syncope.wa.starter.webauthn.WAWebAuthnCredentialRepository;
import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.gauth.LdapGoogleAuthenticatorMultifactorProperties;
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
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustRecordKeyGenerator;
import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustStorage;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.spring.CasApplicationReadyListener;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import org.ldaptive.ConnectionFactory;
import org.pac4j.core.client.Client;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

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

    @ConditionalOnMissingBean
    @Bean
    public AccessMapper defaultAccessMapper() {
        return new DefaultAccessMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public HttpRequestAccessMapper httpRequestAccessMapper() {
        return new HttpRequestAccessMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public RemoteEndpointAccessMapper remoteEndpointAccessMapper() {
        return new RemoteEndpointAccessMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public TimeBasedAccessMapper timeBasedAccessMapper() {
        return new TimeBasedAccessMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenFGAAccessMapper openFGAAccessMapper() {
        return new OpenFGAAccessMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthMapper authMapper() {
        return new DefaultAuthMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public TicketExpirationMapper ticketExpirationMapper() {
        return new DefaultTicketExpirationMapper();
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
            final CasConfigurationProperties casProperties,
            final ObjectProvider<AuthenticationEventExecutionPlan> authenticationEventExecutionPlan,
            final List<MultifactorAuthenticationProvider> multifactorAuthenticationProviders,
            final List<AuthMapper> authMappers,
            final List<AccessMapper> accessMappers,
            final List<AttrReleaseMapper> attrReleaseMappers,
            final List<TicketExpirationMapper> ticketExpirationMappers,
            final List<ClientAppMapper> clientAppMappers) {

        return new RegisteredServiceMapper(
                Optional.ofNullable(casProperties.getAuthn().getPac4j().getCore().getName()).
                    orElseGet(DelegatedClientAuthenticationHandler.class::getSimpleName),
                authenticationEventExecutionPlan,
                multifactorAuthenticationProviders,
                authMappers,
                accessMappers,
                attrReleaseMappers,
                ticketExpirationMappers,
                clientAppMappers);
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean
    @Bean
    public ServiceRegistryExecutionPlanConfigurer syncopeServiceRegistryConfigurer(
            final ConfigurableApplicationContext ctx,
            final WARestClient waRestClient,
            final RegisteredServiceMapper registeredServiceMapper,
            @Qualifier("serviceRegistryListeners")
            final ObjectProvider<List<ServiceRegistryListener>> serviceRegistryListeners) {

        WAServiceRegistry registry = new WAServiceRegistry(
                waRestClient, registeredServiceMapper, ctx,
                Optional.ofNullable(serviceRegistryListeners.getIfAvailable()).orElseGet(ArrayList::new));
        return plan -> plan.registerServiceRegistry(registry);
    }

    @Bean
    public CasApplicationReadyListener samlIdPCasEventListener() {
        // skip generating IdP metadata at this stage, as the default samlIdPCasEventListener bean is doing
        return event -> {
        };
    }

    @Bean
    public SamlIdPMetadataGenerator samlIdPMetadataGenerator(
            final WARestClient waRestClient,
            final SamlIdPMetadataGeneratorConfigurationContext context) {

        return new WASamlIdPMetadataGenerator(context, waRestClient);
    }

    @Bean
    public SamlIdPMetadataLocator samlIdPMetadataLocator(
            @Qualifier("samlIdPMetadataGeneratorCipherExecutor")
            final CipherExecutor<String, String> cipherExecutor,
            @Qualifier("samlIdPMetadataCache")
            final Cache<String, SamlIdPMetadataDocument> samlIdPMetadataCache,
            final ApplicationContext applicationContext,
            final WARestClient waRestClient) {

        return new WASamlIdPMetadataLocator(
                cipherExecutor,
                samlIdPMetadataCache,
                applicationContext,
                waRestClient);
    }

    @Bean
    public AuditTrailExecutionPlanConfigurer auditConfigurer(final WARestClient waRestClient) {
        return plan -> plan.registerAuditTrailManager(new WAAuditTrailManager(waRestClient));
    }

    @ConditionalOnMissingBean
    @Bean
    public CasEventRepositoryFilter syncopeWAEventRepositoryFilter() {
        return CasEventRepositoryFilter.noOp();
    }

    @Bean
    public CasEventRepository casEventRepository(
            final WARestClient waRestClient,
            @Qualifier("syncopeWAEventRepositoryFilter")
            final CasEventRepositoryFilter syncopeWAEventRepositoryFilter) {

        return new WAEventRepository(syncopeWAEventRepositoryFilter, waRestClient);
    }

    @Bean
    public DelegatedClientFactoryCustomizer<Client> delegatedClientCustomizer(final WARestClient waRestClient) {
        return new WASAML2ClientCustomizer(waRestClient);
    }

    @Bean
    public WAGoogleMfaAuthTokenRepository oneTimeTokenAuthenticatorTokenRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient waRestClient) {

        return new WAGoogleMfaAuthTokenRepository(
                waRestClient, casProperties.getAuthn().getMfa().getGauth().getCore().getTimeStepSize());
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
            final WARestClient waRestClient) {

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
        return new WAGoogleMfaAuthCredentialRepository(waRestClient, googleAuthenticatorInstance);
    }

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean(name = MultifactorAuthenticationTrustStorage.BEAN_NAME)
    public MultifactorAuthenticationTrustStorage mfaTrustStorage(
            final CasConfigurationProperties casProperties,
            @Qualifier("mfaTrustRecordKeyGenerator")
            final MultifactorAuthenticationTrustRecordKeyGenerator keyGenerationStrategy,
            @Qualifier("mfaTrustCipherExecutor")
            final CipherExecutor<Serializable, String> mfaTrustCipherExecutor,
            final WARestClient waRestClient) {

        return new WAMultifactorAuthenticationTrustStorage(
                casProperties.getAuthn().getMfa().getTrusted(),
                mfaTrustCipherExecutor,
                keyGenerationStrategy,
                waRestClient);
    }

    @Bean
    public OidcJsonWebKeystoreGeneratorService oidcJsonWebKeystoreGeneratorService(
            final CasConfigurationProperties casProperties,
            final WARestClient waRestClient) {

        return new WAOIDCJWKSGeneratorService(
                waRestClient,
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksKeyId(),
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksType(),
                casProperties.getAuthn().getOidc().getJwks().getCore().getJwksKeySize());
    }

    @Bean
    public WebAuthnCredentialRepository webAuthnCredentialRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient waRestClient) {

        return new WAWebAuthnCredentialRepository(casProperties, waRestClient);
    }

    @Bean
    public SurrogateAuthenticationService surrogateAuthenticationService(final WARestClient waRestClient) {
        return new WASurrogateAuthenticationService(waRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(final WARestClient waRestClient) {
        return new SyncopeCoreHealthIndicator(waRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeWAInfoContributor syncopeWAInfoContributor(final WAProperties waProperties) {
        return new SyncopeWAInfoContributor(waProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDetailsService actuatorUserDetailsService(final WAProperties waProperties) {
        UserDetails user = User.withUsername(waProperties.getAnonymousUser()).
                password("{noop}" + waProperties.getAnonymousKey()).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new InMemoryUserDetailsManager(user);
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.WA);
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.WA);
    }
}
