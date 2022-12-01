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
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.IdRepoExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdRepoImplementationInfoProvider;
import org.apache.syncope.client.console.commons.IdRepoPolicyTabProvider;
import org.apache.syncope.client.console.commons.IdRepoRealmPolicyProvider;
import org.apache.syncope.client.console.commons.IdRepoStatusProvider;
import org.apache.syncope.client.console.commons.IdRepoVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.PreviewUtils;
import org.apache.syncope.client.console.commons.RealmPolicyProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationContributor;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConsoleContext {

    @ConditionalOnMissingBean
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup(
            final ApplicationContext ctx, final ConsoleProperties props) {

        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup(
                ctx.getBeansOfType(ClassPathScanImplementationContributor.class).values(), props);
        lookup.load();
        return lookup;
    }

    @ConditionalOnMissingBean
    @Bean
    public MIMETypesLoader mimeTypesLoader() {
        MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
        mimeTypesLoader.load();
        return mimeTypesLoader;
    }

    @ConditionalOnMissingBean
    @Bean
    public PreviewUtils previewUtils() {
        return new PreviewUtils();
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceProvider resourceProvider() {
        return new IdRepoExternalResourceProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionsProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionLinksProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps() {
        return new IdRepoAnyWizardBuilderAdditionalSteps();
    }

    @ConditionalOnMissingBean
    @Bean
    public StatusProvider statusProvider() {
        return new IdRepoStatusProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider() {
        return new IdRepoVirSchemaDetailsPanelProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationInfoProvider implementationInfoProvider() {
        return new IdRepoImplementationInfoProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmPolicyProvider realmPolicyProvider() {
        return new IdRepoRealmPolicyProvider();
    }

    @Bean
    public PolicyTabProvider idRepoPolicyTabProvider() {
        return new IdRepoPolicyTabProvider();
    }
}
