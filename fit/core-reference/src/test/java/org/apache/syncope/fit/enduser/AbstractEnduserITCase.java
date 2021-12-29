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
package org.apache.syncope.fit.enduser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;
import com.giffing.wicket.spring.boot.context.extensions.boot.actuator.WicketEndpointRepository;
import com.giffing.wicket.spring.boot.starter.app.classscanner.candidates.WicketClassCandidatesHolder;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.settings.general.GeneralSettingsProperties;
import com.giffing.wicket.spring.boot.starter.configuration.extensions.external.spring.boot.actuator.WicketEndpointRepositoryDefault;
import java.io.InputStream;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.PreviewUtils;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.client.ui.commons.ApplicationContextProvider;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.AbstractUIITCase;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.syncope.client.enduser.EnduserProperties;
import org.apache.syncope.fit.console.AbstractConsoleITCase;
import org.springframework.test.context.support.TestPropertySourceUtils;

public abstract class AbstractEnduserITCase extends AbstractUIITCase {

    @ImportAutoConfiguration(classes = { SelfKeymasterClientContext.class, ZookeeperKeymasterClientContext.class })
    @Configuration(proxyBeanMethods = false)
    public static class SyncopeEnduserWebApplicationTestConfig {

        @Bean
        public EnduserProperties enduserProperties() {
            EnduserProperties enduserProperties = new EnduserProperties();

            enduserProperties.setAdminUser(ADMIN_UNAME);

            enduserProperties.setAnonymousUser(ANONYMOUS_UNAME);
            enduserProperties.setAnonymousKey(ANONYMOUS_KEY);

            enduserProperties.setCsrf(false);

            return enduserProperties;
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
        public ApplicationContextProvider applicationContextProvider() {
            return new ApplicationContextProvider();
        }

        @Bean
        public ClassPathScanImplementationLookup classPathScanImplementationLookup() {
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
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
    }

    protected static SyncopeClientFactoryBean clientFactory;

    protected static SyncopeClient adminClient;

    protected static UserService userService;

    protected static SecurityQuestionService securityQuestionService;

    @BeforeAll
    public static void setUp() {
        Locale.setDefault(Locale.ENGLISH);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        ctx.register(SyncopeEnduserWebApplicationTestConfig.class);
        ctx.register(SyncopeWebApplication.class);

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

    @BeforeAll
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS);
        LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        userService = adminClient.getService(UserService.class);

        // create test user for must change password
        userService.create(new UserCR.Builder(SyncopeConstants.ROOT_REALM, "mustchangepassword").
                password("password123").
                mustChangePassword(true).
                plainAttr(attr("fullname", "mustchangepassword@apache.org")).
                plainAttr(attr("firstname", "mustchangepassword@apache.org")).
                plainAttr(attr("surname", "surname")).
                plainAttr(attr("ctype", "a type")).
                plainAttr(attr("userId", "mustchangepassword@apache.org")).
                plainAttr(attr("email", "mustchangepassword@apache.org")).
                plainAttr(attr("loginDate", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()))).
                build());

        // create test user for self password reset
        userService.create(new UserCR.Builder(SyncopeConstants.ROOT_REALM, "selfpwdreset").
                password("password123").
                plainAttr(attr("fullname", "selfpwdreset@apache.org")).
                plainAttr(attr("firstname", "selfpwdreset@apache.org")).
                plainAttr(attr("surname", "surname")).
                plainAttr(attr("ctype", "a type")).
                plainAttr(attr("userId", "selfpwdreset@apache.org")).
                plainAttr(attr("email", "selfpwdreset@apache.org")).
                plainAttr(attr("loginDate", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()))).
                build());

        // create test user for self update
        userService.create(new UserCR.Builder(SyncopeConstants.ROOT_REALM, "selfupdate").
                password("password123").
                plainAttr(attr("fullname", "selfupdate@apache.org")).
                plainAttr(attr("firstname", "selfupdate@apache.org")).
                plainAttr(attr("surname", "surname")).
                plainAttr(attr("ctype", "a type")).
                plainAttr(attr("userId", "selfupdate@apache.org")).
                build());

        securityQuestionService = adminClient.getService(SecurityQuestionService.class);
    }

    @AfterAll
    public static void cleanUp() {
        userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().
                        is("username").equalTo("selfupdate").
                        or("username").equalTo("selfpwdreset").
                        or("username").equalTo("mustchangepassword").query()).
                build()).getResult().forEach(user -> userService.delete(user.getKey()));
    }

    protected static void doLogin(final String user, final String passwd) {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", user);
        formTester.setValue("password", passwd);
        formTester.submit("submit");
    }

    protected static Attr attr(final String schema, final String value) {
        return new Attr.Builder(schema).value(value).build();
    }
}
