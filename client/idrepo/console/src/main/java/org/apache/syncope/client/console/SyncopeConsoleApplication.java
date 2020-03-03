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

import com.giffing.wicket.spring.boot.starter.web.config.WicketWebInitializerAutoConfig.WebSocketWicketWebInitializerAutoConfiguration;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.IdRepoExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdRepoImplementationInfoProvider;
import org.apache.syncope.client.console.commons.IdRepoPolicyTabProvider;
import org.apache.syncope.client.console.commons.IdRepoStatusProvider;
import org.apache.syncope.client.console.commons.IdRepoVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
    ErrorMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class })
public class SyncopeConsoleApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeConsoleApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        builder.properties(WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED + "=false");
        return super.configure(builder);
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.CONSOLE);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.CONSOLE);
    }

    @ConditionalOnMissingBean(name = "classPathScanImplementationLookup")
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup() {
        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
        lookup.load();
        return lookup;
    }

    @ConditionalOnMissingBean(name = "mimeTypesLoader")
    @Bean
    public MIMETypesLoader mimeTypesLoader() {
        MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
        mimeTypesLoader.load();
        return mimeTypesLoader;
    }

    @ConditionalOnMissingBean(name = "resourceProvider")
    @Bean
    public ExternalResourceProvider resourceProvider() {
        return new IdRepoExternalResourceProvider();
    }

    @ConditionalOnMissingBean(name = "anyDirectoryPanelAdditionalActionsProvider")
    @Bean
    public AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionsProvider();
    }

    @ConditionalOnMissingBean(name = "anyDirectoryPanelAdditionalActionLinksProvider")
    @Bean
    public AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionLinksProvider();
    }

    @ConditionalOnMissingBean(name = "anyWizardBuilderAdditionalSteps")
    @Bean
    public AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps() {
        return new IdRepoAnyWizardBuilderAdditionalSteps();
    }

    @ConditionalOnMissingBean(name = "statusProvider")
    @Bean
    public StatusProvider statusProvider() {
        return new IdRepoStatusProvider();
    }

    @ConditionalOnMissingBean(name = "virSchemaDetailsPanelProvider")
    @Bean
    public VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider() {
        return new IdRepoVirSchemaDetailsPanelProvider();
    }

    @ConditionalOnMissingBean(name = "implementationInfoProvider")
    @Bean
    public ImplementationInfoProvider implementationInfoProvider() {
        return new IdRepoImplementationInfoProvider();
    }

    @ConditionalOnMissingBean(name = "policyTabProvider")
    @Bean
    public PolicyTabProvider policyTabProvider() {
        return new IdRepoPolicyTabProvider();
    }
}
