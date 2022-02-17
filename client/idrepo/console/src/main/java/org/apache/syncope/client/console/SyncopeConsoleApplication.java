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
import java.util.Map;
import org.apache.syncope.client.console.actuate.SyncopeConsoleInfoContributor;
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
import org.apache.syncope.client.console.wizards.any.UserFormFinalizerUtils;
import org.apache.syncope.client.ui.commons.ApplicationContextProvider;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.client.ui.commons.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
    ErrorMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class }, proxyBeanMethods = false)
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

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(final ServiceOps serviceOps,
                                                                 final ConsoleProperties props) {
        return new SyncopeCoreHealthIndicator(
                serviceOps,
                props.getAnonymousUser(),
                props.getAnonymousKey(),
                props.isUseGZIPCompression());
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeConsoleInfoContributor syncopeConsoleInfoContributor() {
        return new SyncopeConsoleInfoContributor();
    }

    @ConditionalOnMissingBean(name = "classPathScanImplementationLookup")
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup(final ApplicationContext ctx,
                                                                               final ConsoleProperties props) {
        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup(
                ctx.getBeansOfType(ClassPathScanImplementationContributor.class).values(), props);
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

    @ConditionalOnMissingBean(name = "previewUtils")
    @Bean
    public PreviewUtils previewUtils() {
        return new PreviewUtils();
    }

    @ConditionalOnMissingBean(name = "userFormFinalizerUtils")
    @Bean
    public UserFormFinalizerUtils userFormFinalizerUtils() {
        return new UserFormFinalizerUtils();
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

    @ConditionalOnMissingBean(name = "realmPolicyProvider")
    @Bean
    public RealmPolicyProvider realmPolicyProvider() {
        return new IdRepoRealmPolicyProvider();
    }

    @Bean
    public PolicyTabProvider idRepoPolicyTabProvider() {
        return new IdRepoPolicyTabProvider();
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.CONSOLE);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.CONSOLE);
    }
}
