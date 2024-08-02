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
package org.apache.syncope.core.persistence.jpa;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.jpa.openjpa.ConnectorManagerRemoteCommitListener;
import org.apache.syncope.core.persistence.jpa.spring.DomainEntityManagerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

public class DomainConfFactory implements DomainRegistry {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainConfFactory.class);

    protected final ConfigurableApplicationContext ctx;

    public DomainConfFactory(final ConfigurableApplicationContext ctx) {
        this.ctx = ctx;
    }

    protected DefaultListableBeanFactory beanFactory() {
        return (DefaultListableBeanFactory) ctx.getBeanFactory();
    }

    protected void unregisterSingleton(final String name) {
        if (beanFactory().containsSingleton(name)) {
            beanFactory().destroySingleton(name);
        }
    }

    protected void registerSingleton(final String name, final Object bean) {
        unregisterSingleton(name);
        beanFactory().registerSingleton(name, bean);
    }

    protected void unregisterBeanDefinition(final String name) {
        if (beanFactory().containsBeanDefinition(name)) {
            beanFactory().removeBeanDefinition(name);
        }
    }

    protected void registerBeanDefinition(final String name, final BeanDefinition beanDefinition) {
        unregisterBeanDefinition(name);
        beanFactory().registerBeanDefinition(name, beanDefinition);
    }

    @Override
    public void register(final Domain domain) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(domain.getJdbcDriver());
        hikariConfig.setJdbcUrl(domain.getJdbcURL());
        hikariConfig.setUsername(domain.getDbUsername());
        hikariConfig.setPassword(domain.getDbPassword());
        hikariConfig.setSchema(domain.getDbSchema());
        hikariConfig.setTransactionIsolation(domain.getTransactionIsolation().name());
        hikariConfig.setMaximumPoolSize(domain.getPoolMaxActive());
        hikariConfig.setMinimumIdle(domain.getPoolMinIdle());

        // domainDataSource
        registerBeanDefinition(
                domain.getKey() + "DataSource",
                BeanDefinitionBuilder.rootBeanDefinition(JndiObjectFactoryBean.class).
                        addPropertyValue("jndiName", "java:comp/env/jdbc/syncope" + domain.getKey() + "DataSource").
                        addPropertyValue("defaultObject", new HikariDataSource(hikariConfig)).
                        getBeanDefinition());
        DataSource initedDataSource = beanFactory().getBean(domain.getKey() + "DataSource", DataSource.class);

        // domainResourceDatabasePopulator
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        databasePopulator.addScript(new ClassPathResource("/audit/" + domain.getAuditSql()));

        // domainDataSourceInitializer
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(initedDataSource);
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        registerSingleton(domain.getKey().toLowerCase() + "DataSourceInitializer", dataSourceInitializer);
        beanFactory().initializeBean(dataSourceInitializer, domain.getKey().toLowerCase() + "DataSourceInitializer");

        // domainEntityManagerFactory
        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(domain.getDatabasePlatform());

        ConnectorManagerRemoteCommitListener connectorManagerRemoteCommitListener =
                new ConnectorManagerRemoteCommitListener(domain.getKey());

        BeanDefinitionBuilder emf = BeanDefinitionBuilder.rootBeanDefinition(DomainEntityManagerFactoryBean.class).
                addPropertyValue("persistenceUnitName", domain.getKey()).
                addPropertyValue("mappingResources", domain.getOrm()).
                addPropertyReference("dataSource", domain.getKey() + "DataSource").
                addPropertyValue("jpaVendorAdapter", vendorAdapter).
                addPropertyReference("commonEntityManagerFactoryConf", "commonEMFConf").
                addPropertyValue("connectorManagerRemoteCommitListener", connectorManagerRemoteCommitListener);

        Map<String, Object> jpaPropertyMap = new HashMap<>();
        jpaPropertyMap.putAll(vendorAdapter.getJpaPropertyMap());
        Optional.ofNullable(domain.getDbSchema()).
                ifPresent(s -> jpaPropertyMap.put("openjpa.jdbc.Schema", s));
        if (ctx.getEnvironment().containsProperty("openjpaMetaDataFactory")) {
            jpaPropertyMap.put(
                    "openjpa.MetaDataFactory",
                    Objects.requireNonNull(ctx.getEnvironment().getProperty("openjpaMetaDataFactory")).
                            replace("##orm##", domain.getOrm()));
        }
        if (!jpaPropertyMap.isEmpty()) {
            emf.addPropertyValue("jpaPropertyMap", jpaPropertyMap);
        }

        registerBeanDefinition(domain.getKey() + "EntityManagerFactory", emf.getBeanDefinition());
        beanFactory().getBean(domain.getKey() + "EntityManagerFactory");

        // domainTransactionManager
        AbstractBeanDefinition domainTransactionManager =
                BeanDefinitionBuilder.rootBeanDefinition(JpaTransactionManager.class).
                        addPropertyReference("entityManagerFactory", domain.getKey() + "EntityManagerFactory").
                        getBeanDefinition();
        domainTransactionManager.addQualifier(new AutowireCandidateQualifier(Qualifier.class, domain.getKey()));
        registerBeanDefinition(domain.getKey() + "TransactionManager", domainTransactionManager);

        // domainTransactionTemplate
        AbstractBeanDefinition domainTransactionTemplate =
                BeanDefinitionBuilder.rootBeanDefinition(TransactionTemplate.class).
                        addPropertyReference("transactionManager", domain.getKey() + "TransactionManager").
                        getBeanDefinition();
        registerBeanDefinition(domain.getKey() + "TransactionTemplate", domainTransactionTemplate);

        // domainContentXML
        registerBeanDefinition(domain.getKey() + "ContentXML",
                BeanDefinitionBuilder.rootBeanDefinition(ByteArrayInputStream.class).
                        addConstructorArgValue(domain.getContent().getBytes()).
                        getBeanDefinition());

        // domainKeymasterConfParamsJSON
        registerBeanDefinition(domain.getKey() + "KeymasterConfParamsJSON",
                BeanDefinitionBuilder.rootBeanDefinition(ByteArrayInputStream.class).
                        addConstructorArgValue(domain.getKeymasterConfParams().getBytes()).
                        getBeanDefinition());
    }

    @Override
    public void unregister(final String domain) {
        // domainKeymasterConfParamsJSON
        unregisterSingleton(domain + "KeymasterConfParamsJSON");
        unregisterBeanDefinition(domain + "KeymasterConfParamsJSON");

        // domainContentXML
        unregisterSingleton(domain + "ContentXML");
        unregisterBeanDefinition(domain + "ContentXML");

        // domainEntityManagerFactory
        try {
            EntityManagerFactory emf = beanFactory().
                    getBean(domain + "EntityManagerFactory", EntityManagerFactory.class);
            emf.close();
        } catch (Exception e) {
            LOG.error("Could not close EntityManagerFactory for Domain {}", domain, e);
        }
        unregisterSingleton(domain + "EntityManagerFactory");
        unregisterBeanDefinition(domain + "EntityManagerFactory");

        // domainTransactionManager
        unregisterSingleton(domain + "TransactionManager");
        unregisterBeanDefinition(domain + "TransactionManager");

        // domainDataSourceInitializer
        unregisterSingleton(domain.toLowerCase() + "DataSourceInitializer");

        // domainResourceDatabasePopulator
        unregisterSingleton(domain.toLowerCase() + "ResourceDatabasePopulator");

        // domainDataSource
        unregisterSingleton(domain + "DataSource");
        unregisterBeanDefinition(domain + "DataSource");
    }
}
