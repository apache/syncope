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

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;
import com.giffing.wicket.spring.boot.context.extensions.boot.actuator.WicketEndpointRepository;
import com.giffing.wicket.spring.boot.starter.app.classscanner.candidates.WicketClassCandidatesHolder;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.settings.general.GeneralSettingsProperties;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.boot.actuator.WicketEndpointRepositoryDefault;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.syncope.client.console.SyncopeAMConsoleContext;
import org.apache.syncope.client.console.SyncopeIdMConsoleContext;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.IdRepoPolicyTabProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.PreviewUtils;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.console.wizards.any.UserFormFinalizerUtils;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.ApplicationContextProvider;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.fit.ui.AbstractUITCase;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public abstract class AbstractConsoleITCase extends AbstractUITCase {

    @ImportAutoConfiguration(classes = { SelfKeymasterClientContext.class, ZookeeperKeymasterClientContext.class })
    @Configuration
    public static class SyncopeConsoleWebApplicationTestConfig {

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
        public ApplicationContextProvider applicationContextProvider() {
            return new ApplicationContextProvider();
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
        public PolicyTabProvider idRepoPolicyTabProvider() {
            return new IdRepoPolicyTabProvider();
        }
    }

    @BeforeAll
    public static void setUp() {
        Locale.setDefault(Locale.ENGLISH);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SyncopeConsoleWebApplicationTestConfig.class);
        ctx.register(SyncopeWebApplication.class);
        ctx.register(SyncopeAMConsoleContext.class);
        ctx.register(SyncopeIdMConsoleContext.class);
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
