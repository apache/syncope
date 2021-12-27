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
package org.apache.syncope.core.starter;

import java.util.Map;
import org.apache.cxf.spring.boot.autoconfigure.openapi.OpenApiAutoConfiguration;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.starter.actuate.DomainsHealthIndicator;
import org.apache.syncope.core.starter.actuate.ExternalResourcesHealthIndicator;
import org.apache.syncope.core.starter.actuate.DefaultSyncopeCoreInfoContributor;
import org.apache.syncope.core.starter.actuate.SyncopeCoreInfoContributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.mail.MailHealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
    ErrorMvcAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    OpenApiAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    QuartzAutoConfiguration.class }, proxyBeanMethods = false)
@EnableTransactionManagement
public class SyncopeCoreApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeCoreApplication.class).
                properties("spring.config.name:core").
                build().run(args);
    }

    @Autowired
    protected JavaMailSender mailSender;

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of("spring.config.name", "core")).sources(SyncopeCoreApplication.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreInfoContributor syncopeCoreInfoContributor() {
        return new DefaultSyncopeCoreInfoContributor();
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainsHealthIndicator domainsHealthIndicator() {
        return new DomainsHealthIndicator();
    }

    @ConditionalOnMissingBean
    @Bean
    public MailHealthIndicator mailHealthIndicator() {
        return new MailHealthIndicator((JavaMailSenderImpl) mailSender);
    }

    @ConditionalOnClass(name = { "org.apache.syncope.core.logic.ResourceLogic" })
    @ConditionalOnMissingBean
    @Bean
    public ExternalResourcesHealthIndicator externalResourcesHealthIndicator() {
        return new ExternalResourcesHealthIndicator();
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public SyncopeCoreStart keymasterStart(final DomainHolder domainHolder) {
        return new SyncopeCoreStart(domainHolder);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.CORE);
    }


    @Bean
    public SyncopeStarterEventListener syncopeCoreEventListener(
        @Qualifier("syncopeCoreInfoContributor")
        final SyncopeCoreInfoContributor syncopeCoreInfoContributor) {
        return new DefaultSyncopeStarterEventListener(syncopeCoreInfoContributor);
    }

    @FunctionalInterface
    public interface SyncopeStarterEventListener {
        void addLoadInstant(PayloadApplicationEvent<SystemInfo.LoadInstant> event);
    }

    public static class DefaultSyncopeStarterEventListener implements SyncopeStarterEventListener {
        private final SyncopeCoreInfoContributor contributor;

        public DefaultSyncopeStarterEventListener(final SyncopeCoreInfoContributor contributor) {
            this.contributor = contributor;
        }

        @EventListener
        @Override
        public void addLoadInstant(final PayloadApplicationEvent<SystemInfo.LoadInstant> event) {
            contributor.addLoadInstant(event);
        }
    }
}
