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
package org.apache.syncope.wa.starter;

import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryExecutionPlanConfigurer;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.wa.WARestClient;

import org.apereo.cas.support.pac4j.authentication.DelegatedClientFactoryCustomizer;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.idp.metadata.locator.SamlIdPMetadataLocator;

import org.apache.syncope.wa.pac4j.saml.SyncopeWASAML2ClientCustomizer;
import org.pac4j.core.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Collection;
import org.apache.syncope.wa.saml.idp.metadata.RestfulSamlIdPMetadataGenerator;
import org.apache.syncope.wa.saml.idp.metadata.RestfulSamlIdPMetadataLocator;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGeneratorConfigurationContext;
import org.apereo.cas.support.saml.idp.metadata.writer.SamlIdPCertificateAndKeyWriter;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class SyncopeWAConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    @Qualifier("samlSelfSignedCertificateWriter")
    private ObjectProvider<SamlIdPCertificateAndKeyWriter> samlSelfSignedCertificateWriter;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("serviceRegistryListeners")
    private Collection<ServiceRegistryListener> serviceRegistryListeners;

    @Autowired
    @Bean
    public ServiceRegistryExecutionPlanConfigurer syncopeServiceRegistryConfigurer(final WARestClient restClient) {
        SyncopeServiceRegistry registry =
                new SyncopeServiceRegistry(restClient, applicationContext, serviceRegistryListeners);
        return plan -> plan.registerServiceRegistry(registry);
    }

    @Autowired
    @Bean
    public SamlIdPMetadataGenerator samlIdPMetadataGenerator(final WARestClient restClient) {
        SamlIdPMetadataGeneratorConfigurationContext context =
                SamlIdPMetadataGeneratorConfigurationContext.builder().
                        samlIdPMetadataLocator(samlIdPMetadataLocator(restClient)).
                        samlIdPCertificateAndKeyWriter(samlSelfSignedCertificateWriter.getObject()).
                        resourceLoader(resourceLoader).
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
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.WA);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.WA);
    }

}
