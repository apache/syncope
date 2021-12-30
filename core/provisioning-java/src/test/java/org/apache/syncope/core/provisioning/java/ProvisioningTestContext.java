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
package org.apache.syncope.core.provisioning.java;

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.MasterDomain;
import org.apache.syncope.core.persistence.jpa.PersistenceContext;
import org.apache.syncope.core.persistence.jpa.StartupDomainLoader;
import org.apache.syncope.core.spring.security.SecurityContext;
import org.apache.syncope.core.workflow.java.WorkflowContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:core-test.properties")
@Import({ ProvisioningContext.class, SecurityContext.class,
    PersistenceContext.class, MasterDomain.class, WorkflowContext.class })
@Configuration(proxyBeanMethods = false)
public class ProvisioningTestContext {

    @Bean
    public TestInitializer testInitializer(
            final StartupDomainLoader domainLoader,
            final DomainHolder domainHolder,
            final ContentLoader contentLoader,
            final ConfigurableApplicationContext ctx) {

        return new TestInitializer(domainLoader, domainHolder, contentLoader, ctx);
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
}
