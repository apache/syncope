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

import jakarta.persistence.EntityManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepoExtJSONImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepoExtJSONImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.JSONAnyObjectRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.JSONGroupRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.JSONUserRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepoExtJSONImpl;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingDataSource;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

@Configuration(proxyBeanMethods = false)
public abstract class JPAJSONPersistenceContext {

    @ConditionalOnMissingBean(name = "jpaJSONAnyObjectDAO")
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy JPAJSONAnyDAO anyDAO,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(
                JSONAnyObjectRepo.class,
                new AnyObjectRepoExtJSONImpl(
                        anyUtilsFactory,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        userDAO,
                        groupDAO,
                        anyDAO,
                        domainEntityManager,
                        domainDataSource));
    }

    @ConditionalOnMissingBean(name = "jpaJSONGroupDAO")
    @Bean
    public GroupDAO groupDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final @Lazy JPAJSONAnyDAO anyDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(
                JSONGroupRepo.class,
                new GroupRepoExtJSONImpl(
                        anyUtilsFactory,
                        publisher,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        anyMatchDAO,
                        userDAO,
                        anyObjectDAO,
                        anySearchDAO,
                        anyDAO,
                        searchCondVisitor,
                        domainEntityManager,
                        domainDataSource));
    }

    @ConditionalOnMissingBean(name = "jpaJSONPlainAttrValueDAO")
    @Bean
    public PlainAttrValueDAO plainAttrValueDAO() {
        return new JPAJSONPlainAttrValueDAO();
    }

    @ConditionalOnMissingBean(name = "jpaJSONUserDAO")
    @Bean
    public UserDAO userDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy RoleDAO roleDAO,
            final @Lazy AccessTokenDAO accessTokenDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy DelegationDAO delegationDAO,
            final @Lazy FIQLQueryDAO fiqlQueryDAO,
            final @Lazy JPAJSONAnyDAO anyDAO,
            final SecurityProperties securityProperties,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(JSONUserRepo.class,
                new UserRepoExtJSONImpl(
                        anyUtilsFactory,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        roleDAO,
                        accessTokenDAO,
                        groupDAO,
                        delegationDAO,
                        fiqlQueryDAO,
                        anyDAO,
                        securityProperties,
                        domainEntityManager,
                        domainDataSource));
    }
}
