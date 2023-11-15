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
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.commons.AccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.wizards.any.UserFormFinalizer;
import org.apache.syncope.client.ui.commons.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.wicket.request.resource.IResource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsoleProperties.class)
public class SyncopeConsoleApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeConsoleApplication.class).
                properties("spring.config.name:console").
                build().run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of(
                WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED, false,
                "spring.config.name", "console")).
                sources(SyncopeConsoleApplication.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeWebApplication syncopeWebApplication(
            final ConsoleProperties props,
            final ClassPathScanImplementationLookup lookup,
            final ServiceOps serviceOps,
            final ExternalResourceProvider resourceProvider,
            final AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider,
            final AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider,
            final AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps,
            final StatusProvider statusProvider,
            final VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider,
            final ImplementationInfoProvider implementationInfoProvider,
            final AccessPolicyConfProvider accessPolicyConfProvider,
            final List<PolicyTabProvider> policyTabProviders,
            final List<UserFormFinalizer> userFormFinalizers,
            final List<IResource> resources) {

        return new SyncopeWebApplication(
                props,
                lookup,
                serviceOps,
                resourceProvider,
                anyDirectoryPanelAdditionalActionsProvider,
                anyDirectoryPanelAdditionalActionLinksProvider,
                anyWizardBuilderAdditionalSteps, statusProvider,
                virSchemaDetailsPanelProvider,
                implementationInfoProvider,
                accessPolicyConfProvider,
                policyTabProviders,
                userFormFinalizers,
                resources);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(
            final ServiceOps serviceOps, final ConsoleProperties props) {

        return new SyncopeCoreHealthIndicator(
                serviceOps,
                props.getAnonymousUser(),
                props.getAnonymousKey(),
                props.isUseGZIPCompression());
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.CONSOLE);
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.CONSOLE);
    }
}
