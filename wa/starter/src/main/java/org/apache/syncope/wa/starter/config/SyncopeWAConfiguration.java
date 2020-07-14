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

import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRepository;
import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.u2f.U2FMultifactorProperties;
import org.apereo.cas.oidc.jwks.OidcJsonWebKeystoreGeneratorService;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.token.OneTimeTokenRepository;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactoryCustomizer;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;
import org.apereo.cas.support.saml.idp.metadata.writer.SamlIdPCertificateAndKeyWriter;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.util.crypto.CipherExecutor;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.audit.SyncopeWAAuditTrailManager;
import org.apache.syncope.wa.starter.gauth.credential.SyncopeWAGoogleMfaAuthCredentialRepository;
import org.apache.syncope.wa.starter.gauth.token.SyncopeWAGoogleMfaAuthTokenRepository;
import org.apache.syncope.wa.starter.mapping.AccessMapFor;
import org.apache.syncope.wa.starter.mapping.AccessMapper;
import org.apache.syncope.wa.starter.mapping.AttrReleaseMapFor;
import org.apache.syncope.wa.starter.mapping.AttrReleaseMapper;
import org.apache.syncope.wa.starter.mapping.AuthMapFor;
import org.apache.syncope.wa.starter.mapping.AuthMapper;
import org.apache.syncope.wa.starter.mapping.ClientAppMapFor;
import org.apache.syncope.wa.starter.mapping.ClientAppMapper;
import org.apache.syncope.wa.starter.mapping.RegisteredServiceMapper;
import org.apache.syncope.wa.starter.oidc.SyncopeWAOIDCJWKSGeneratorService;
import org.apache.syncope.wa.starter.pac4j.saml.SyncopeWASAML2ClientCustomizer;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataGenerator;
import org.apache.syncope.wa.starter.saml.idp.metadata.RestfulSamlIdPMetadataLocator;
import org.apache.syncope.wa.starter.services.SyncopeWAServiceRegistry;
import org.apache.syncope.wa.starter.u2f.SyncopeWAU2FDeviceRepository;
import org.pac4j.core.client.Client;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SyncopeWAConfiguration {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("samlSelfSignedCertificateWriter")
    private ObjectProvider<SamlIdPCertificateAndKeyWriter> samlSelfSignedCertificateWriter;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("serviceRegistryListeners")
    private Collection<ServiceRegistryListener> serviceRegistryListeners;

    @ConditionalOnMissingBean
    @Bean
    public RegisteredServiceMapper registeredServiceMapper() {
        Map<String, AuthMapper> authPolicyConfMappers = new HashMap<>();
        applicationContext.getBeansOfType(AuthMapper.class).forEach((name, bean) -> {
            AuthMapFor authMapFor = applicationContext.findAnnotationOnBean(name, AuthMapFor.class);
            if (authMapFor != null) {
                authPolicyConfMappers.put(authMapFor.authPolicyConfClass().getName(), bean);
            }
        });

        Map<String, AccessMapper> accessPolicyConfMappers = new HashMap<>();
        applicationContext.getBeansOfType(AccessMapper.class).forEach((name, bean) -> {
            AccessMapFor accessMapFor = applicationContext.findAnnotationOnBean(name, AccessMapFor.class);
            if (accessMapFor != null) {
                accessPolicyConfMappers.put(accessMapFor.accessPolicyConfClass().getName(), bean);
            }
        });

        Map<String, AttrReleaseMapper> attrReleasePolicyConfMappers = new HashMap<>();
        applicationContext.getBeansOfType(AttrReleaseMapper.class).forEach((name, bean) -> {
            AttrReleaseMapFor attrReleaseMapFor =
                applicationContext.findAnnotationOnBean(name, AttrReleaseMapFor.class);
            if (attrReleaseMapFor != null) {
                attrReleasePolicyConfMappers.put(attrReleaseMapFor.attrReleasePolicyConfClass().getName(), bean);
            }
        });

        Map<String, ClientAppMapper> clientAppTOMappers = new HashMap<>();
        applicationContext.getBeansOfType(ClientAppMapper.class).forEach((name, bean) -> {
            ClientAppMapFor clientAppMapFor = applicationContext.findAnnotationOnBean(name, ClientAppMapFor.class);
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

    @Autowired
    @Bean
    public ServiceRegistryExecutionPlanConfigurer syncopeServiceRegistryConfigurer(
        final WARestClient restClient, final RegisteredServiceMapper registeredServiceMapper) {

        SyncopeWAServiceRegistry registry = new SyncopeWAServiceRegistry(
            restClient, registeredServiceMapper, applicationContext, serviceRegistryListeners);
        return plan -> plan.registerServiceRegistry(registry);
    }

    @Autowired
    @Bean
    public SamlIdPMetadataGenerator samlIdPMetadataGenerator(final WARestClient restClient) {
        SamlIdPMetadataGeneratorConfigurationContext context =
            SamlIdPMetadataGeneratorConfigurationContext.builder().
                samlIdPMetadataLocator(samlIdPMetadataLocator(restClient)).
                samlIdPCertificateAndKeyWriter(samlSelfSignedCertificateWriter.getObject()).
                applicationContext(applicationContext).
                casProperties(casProperties).
                metadataCipherExecutor(CipherExecutor.noOpOfStringToString()).
                build();
        return new RestfulSamlIdPMetadataGenerator(context, restClient);
    }

    @Autowired
    @Bean
    public SamlIdPMetadataLocator samlIdPMetadataLocator(final WARestClient restClient) {
        return new RestfulSamlIdPMetadataLocator(CipherExecutor.noOpOfStringToString(), restClient);
    }

    @Bean
    @Autowired
    public AuditTrailExecutionPlanConfigurer auditConfigurer(final WARestClient restClient) {
        return plan -> plan.registerAuditTrailManager(new SyncopeWAAuditTrailManager(restClient));
    }

    @Autowired
    @Bean
    public DelegatedClientFactoryCustomizer<Client> delegatedClientCustomizer(final WARestClient restClient) {
        return new SyncopeWASAML2ClientCustomizer(restClient);
    }

    @Bean
    @Autowired
    public OneTimeTokenRepository oneTimeTokenAuthenticatorTokenRepository(final WARestClient restClient) {
        return new SyncopeWAGoogleMfaAuthTokenRepository(restClient,
            casProperties.getAuthn().getMfa().getGauth().getTimeStepSize());
    }

    @Bean
    @Autowired
    public OneTimeTokenCredentialRepository googleAuthenticatorAccountRegistry(
        final IGoogleAuthenticator googleAuthenticatorInstance, final WARestClient restClient) {
        return new SyncopeWAGoogleMfaAuthCredentialRepository(restClient, googleAuthenticatorInstance);
    }

    @Bean
    @Autowired
    public OidcJsonWebKeystoreGeneratorService oidcJsonWebKeystoreGeneratorService(final WARestClient restClient) {
        return new SyncopeWAOIDCJWKSGeneratorService(restClient);
    }

    @Bean
    public OpenAPI casSwaggerOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Apache Syncope")
                .description("Apache Syncope " + version())
                .contact(new Contact()
                    .name("The Apache Syncope community")
                    .email("dev@syncope.apache.org")
                    .url("http://syncope.apache.org"))
                .version(version()));
    }

    @Bean
    @Autowired
    @RefreshScope
    public U2FDeviceRepository u2fDeviceRepository(final WARestClient restClient) {
        U2FMultifactorProperties u2f = casProperties.getAuthn().getMfa().getU2f();
        final LocalDate expirationDate = LocalDate.now(ZoneId.systemDefault())
            .minus(u2f.getExpireDevices(), DateTimeUtils.toChronoUnit(u2f.getExpireDevicesTimeUnit()));
        final LoadingCache<String, String> requestStorage = Caffeine.newBuilder()
            .expireAfterWrite(u2f.getExpireRegistrations(), u2f.getExpireRegistrationsTimeUnit())
            .build(key -> StringUtils.EMPTY);
        return new SyncopeWAU2FDeviceRepository(requestStorage, restClient, expirationDate);
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.WA);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.WA);
    }

    @Bean
    public String version() {
        return applicationContext.getEnvironment().getProperty("version");
    }

    @Bean
    public String buildNumber() {
        return applicationContext.getEnvironment().getProperty("buildNumber");
    }
}
