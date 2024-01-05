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
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;

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

        DomainHolder domainHolder = beanFactory().getBean(DomainHolder.class);
        domainHolder.getDomains().put(domain.getKey(), initedDataSource);

        DomainRoutingDataSource domainRoutingDataSource = beanFactory().getBean(DomainRoutingDataSource.class);
        domainRoutingDataSource.add(domain.getKey(), initedDataSource);
        
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

        // domainDataSourceInitializer
        unregisterSingleton(domain.toLowerCase() + "DataSourceInitializer");

        // domainResourceDatabasePopulator
        unregisterSingleton(domain.toLowerCase() + "ResourceDatabasePopulator");

        // domainDataSource
        unregisterSingleton(domain + "DataSource");
        unregisterBeanDefinition(domain + "DataSource");

        DomainHolder domainHolder = beanFactory().getBean(DomainHolder.class);
        domainHolder.getDomains().remove(domain);

        DomainRoutingDataSource domainRoutingDataSource = beanFactory().getBean(DomainRoutingDataSource.class);
        domainRoutingDataSource.remove(domain);
    }
}
