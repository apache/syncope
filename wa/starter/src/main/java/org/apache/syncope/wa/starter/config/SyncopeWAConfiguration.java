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
import org.apache.syncope.common.lib.types.JWSAlgorithm;
import org.apache.syncope.wa.bootstrap.WAProperties;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.wa.starter.actuate.SyncopeWAInfoContributor;
import org.apache.syncope.wa.starter.audit.SyncopeWAAuditTrailManager;
import org.apache.syncope.wa.starter.events.SyncopeWAEventRepository;
import org.apache.syncope.wa.starter.gauth.SyncopeWAGoogleMfaAuthCredentialRepository;
import org.apache.syncope.wa.starter.gauth.SyncopeWAGoogleMfaAuthTokenRepository;
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
import org.apache.syncope.wa.starter.oidc.SyncopeWAOIDCJWKSGeneratorService;
import org.apache.syncope.wa.starter.pac4j.saml.SyncopeWASAML2ClientCustomizer;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataGenerator;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataLocator;
import org.apache.syncope.wa.starter.services.SyncopeWAServiceRegistry;
import org.apache.syncope.wa.starter.surrogate.SyncopeWASurrogateAuthenticationService;
import org.apache.syncope.wa.starter.u2f.SyncopeWAU2FDeviceRepository;
import org.apache.syncope.wa.starter.webauthn.SyncopeWAWebAuthnCredentialRepository;
import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRepository;
import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.authentication.surrogate.SurrogateAuthenticationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FCoreMultifactorAuthenticationProperties;
import org.apereo.cas.oidc.jwks.generator.OidcJsonWebKeystoreGeneratorService;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.token.OneTimeTokenRepository;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.events.CasEventRepository;
import org.apereo.cas.support.events.CasEventRepositoryFilter;
import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactoryCustomizer;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import org.pac4j.core.client.Client;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SyncopeWAConfiguration {

    private static String version(final ConfigurableApplicationContext ctx) {
        return ctx.getEnvironment().getProperty("version");
    }

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

    @ConditionalOnMissingBean
    @Bean
    public RegisteredServiceMapper registeredServiceMapper(final ConfigurableApplicationContext ctx) {
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
                authPolicyConfMappers,
                accessPolicyConfMappers,
                attrReleasePolicyConfMappers,
                clientAppTOMappers);
    }

    @Bean
    public ServiceRegistryExecutionPlanConfigurer syncopeServiceRegistryConfigurer(
            final ConfigurableApplicationContext ctx,
            final WARestClient restClient,
            final RegisteredServiceMapper registeredServiceMapper,
            @Qualifier("serviceRegistryListeners")
            final ObjectProvider<List<ServiceRegistryListener>> serviceRegistryListeners) {

        SyncopeWAServiceRegistry registry = new SyncopeWAServiceRegistry(
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
        return plan -> plan.registerAuditTrailManager(new SyncopeWAAuditTrailManager(restClient));
    }

    @ConditionalOnMissingBean(name = "syncopeWaEventRepositoryFilter")
    @Bean
    public CasEventRepositoryFilter syncopeWAEventRepositoryFilter() {
        return CasEventRepositoryFilter.noOp();
    }

    @Bean
    public CasEventRepository casEventRepository(final WARestClient restClient,
            @Qualifier("syncopeWAEventRepositoryFilter")
            final CasEventRepositoryFilter syncopeWAEventRepositoryFilter) {
        return new SyncopeWAEventRepository(syncopeWAEventRepositoryFilter, restClient);
    }

    @Bean
    public DelegatedClientFactoryCustomizer<Client> delegatedClientCustomizer(final WARestClient restClient) {
        return new SyncopeWASAML2ClientCustomizer(restClient);
    }

    @Bean
    public OneTimeTokenRepository oneTimeTokenAuthenticatorTokenRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {
        return new SyncopeWAGoogleMfaAuthTokenRepository(
                restClient, casProperties.getAuthn().getMfa().getGauth().getCore().getTimeStepSize());
    }

    @Bean
    public OneTimeTokenCredentialRepository googleAuthenticatorAccountRegistry(
            final IGoogleAuthenticator googleAuthenticatorInstance, final WARestClient restClient) {

        return new SyncopeWAGoogleMfaAuthCredentialRepository(restClient, googleAuthenticatorInstance);
    }

    @Bean
    public OidcJsonWebKeystoreGeneratorService oidcJsonWebKeystoreGeneratorService(
            final ConfigurableApplicationContext ctx,
            final WARestClient restClient) {
        int size = ctx.getEnvironment().
                getProperty("cas.authn.oidc.jwks.size", int.class, 2048);
        JWSAlgorithm algorithm = ctx.getEnvironment().
                getProperty("cas.authn.oidc.jwks.algorithm", JWSAlgorithm.class, JWSAlgorithm.RS256);
        return new SyncopeWAOIDCJWKSGeneratorService(restClient, size, algorithm);
    }

    @Bean
    public WebAuthnCredentialRepository webAuthnCredentialRepository(
            final CasConfigurationProperties casProperties,
            final WARestClient restClient) {
        return new SyncopeWAWebAuthnCredentialRepository(casProperties, restClient);
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
        return new SyncopeWAU2FDeviceRepository(casProperties, requestStorage, restClient, expirationDate);
    }

    @Bean
    public SurrogateAuthenticationService surrogateAuthenticationService(final WARestClient restClient) {
        return new SyncopeWASurrogateAuthenticationService(restClient);
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
