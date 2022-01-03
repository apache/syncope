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

import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONAnyObjectDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONGroupDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONPlainAttrDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJSONUserDAO;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public abstract class JPAJSONPersistenceContext {

    @Autowired
    protected ApplicationEventPublisher publisher;

    @Autowired
    protected SecurityProperties securityProperties;

    @ConditionalOnMissingBean(name = "jpaJSONAnyObjectDAO")
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy JPAJSONAnyDAO anyDAO) {

        return new JPAJSONAnyObjectDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyDAO);
    }

    @ConditionalOnMissingBean(name = "jpaJSONGroupDAO")
    @Bean
    public GroupDAO groupDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy PlainAttrDAO plainAttrDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final SearchCondVisitor searchCondVisitor,
            final @Lazy JPAJSONAnyDAO anyDAO) {

        return new JPAJSONGroupDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                anyMatchDAO,
                plainAttrDAO,
                userDAO,
                anyObjectDAO,
                anySearchDAO,
                searchCondVisitor,
                anyDAO);
    }

    @ConditionalOnMissingBean(name = "jpaJSONPlainAttrDAO")
    @Bean
    public PlainAttrDAO plainAttrDAO() {
        return new JPAJSONPlainAttrDAO();
    }

    @ConditionalOnMissingBean(name = "jpaJSONPlainAttrValueDAO")
    @Bean
    public PlainAttrValueDAO plainAttrValueDAO() {
        return new JPAJSONPlainAttrValueDAO();
    }

    @ConditionalOnMissingBean(name = "jpaJSONUserDAO")
    @Bean
    public UserDAO userDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy RoleDAO roleDAO,
            final @Lazy AccessTokenDAO accessTokenDAO,
            final @Lazy RealmDAO realmDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy DelegationDAO delegationDAO,
            final @Lazy JPAJSONAnyDAO anyDAO) {

        return new JPAJSONUserDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                roleDAO,
                accessTokenDAO,
                realmDAO,
                groupDAO,
                delegationDAO,
                securityProperties,
                anyDAO);
    }
}
