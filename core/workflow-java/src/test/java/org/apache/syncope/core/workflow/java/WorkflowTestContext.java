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
package org.apache.syncope.core.workflow.java;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.MariaDBPersistenceContext;
import org.apache.syncope.core.persistence.jpa.MySQLPersistenceContext;
import org.apache.syncope.core.persistence.jpa.OraclePersistenceContext;
import org.apache.syncope.core.persistence.jpa.PGPersistenceContext;
import org.apache.syncope.core.persistence.jpa.PersistenceContext;
import org.apache.syncope.core.persistence.jpa.StartupDomainLoader;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.SecurityContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:core-test.properties")
@Import({
    SecurityContext.class,
    WorkflowContext.class,
    PersistenceContext.class,
    PGPersistenceContext.class,
    MySQLPersistenceContext.class,
    MariaDBPersistenceContext.class,
    OraclePersistenceContext.class
})
@Configuration(proxyBeanMethods = false)
public class WorkflowTestContext {

    @Bean
    public TestInitializer testInitializer(
            final StartupDomainLoader domainLoader,
            final ContentLoader contentLoader,
            final ConfigurableApplicationContext ctx) {

        return new TestInitializer(domainLoader, contentLoader, ctx);
    }

    @Bean
    public UserDataBinder userDataBinder(final RealmSearchDAO realmSearchDAO) {
        UserDataBinder dataBinder = mock(UserDataBinder.class);

        doAnswer(ic -> {
            User user = ic.getArgument(0);
            UserCR userCR = ic.getArgument(1);

            user.setUsername(userCR.getUsername());
            user.setRealm(realmSearchDAO.findByFullPath(userCR.getRealm()).orElseThrow());
            user.setCreator("admin");
            user.setCreationDate(OffsetDateTime.now());
            user.setCipherAlgorithm(CipherAlgorithm.SHA256);
            user.setPassword(userCR.getPassword());

            return null;
        }).when(dataBinder).create(any(User.class), any(UserCR.class));

        return dataBinder;
    }

    @Bean
    public GroupDataBinder groupDataBinder() {
        GroupDataBinder dataBinder = mock(GroupDataBinder.class);
        return dataBinder;
    }

    @Bean
    public AnyObjectDataBinder anyObjectDataBinder() {
        AnyObjectDataBinder dataBinder = mock(AnyObjectDataBinder.class);
        return dataBinder;
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
}
