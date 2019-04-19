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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.request.DomainCR;
import org.apache.syncope.core.persistence.jpa.spring.DomainEntityManagerFactoryBean;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;
import org.springframework.stereotype.Component;

@Component
public class DomainConfFactory implements EnvironmentAware {

    @Value("${content.directory}")
    private String contentDirectory;

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    private void registerSingleton(final String name, final Object bean) {
        if (ApplicationContextProvider.getBeanFactory().containsSingleton(name)) {
            ApplicationContextProvider.getBeanFactory().destroySingleton(name);
        }
        ApplicationContextProvider.getBeanFactory().registerSingleton(name, bean);
    }

    private void registerBeanDefinition(final String name, final BeanDefinition beanDefinition) {
        if (ApplicationContextProvider.getBeanFactory().containsBeanDefinition(name)) {
            ApplicationContextProvider.getBeanFactory().removeBeanDefinition(name);
        }
        ApplicationContextProvider.getBeanFactory().registerBeanDefinition(name, beanDefinition);
    }

    public void register(final DomainCR req) {
        // localDomainDataSource
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(req.getJdbcDriver());
        hikariConfig.setJdbcUrl(req.getJdbcURL());
        hikariConfig.setUsername(req.getDbUsername());
        hikariConfig.setPassword(req.getDbPassword());
        hikariConfig.setSchema(req.getDbSchema());
        hikariConfig.setTransactionIsolation(req.getTransactionIsolation());
        hikariConfig.setMaximumPoolSize(req.getMaxPoolSize());
        hikariConfig.setMinimumIdle(req.getMinIdle());
        String domainName = StringUtils.capitalize(req.getDomainName());

        HikariDataSource localDomainDataSource = new HikariDataSource(hikariConfig);

        // domainDataSource
        registerBeanDefinition(
                domainName + "DataSource",
                BeanDefinitionBuilder.rootBeanDefinition(JndiObjectFactoryBean.class).
                        addPropertyValue("jndiName", "java:comp/env/jdbc/syncope" + domainName + "DataSource").
                        addPropertyValue("defaultObject", localDomainDataSource).
                        getBeanDefinition());
        DataSource initedDataSource = ApplicationContextProvider.getBeanFactory().
                getBean(domainName + "DataSource", DataSource.class);

        // domainResourceDatabasePopulator
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        databasePopulator.addScript(new ClassPathResource("/audit/" + req.getAuditSql()));

        registerSingleton(domainName.toLowerCase() + "ResourceDatabasePopulator", databasePopulator);

        // domainDataSourceInitializer
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(initedDataSource);
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        registerSingleton(domainName.toLowerCase() + "DataSourceInitializer", dataSourceInitializer);

        // domainEntityManagerFactory
        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(req.getDatabasePlatform());

        BeanDefinitionBuilder emf = BeanDefinitionBuilder.rootBeanDefinition(DomainEntityManagerFactoryBean.class).
                addPropertyValue("mappingResources", req.getOrm()).
                addPropertyValue("persistenceUnitName", domainName).
                addPropertyReference("dataSource", domainName + "DataSource").
                addPropertyValue("jpaVendorAdapter", vendorAdapter).
                addPropertyReference("commonEntityManagerFactoryConf", "commonEMFConf");
        if (env.containsProperty("openjpaMetaDataFactory")) {
            emf.addPropertyValue("jpaPropertyMap",
                    Collections.singletonMap(
                            "openjpa.MetaDataFactory",
                            env.getProperty("openjpaMetaDataFactory").replace("##orm##", req.getOrm())));
        }
        registerBeanDefinition(domainName + "EntityManagerFactory", emf.getBeanDefinition());
        ApplicationContextProvider.getBeanFactory().getBean(domainName + "EntityManagerFactory");

        // domainTransactionManager
        AbstractBeanDefinition domainTransactionManager =
                BeanDefinitionBuilder.rootBeanDefinition(JpaTransactionManager.class).
                        addPropertyReference("entityManagerFactory", domainName + "EntityManagerFactory").
                        getBeanDefinition();
        domainTransactionManager.addQualifier(new AutowireCandidateQualifier(Qualifier.class, domainName));
        registerBeanDefinition(domainName + "TransactionManager", domainTransactionManager);

        // domainContentXML
        registerBeanDefinition(domainName + "ContentXML",
                BeanDefinitionBuilder.rootBeanDefinition(ResourceWithFallbackLoader.class).
                        addPropertyValue(
                                "primary",
                                "file:" + contentDirectory + "/domains/" + domainName + "Content.xml").
                        addPropertyValue(
                                "fallback",
                                "classpath:domains/" + domainName + "Content.xml").
                        getBeanDefinition());

        // domainKeymasterContentJSON
        registerBeanDefinition(domainName + "KeymasterContentJSON",
                BeanDefinitionBuilder.rootBeanDefinition(ResourceWithFallbackLoader.class).
                        addPropertyValue(
                                "primary",
                                "file:" + contentDirectory + "/domains/" + domainName + "KeymasterContent.json").
                        addPropertyValue(
                                "fallback",
                                "classpath:domains/" + domainName + "KeymasterContent.json").
                        getBeanDefinition());

    }
}
