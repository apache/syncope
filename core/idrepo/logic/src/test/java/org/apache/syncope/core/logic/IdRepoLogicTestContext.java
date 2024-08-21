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
package org.apache.syncope.core.logic;

import static org.mockito.Mockito.mock;

import javax.cache.CacheManager;
import javax.cache.Caching;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.MariaDBPersistenceContext;
import org.apache.syncope.core.persistence.jpa.MySQLPersistenceContext;
import org.apache.syncope.core.persistence.jpa.OraclePersistenceContext;
import org.apache.syncope.core.persistence.jpa.PGPersistenceContext;
import org.apache.syncope.core.persistence.jpa.PersistenceContext;
import org.apache.syncope.core.persistence.jpa.StartupDomainLoader;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.java.ProvisioningContext;
import org.apache.syncope.core.spring.security.SecurityContext;
import org.apache.syncope.core.workflow.java.WorkflowContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;

@PropertySource("classpath:core-test.properties")
@Import({
    SecurityContext.class,
    WorkflowContext.class,
    PersistenceContext.class,
    PGPersistenceContext.class,
    MySQLPersistenceContext.class,
    MariaDBPersistenceContext.class,
    OraclePersistenceContext.class,
    ProvisioningContext.class,
    IdRepoLogicContext.class
})
@Configuration(proxyBeanMethods = false)
public class IdRepoLogicTestContext {

    @Bean
    public TestInitializer testInitializer(
            final StartupDomainLoader domainLoader,
            final ContentLoader contentLoader,
            final ConfigurableApplicationContext ctx) {

        return new TestInitializer(domainLoader, contentLoader, ctx);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    public LoggingSystem loggingSystem() {
        return mock(LoggingSystem.class);
    }

    @Bean
    public ImplementationLookup implementationLookup() {
        return new DummyImplementationLookup();
    }

    @Bean
    public ConfParamOps confParamOps() {
        return new DummyConfParamOps();
    }

    @Bean
    public DomainOps domainOps(final DomainRegistry<JPADomain> domainRegistry) {
        return new DummyDomainOps(domainRegistry);
    }

    @Bean
    public ServiceOps serviceOps() {
        return new DummyServiceOps();
    }

    @Bean
    public CacheManager cacheManager() {
        return Caching.getCachingProvider().getCacheManager();
    }
}
