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

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingEntityManagerFactory;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.DefaultEncryptorManager;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jndi.JndiObjectFactoryBean;

@Import({
    PersistenceContext.class,
    PGPersistenceContext.class,
    MySQLPersistenceContext.class,
    MariaDBPersistenceContext.class,
    OraclePersistenceContext.class
})
@Configuration(proxyBeanMethods = false)
public class PersistenceTestContext {

    public static final ThreadLocal<String> TEST_DOMAIN = ThreadLocal.withInitial(() -> SyncopeConstants.MASTER_DOMAIN);

    @Value("${security.adminUser}")
    private String adminUser;

    @Value("${security.anonymousUser}")
    private String anonymousUser;

    @Bean
    public String adminUser() {
        return adminUser;
    }

    @Bean
    public String anonymousUser() {
        return anonymousUser;
    }

    @Bean
    public TestInitializer testInitializer(
            final StartupDomainLoader domainLoader,
            final DomainHolder<DataSource> domainHolder,
            final ContentLoader contentLoader,
            final DomainRoutingEntityManagerFactory entityManagerFactory,
            final ConfigurableApplicationContext ctx) {

        return new TestInitializer(domainLoader, domainHolder, contentLoader, entityManagerFactory, ctx);
    }

    @Bean
    public SecurityProperties securityProperties() {
        return new SecurityProperties();
    }

    @Bean
    public PasswordGenerator passwordGenerator() {
        return new DefaultPasswordGenerator();
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
    public ConnectorManager connectorManager() {
        return new DummyConnectorManager();
    }

    @Bean
    public EncryptorManager encryptorManager() {
        return new DefaultEncryptorManager();
    }

    @Bean
    public DomainRoutingEntityManagerFactory entityManagerFactory(
            final PersistenceProperties props,
            @Qualifier("MasterDataSource")
            final JndiObjectFactoryBean masterDataSource,
            final CommonEntityManagerFactoryConf commonEMFConf) {

        DomainRoutingEntityManagerFactory emf = new DomainRoutingEntityManagerFactory(commonEMFConf) {

            @Override
            protected EntityManagerFactory delegate() {
                return delegates.getOrDefault(
                        TEST_DOMAIN.get(),
                        delegates.get(SyncopeConstants.MASTER_DOMAIN));
            }
        };
        emf.master(props, masterDataSource);
        return emf;
    }
}
