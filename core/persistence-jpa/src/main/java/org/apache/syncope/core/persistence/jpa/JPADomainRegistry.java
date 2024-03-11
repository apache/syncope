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
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingEntityManagerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jndi.JndiObjectFactoryBean;

public class JPADomainRegistry implements DomainRegistry<JPADomain> {

    protected final ConfigurableApplicationContext ctx;

    public JPADomainRegistry(final ConfigurableApplicationContext ctx) {
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

    @SuppressWarnings("unchecked")
    protected DomainHolder<DataSource> domainHolder() {
        return beanFactory().getBean(DomainHolder.class);
    }

    @Override
    public void register(final JPADomain domain) {
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
        beanFactory().registerBeanDefinition(
                domain.getKey() + "DataSource",
                BeanDefinitionBuilder.rootBeanDefinition(JndiObjectFactoryBean.class).
                        addPropertyValue("jndiName", "java:comp/env/jdbc/syncope" + domain.getKey() + "DataSource").
                        addPropertyValue("defaultObject", new HikariDataSource(hikariConfig)).
                        getBeanDefinition());
        DataSource initedDataSource = beanFactory().getBean(domain.getKey() + "DataSource", DataSource.class);

        domainHolder().getDomains().put(domain.getKey(), initedDataSource);

        // DomainRoutingEntityManagerFactory#domain
        beanFactory().getBean(DomainRoutingEntityManagerFactory.class).
                domain(domain, initedDataSource, ctx.getEnvironment().getProperty("openjpaMetaDataFactory"));

        // domainContentXML
        beanFactory().registerBeanDefinition(domain.getKey() + "ContentXML",
                BeanDefinitionBuilder.rootBeanDefinition(ByteArrayInputStream.class).
                        addConstructorArgValue(domain.getContent().getBytes()).
                        getBeanDefinition());

        // domainKeymasterConfParamsJSON
        beanFactory().registerBeanDefinition(domain.getKey() + "KeymasterConfParamsJSON",
                BeanDefinitionBuilder.rootBeanDefinition(ByteArrayInputStream.class).
                        addConstructorArgValue(domain.getKeymasterConfParams().getBytes()).
                        getBeanDefinition());
    }

    @Override
    public void unregister(final String domain) {
        // domainKeymasterConfParamsJSON
        unregisterSingleton(domain + "KeymasterConfParamsJSON");
        beanFactory().removeBeanDefinition(domain + "KeymasterConfParamsJSON");

        // domainContentXML
        unregisterSingleton(domain + "ContentXML");
        beanFactory().removeBeanDefinition(domain + "ContentXML");

        // DomainRoutingEntityManagerFactory#remove
        beanFactory().getBean(DomainRoutingEntityManagerFactory.class).remove(domain);

        // domainDataSourceInitializer
        unregisterSingleton(domain.toLowerCase() + "DataSourceInitializer");

        // domainResourceDatabasePopulator
        unregisterSingleton(domain.toLowerCase() + "ResourceDatabasePopulator");

        // domainDataSource
        unregisterSingleton(domain + "DataSource");
        beanFactory().removeBeanDefinition(domain + "DataSource");

        beanFactory().getBean(DomainHolder.class).getDomains().remove(domain);
    }
}
