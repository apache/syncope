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
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.starter.actuate.DefaultSyncopeCoreInfoContributor;
import org.apache.syncope.core.starter.actuate.DomainsHealthIndicator;
import org.apache.syncope.core.starter.actuate.EntityCacheEndpoint;
import org.apache.syncope.core.starter.actuate.ExternalResourcesHealthIndicator;
import org.apache.syncope.core.starter.actuate.SyncopeCoreInfoContributor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.mail.MailHealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {
            ErrorMvcAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            OpenApiAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            Neo4jDataAutoConfiguration.class,
            Neo4jRepositoriesAutoConfiguration.class,
            Neo4jReactiveDataAutoConfiguration.class,
            Neo4jReactiveRepositoriesAutoConfiguration.class,
            TaskExecutionAutoConfiguration.class,
            TaskSchedulingAutoConfiguration.class,
            ElasticsearchRestClientAutoConfiguration.class,
            ElasticsearchClientAutoConfiguration.class },
        proxyBeanMethods = false)
@EnableTransactionManagement
@EnableCaching
public class SyncopeCoreApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeCoreApplication.class).
                properties("spring.config.name:core").
                build().run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of("spring.config.name", "core")).sources(SyncopeCoreApplication.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskExecutorUnloader taskExecutorUnloader(final ListableBeanFactory beanFactory) {
        return new TaskExecutorUnloader(beanFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreStart keymasterStart(final DomainHolder<?> domainHolder) {
        return new SyncopeCoreStart(domainHolder);
    }

    @ConditionalOnMissingBean
    @Bean
    public KeymasterStop keymasterStop(final DomainHolder<?> domainHolder) {
        return new SyncopeCoreStop(domainHolder);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreInfoContributor syncopeCoreInfoContributor(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ExternalResourceDAO resourceDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final RoleDAO roleDAO,
            final PolicyDAO policyDAO,
            final NotificationDAO notificationDAO,
            final TaskDAO taskDAO,
            final VirSchemaDAO virSchemaDAO,
            final SecurityQuestionDAO securityQuestionDAO,
            final PersistenceInfoDAO persistenceInfoDAO,
            final ConfParamOps confParamOps,
            final ConnIdBundleManager bundleManager,
            final ImplementationLookup implLookup) {

        return new DefaultSyncopeCoreInfoContributor(
                anyTypeDAO,
                anyTypeClassDAO,
                resourceDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                roleDAO,
                policyDAO,
                notificationDAO,
                taskDAO,
                virSchemaDAO,
                securityQuestionDAO,
                persistenceInfoDAO,
                confParamOps,
                bundleManager,
                implLookup);
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainsHealthIndicator domainsHealthIndicator(final DomainHolder<?> domainHolder) {
        return new DomainsHealthIndicator(domainHolder);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailHealthIndicator mailHealthIndicator(final JavaMailSender mailSender) {
        return new MailHealthIndicator((JavaMailSenderImpl) mailSender);
    }

    @ConditionalOnClass(name = { "org.apache.syncope.core.logic.ResourceLogic" })
    @ConditionalOnMissingBean
    @Bean
    public ExternalResourcesHealthIndicator externalResourcesHealthIndicator(
            final DomainOps domainOps,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final ConnectorManager connectorManager) {

        return new ExternalResourcesHealthIndicator(domainOps, resourceDAO, connInstanceDataBinder, connectorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityCacheEndpoint entityCacheEndpoint(final EntityCacheDAO entityCacheDAO) {
        return new EntityCacheEndpoint(entityCacheDAO);
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
