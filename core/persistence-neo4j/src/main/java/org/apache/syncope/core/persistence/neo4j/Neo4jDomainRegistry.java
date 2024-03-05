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
package org.apache.syncope.core.persistence.neo4j;

import java.io.ByteArrayInputStream;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class Neo4jDomainRegistry implements DomainRegistry<Neo4jDomain> {

    protected final ConfigurableApplicationContext ctx;

    public Neo4jDomainRegistry(final ConfigurableApplicationContext ctx) {
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
    protected DomainHolder<Driver> domainHolder() {
        return beanFactory().getBean(DomainHolder.class);
    }

    @Override
    public void register(final Neo4jDomain domain) {
        // domainDriver
        Driver driver = GraphDatabase.driver(
                domain.getUri(),
                AuthTokens.basic(domain.getUsername(), domain.getPassword()),
                Config.builder().
                        withMaxConnectionPoolSize(domain.getMaxConnectionPoolSize()).
                        withDriverMetrics().
                        withLogging(Logging.slf4j()).build());
        registerSingleton(domain.getKey().toLowerCase() + "Driver", driver);

        domainHolder().getDomains().put(domain.getKey(), driver);

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

        // domainDriver
        unregisterSingleton(domain + "Driver");

        beanFactory().getBean(DomainHolder.class).getDomains().remove(domain);
    }
}
