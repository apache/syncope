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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;
import com.giffing.wicket.spring.boot.context.extensions.boot.actuator.WicketEndpointRepository;
import com.giffing.wicket.spring.boot.starter.app.classscanner.candidates.WicketClassCandidatesHolder;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.settings.general.GeneralSettingsProperties;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.boot.actuator.WicketEndpointRepositoryDefault;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.syncope.client.console.AbstractTest.TestSyncopeWebApplication.SyncopeServiceClient;
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
import org.apache.syncope.client.console.commons.IdRepoStatusProvider;
import org.apache.syncope.client.console.commons.IdRepoVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.PreviewUtils;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.wizards.any.UserFormFinalizerUtils;
import org.apache.syncope.client.lib.AuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public abstract class AbstractTest {

    @ImportAutoConfiguration
    @Configuration(proxyBeanMethods = false)
    public static class SyncopeConsoleWebApplicationTestConfig {

        @Bean
        public ConsoleProperties consoleProperties() {
            ConsoleProperties consoleProperties = new ConsoleProperties();

            consoleProperties.getSecurityHeaders().put("X-XSS-Protection", "1; mode=block");
            consoleProperties.getSecurityHeaders().
                    put("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            consoleProperties.getSecurityHeaders().put("X-Content-Type-Options", "nosniff");
            consoleProperties.getSecurityHeaders().put("X-Frame-Options", "sameorigin");

            consoleProperties.setAnonymousUser("anonymousUser");

            return consoleProperties;
        }

        @Bean
        public ServiceOps selfServiceOps() {
            return mock(ServiceOps.class);
        }

        @Bean
        public DomainOps domainOps() {
            DomainOps domainOps = mock(DomainOps.class);
            when(domainOps.list()).thenReturn(List.of(new Domain.Builder(SyncopeConstants.MASTER_DOMAIN).build()));
            return domainOps;
        }

        @Bean
        public GeneralSettingsProperties generalSettingsProperties() {
            return new GeneralSettingsProperties();
        }

        @Bean
        public List<WicketApplicationInitConfiguration> configurations() {
            return List.of();
        }

        @Bean
        public WicketClassCandidatesHolder wicketClassCandidatesHolder() {
            return new WicketClassCandidatesHolder();
        }

        @Bean
        public WicketEndpointRepository wicketEndpointRepository() {
            return new WicketEndpointRepositoryDefault();
        }

        @Bean
        public ClassPathScanImplementationLookup classPathScanImplementationLookup() {
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup(Set.of());
            lookup.load();
            return lookup;
        }

        @Bean
        public MIMETypesLoader mimeTypesLoader() {
            MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
            mimeTypesLoader.load();
            return mimeTypesLoader;
        }

        @Bean
        public PreviewUtils previewUtils() {
            return new PreviewUtils();
        }

        @Bean
        public UserFormFinalizerUtils userFormFinalizerUtils() {
            return new UserFormFinalizerUtils();
        }

        @Bean
        public ExternalResourceProvider resourceProvider() {
            return new IdRepoExternalResourceProvider();
        }

        @Bean
        public AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider() {
            return new IdRepoAnyDirectoryPanelAdditionalActionsProvider();
        }

        @Bean
        public AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAditionalActionLinksProvider() {
            return new IdRepoAnyDirectoryPanelAdditionalActionLinksProvider();
        }

        @Bean
        public AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps() {
            return new IdRepoAnyWizardBuilderAdditionalSteps();
        }

        @Bean
        public StatusProvider statusProvider() {
            return new IdRepoStatusProvider();
        }

        @Bean
        public VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider() {
            return new IdRepoVirSchemaDetailsPanelProvider();
        }

        @Bean
        public ImplementationInfoProvider implementationInfoProvider() {
            return new IdRepoImplementationInfoProvider();
        }

        @Bean
        public PolicyTabProvider policyTabProvider() {
            return new IdRepoPolicyTabProvider();
        }
    }

    public static class TestSyncopeWebApplication extends SyncopeWebApplication {

        public interface SyncopeServiceClient extends SyncopeService, Client {
        }

        public interface AnyTypeServiceClient extends AnyTypeService, Client {
        }

        public interface SchemaServiceClient extends SchemaService, Client {
        }

        private SyncopeService getSyncopeService() {
            SyncopeServiceClient service = mock(SyncopeServiceClient.class);
            when(service.type(anyString())).thenReturn(service);
            when(service.accept(anyString())).thenReturn(service);

            return service;
        }

        private SchemaService getSchemaService() {
            SchemaServiceClient service = mock(SchemaServiceClient.class);

            when(service.type(anyString())).thenReturn(service);
            when(service.accept(anyString())).thenReturn(service);

            PlainSchemaTO firstname = new PlainSchemaTO();
            firstname.setKey("firstname");
            firstname.setType(AttrSchemaType.String);
            firstname.setAnyTypeClass("minimal user");
            firstname.setMandatoryCondition("false");
            when(service.search(any(SchemaQuery.class))).thenReturn(List.of(firstname));
            return service;
        }

        private AnyTypeService getAnyTypeService() {
            AnyTypeServiceClient service = mock(AnyTypeServiceClient.class);

            when(service.type(anyString())).thenReturn(service);
            when(service.accept(anyString())).thenReturn(service);

            AnyTypeTO anyTypeTO = new AnyTypeTO();
            anyTypeTO.setKey("123456");
            anyTypeTO.setKind(AnyTypeKind.USER);

            when(service.read(anyString())).thenReturn(anyTypeTO);
            return service;
        }

        private UserTO getUserTO() {
            UserTO userTO = new UserTO();
            userTO.setUsername("username");
            return userTO;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SyncopeClientFactoryBean newClientFactory() {
            SyncopeClient client = mock(SyncopeClient.class);

            when(client.self()).thenReturn(Triple.of(new HashMap<>(), List.of(), getUserTO()));

            when(client.gitAndBuildInfo()).thenReturn(Pair.of("", ""));
            when(client.platform()).thenReturn(new PlatformInfo());
            when(client.numbers()).thenAnswer(ic -> {
                NumbersInfo numbersInfo = new NumbersInfo();

                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.RESOURCE.name(), numbersInfo.getTotalResources() > 0);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.ACCOUNT_POLICY.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.PASSWORD_POLICY.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.NOTIFICATION.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.PULL_TASK.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.VIR_SCHEMA.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.ANY_TYPE.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.SECURITY_QUESTION.name(), false);
                numbersInfo.getConfCompleteness().put(
                        NumbersInfo.ConfItem.ROLE.name(), numbersInfo.getTotalRoles() > 0);

                return numbersInfo;
            });
            when(client.system()).thenReturn(new SystemInfo());

            SyncopeService syncopeService = getSyncopeService();
            when(client.getService(SyncopeService.class)).thenReturn(syncopeService);

            SchemaService schemaService = getSchemaService();
            when(client.getService(SchemaService.class)).thenReturn(schemaService);

            AnyTypeService anyTypeService = getAnyTypeService();
            when(client.getService(AnyTypeService.class)).thenReturn(anyTypeService);

            SyncopeClientFactoryBean clientFactory = mock(SyncopeClientFactoryBean.class);
            when(clientFactory.setDomain(any())).thenReturn(clientFactory);
            when(clientFactory.create(any(AuthenticationHandler.class))).thenReturn(client);
            when(clientFactory.create(anyString(), anyString())).thenReturn(client);

            return clientFactory;
        }
    }

    protected static ConsoleProperties PROPS;

    protected static WicketTester TESTER;

    @BeforeAll
    public static void setupTester() throws IOException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SyncopeConsoleWebApplicationTestConfig.class);
        ctx.register(TestSyncopeWebApplication.class);
        ctx.refresh();

        PROPS = ctx.getBean(ConsoleProperties.class);
        TESTER = new WicketTester(ctx.getBean(SyncopeWebApplication.class));
    }
}
