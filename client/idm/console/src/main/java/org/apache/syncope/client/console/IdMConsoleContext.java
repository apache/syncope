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
package org.apache.syncope.client.console;

import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdMAnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.IdMAnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.IdMAnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.IdMExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdMImplementationInfoProvider;
import org.apache.syncope.client.console.commons.IdMPolicyTabProvider;
import org.apache.syncope.client.console.commons.IdMStatusProvider;
import org.apache.syncope.client.console.commons.IdMVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationContributor;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.IdMClassPathScanImplementationContributor;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.console.rest.RemediationRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.status.ReconStatusUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdMConsoleContext {

    @Bean
    public ClassPathScanImplementationContributor idmClassPathScanImplementationContributor() {
        return new IdMClassPathScanImplementationContributor();
    }

    @Bean
    public ExternalResourceProvider resourceProvider(final ResourceRestClient resourceRestClient) {
        return new IdMExternalResourceProvider(resourceRestClient);
    }

    @Bean
    public AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider(
            final ReconciliationRestClient reconciliationRestClient,
            final ImplementationRestClient implementationRestClient) {

        return new IdMAnyDirectoryPanelAdditionalActionsProvider(reconciliationRestClient, implementationRestClient);
    }

    @Bean
    public AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider(
            final ResourceRestClient resourceRestClient,
            final UserRestClient userRestClient) {

        return new IdMAnyDirectoryPanelAdditionalActionLinksProvider(resourceRestClient, userRestClient);
    }

    @Bean
    public AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps() {
        return new IdMAnyWizardBuilderAdditionalSteps();
    }

    @Bean
    public StatusProvider statusProvider(final ReconStatusUtils reconStatusUtils) {
        return new IdMStatusProvider(reconStatusUtils);
    }

    @Bean
    public VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider() {
        return new IdMVirSchemaDetailsPanelProvider();
    }

    @Bean
    public ImplementationInfoProvider implementationInfoProvider(
            final ClassPathScanImplementationLookup lookup,
            final ImplementationRestClient implementationRestClient) {

        return new IdMImplementationInfoProvider(lookup, implementationRestClient);
    }

    @Bean
    public PolicyTabProvider idmPolicyTabProvider(final PolicyRestClient policyRestClient) {
        return new IdMPolicyTabProvider(policyRestClient);
    }

    @Bean
    public ReconStatusUtils reconStatusUtils(final ReconciliationRestClient reconciliationRestClient) {
        return new ReconStatusUtils(reconciliationRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnectorRestClient connectorRestClient() {
        return new ConnectorRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReconciliationRestClient reconciliationRestClient() {
        return new ReconciliationRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationRestClient remediationRestClient() {
        return new RemediationRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public ResourceRestClient resourceRestClient() {
        return new ResourceRestClient();
    }
}
