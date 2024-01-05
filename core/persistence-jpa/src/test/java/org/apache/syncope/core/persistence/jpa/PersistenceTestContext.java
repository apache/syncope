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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingDataSource;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

@Import(PersistenceContext.class)
@Configuration(proxyBeanMethods = false)
public class PersistenceTestContext {

    public static final ThreadLocal<String> TEST_DOMAIN = ThreadLocal.withInitial(() -> SyncopeConstants.MASTER_DOMAIN);

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        List<Resource> locations = new ArrayList<>();
        for (String location : System.getProperty("CORE_PROPERTIES").split(",")) {
            locations.add(resourceLoader.getResource(location));
        }
        ppc.setLocations(locations.toArray(Resource[]::new));

        return ppc;
    }

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
            final DomainHolder domainHolder,
            final ContentLoader contentLoader,
            final ConfigurableApplicationContext ctx) {

        return new TestInitializer(domainLoader, domainHolder, contentLoader, ctx);
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
    public DomainOps domainOps(final DomainRegistry domainRegistry) {
        return new DummyDomainOps(domainRegistry);
    }

    @Bean
    public ConnectorManager connectorManager() {
        return new DummyConnectorManager();
    }

    @Bean
    public DomainRoutingDataSource domainRoutingDataSource(final DomainHolder domainHolder) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        domainHolder.getDomains().forEach((domain, dataSource) -> targetDataSources.put(domain, dataSource));
        DomainRoutingDataSource routingSA = new DomainRoutingDataSource() {

            @Override
            protected Object determineCurrentLookupKey() {
                return TEST_DOMAIN.get();
            }
        };
        routingSA.setDefaultTargetDataSource(domainHolder.getDomains().get(SyncopeConstants.MASTER_DOMAIN));
        routingSA.setTargetDataSources(targetDataSources);
        return routingSA;
    }
}
