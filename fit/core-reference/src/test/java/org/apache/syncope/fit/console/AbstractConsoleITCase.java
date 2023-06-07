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
package org.apache.syncope.fit.console;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;
import com.giffing.wicket.spring.boot.context.extensions.boot.actuator.WicketEndpointRepository;
import com.giffing.wicket.spring.boot.starter.app.classscanner.candidates.WicketClassCandidatesHolder;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.settings.general.GeneralSettingsProperties;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.boot.actuator.WicketEndpointRepositoryDefault;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import org.apache.syncope.client.console.AMConsoleContext;
import org.apache.syncope.client.console.ConsoleProperties;
import org.apache.syncope.client.console.FlowableConsoleContext;
import org.apache.syncope.client.console.IdMConsoleContext;
import org.apache.syncope.client.console.IdRepoConsoleContext;
import org.apache.syncope.client.console.OIDCC4UIConsoleContext;
import org.apache.syncope.client.console.SAML2SP4UIConsoleContext;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.IdRepoPolicyTabProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.fit.AbstractUIITCase;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.TestPropertySourceUtils;

public abstract class AbstractConsoleITCase extends AbstractUIITCase {

    @ImportAutoConfiguration(classes = {
        SelfKeymasterClientContext.class,
        ZookeeperKeymasterClientContext.class,
        IdRepoConsoleContext.class,
        FlowableConsoleContext.class,
        SAML2SP4UIConsoleContext.class,
        OIDCC4UIConsoleContext.class })
    @Configuration(proxyBeanMethods = false)
    public static class SyncopeConsoleWebApplicationTestConfig {

        @Bean
        public ConsoleProperties consoleProperties() {
            ConsoleProperties consoleProperties = new ConsoleProperties();

            consoleProperties.setAnonymousUser(ANONYMOUS_UNAME);
            consoleProperties.setAnonymousKey(ANONYMOUS_KEY);

            consoleProperties.setCsrf(false);
            consoleProperties.getPage().put("topology", Topology.class);

            return consoleProperties;
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
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup(Set.of(),
                    consoleProperties());
            lookup.load();
            return lookup;
        }

        @Bean
        public PolicyTabProvider idRepoPolicyTabProvider(final PolicyRestClient policyRestClient) {
            return new IdRepoPolicyTabProvider(policyRestClient);
        }

        @Bean
        public LoggingSystem loggingSystem() {
            System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
            return LoggingSystem.get(getClass().getClassLoader());
        }
    }

    @BeforeAll
    public static void setUp() {
        Locale.setDefault(Locale.ENGLISH);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        ctx.register(SyncopeConsoleWebApplicationTestConfig.class);
        ctx.register(SyncopeWebApplication.class);
        ctx.register(AMConsoleContext.class);
        ctx.register(IdMConsoleContext.class);

        String springActiveProfiles = null;
        try (InputStream propStream = AbstractConsoleITCase.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            springActiveProfiles = props.getProperty("springActiveProfiles");
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }
        assertNotNull(springActiveProfiles);

        if (springActiveProfiles.contains("zookeeper")) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx, "keymaster.address=127.0.0.1:2181");
        } else {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx, "keymaster.address=http://localhost:9080/syncope/rest/keymaster");
        }
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                ctx, "keymaster.username=" + ANONYMOUS_UNAME);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                ctx, "keymaster.password=" + ANONYMOUS_KEY);

        ctx.refresh();

        TESTER = new WicketTester(ctx.getBean(SyncopeWebApplication.class));

        SYNCOPE_SERVICE = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).create(ADMIN_UNAME, ADMIN_PWD).
                getService(SyncopeService.class);
    }

    protected void doLogin(final String user, final String passwd) {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", user);
        formTester.setValue("password", passwd);
        formTester.submit("submit");
    }
}
