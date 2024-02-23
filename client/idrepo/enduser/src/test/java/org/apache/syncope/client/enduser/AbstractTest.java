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
package org.apache.syncope.client.enduser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.lib.AuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

public abstract class AbstractTest {

    @ImportAutoConfiguration
    @Configuration(proxyBeanMethods = false)
    public static class SyncopeEnduserWebApplicationTestConfig {

        @Bean
        public EnduserProperties enduserProperties() {
            EnduserProperties enduserProperties = new EnduserProperties();

            enduserProperties.getSecurityHeaders().put("X-XSS-Protection", "1; mode=block");
            enduserProperties.getSecurityHeaders().
                    put("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            enduserProperties.getSecurityHeaders().put("X-Content-Type-Options", "nosniff");
            enduserProperties.getSecurityHeaders().put("X-Frame-Options", "sameorigin");

            enduserProperties.setAdminUser("admin");

            enduserProperties.setAnonymousUser("anonymousUser");

            return enduserProperties;
        }

        @Bean
        public ServiceOps selfServiceOps() {
            return mock(ServiceOps.class);
        }

        @Bean
        public DomainOps domainOps() {
            DomainOps domainOps = mock(DomainOps.class);
            when(domainOps.list()).thenReturn(List.of(new JPADomain.Builder(SyncopeConstants.MASTER_DOMAIN).build()));
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
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
            lookup.load();
            return lookup;
        }
    }

    public static class TestSyncopeWebApplication extends SyncopeWebApplication {

        public TestSyncopeWebApplication(
                final ResourceLoader resourceLoader,
                final EnduserProperties props,
                final ClassPathScanImplementationLookup lookup,
                final ServiceOps serviceOps,
                final List<IResource> resources) {

            super(resourceLoader, props, lookup, serviceOps, resources);
        }

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
            SyncopeAnonymousClient anonymousClient = mock(SyncopeAnonymousClient.class);

            when(client.getJWT()).thenReturn("<anyJWT>");

            when(client.self()).thenReturn(Triple.of(new HashMap<>(), List.of(), getUserTO()));

            when(anonymousClient.platform()).thenReturn(new PlatformInfo());
            when(anonymousClient.numbers()).thenAnswer(ic -> {
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
            when(anonymousClient.system()).thenReturn(new SystemInfo());

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
            when(clientFactory.createAnonymous(anyString(), isNull())).thenReturn(anonymousClient);

            return clientFactory;
        }
    }

    protected static EnduserProperties PROPS;

    protected static WicketTester TESTER;

    @BeforeAll
    public static void setupTester() throws IOException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SyncopeEnduserWebApplicationTestConfig.class);
        ctx.register(TestSyncopeWebApplication.class);
        ctx.refresh();

        PROPS = ctx.getBean(EnduserProperties.class);
        TESTER = new WicketTester(ctx.getBean(SyncopeWebApplication.class));
    }
}
