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
import jakarta.persistence.EntityManagerFactory;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.jpa.dao.OracleJPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.repo.OraclePlainSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepoExt;
import org.apache.syncope.core.persistence.jpa.entity.OracleEntityFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = PersistenceProperties.PREFIX, name = PersistenceProperties.DB_TYPE, havingValue = "ORACLE")
public class OraclePersistenceContext {

    @ConditionalOnMissingBean
    @Bean
    public EntityFactory entityFactory() {
        return new OracleEntityFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnySearchDAO anySearchDAO(
            final @Lazy RealmSearchDAO realmSearchDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy PlainSchemaDAO schemaDAO,
            final @Lazy EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager) {

        return new OracleJPAAnySearchDAO(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                entityManagerFactory,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaRepoExt plainSchemaRepoExt(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final EntityManager entityManager) {

        return new OraclePlainSchemaRepoExtImpl(anyUtilsFactory, resourceDAO, plainSchemaDAO, entityManager);
    }
}
