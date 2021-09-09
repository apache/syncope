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
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.jpa.spring.DomainEntityManagerFactoryBean;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;

public class DomainConfFactory implements DomainRegistry {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainConfFactory.class);

    protected static void unregisterSingleton(final String name) {
        if (ApplicationContextProvider.getBeanFactory().containsSingleton(name)) {
            ApplicationContextProvider.getBeanFactory().destroySingleton(name);
        }
    }

    protected static void registerSingleton(final String name, final Object bean) {
        unregisterSingleton(name);
        ApplicationContextProvider.getBeanFactory().registerSingleton(name, bean);
    }

    protected static void unregisterBeanDefinition(final String name) {
        if (ApplicationContextProvider.getBeanFactory().containsBeanDefinition(name)) {
            ApplicationContextProvider.getBeanFactory().removeBeanDefinition(name);
        }
    }

    protected static void registerBeanDefinition(final String name, final BeanDefinition beanDefinition) {
        unregisterBeanDefinition(name);
        ApplicationContextProvider.getBeanFactory().registerBeanDefinition(name, beanDefinition);
    }

    protected final Environment env;

    public DomainConfFactory(final Environment env) {
        this.env = env;
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
        DataSource initedDataSource = ApplicationContextProvider.getBeanFactory().
                getBean(domain.getKey() + "DataSource", DataSource.class);

        // domainResourceDatabasePopulator
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        databasePopulator.addScript(new ClassPathResource("/audit/" + domain.getAuditSql()));
        registerSingleton(domain.getKey().toLowerCase() + "ResourceDatabasePopulator", databasePopulator);

        // domainDataSourceInitializer
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(initedDataSource);
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        registerSingleton(domain.getKey().toLowerCase() + "DataSourceInitializer", dataSourceInitializer);

        // domainEntityManagerFactory
        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(domain.getDatabasePlatform());

        BeanDefinitionBuilder emf = BeanDefinitionBuilder.rootBeanDefinition(DomainEntityManagerFactoryBean.class).
                addPropertyValue("mappingResources", domain.getOrm()).
                addPropertyValue("persistenceUnitName", domain.getKey()).
                addPropertyReference("dataSource", domain.getKey() + "DataSource").
                addPropertyValue("jpaVendorAdapter", vendorAdapter).
                addPropertyReference("commonEntityManagerFactoryConf", "commonEMFConf");
        if (env.containsProperty("openjpaMetaDataFactory")) {
            emf.addPropertyValue("jpaPropertyMap", Map.of(
                    "openjpa.MetaDataFactory",
                    Objects.requireNonNull(env.getProperty("openjpaMetaDataFactory")).
                            replace("##orm##", domain.getOrm())));
        }
        registerBeanDefinition(domain.getKey() + "EntityManagerFactory", emf.getBeanDefinition());
        ApplicationContextProvider.getBeanFactory().getBean(domain.getKey() + "EntityManagerFactory");

        // domainTransactionManager
        AbstractBeanDefinition domainTransactionManager =
                BeanDefinitionBuilder.rootBeanDefinition(JpaTransactionManager.class).
                        addPropertyReference("entityManagerFactory", domain.getKey() + "EntityManagerFactory").
                        getBeanDefinition();
        domainTransactionManager.addQualifier(new AutowireCandidateQualifier(Qualifier.class, domain.getKey()));
        registerBeanDefinition(domain.getKey() + "TransactionManager", domainTransactionManager);

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
            EntityManagerFactory emf = ApplicationContextProvider.getBeanFactory().
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
