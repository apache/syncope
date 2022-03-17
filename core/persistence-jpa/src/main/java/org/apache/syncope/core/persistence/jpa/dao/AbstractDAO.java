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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.syncope.core.persistence.api.dao.DAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

public abstract class AbstractDAO<E extends Entity> implements DAO<E> {

    protected static final Logger LOG = LoggerFactory.getLogger(DAO.class);

    protected EntityManagerFactory entityManagerFactory() {
        return EntityManagerFactoryUtils.findEntityManagerFactory(
                ApplicationContextProvider.getBeanFactory(), AuthContextUtils.getDomain());
    }

    protected EntityManager entityManager() {
        return Optional.ofNullable(EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory())).
                orElseThrow(() -> new IllegalStateException(
                "Could not find EntityManager for domain " + AuthContextUtils.getDomain()));
    }

    @Override
    public void refresh(final E entity) {
        entityManager().refresh(entity);
    }

    @Override
    public void detach(final E entity) {
        entityManager().detach(entity);
    }

    @Override
    public void clear() {
        entityManager().clear();
    }
}
