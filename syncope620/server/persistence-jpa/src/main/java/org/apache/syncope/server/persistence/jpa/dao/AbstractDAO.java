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
package org.apache.syncope.server.persistence.jpa.dao;

import java.util.List;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.server.persistence.api.dao.DAO;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

@Configurable
public abstract class AbstractDAO<E extends Entity<KEY>, KEY> implements DAO<E, KEY> {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(DAO.class);

    private static final String CACHE_STORE_MODE = "javax.persistence.cache.storeMode";

    private static final String CACHE_RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";

    @Value("#{entityManager}")
    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    protected EntityManager entityManager;

    protected CacheRetrieveMode getCacheRetrieveMode() {
        return entityManager.getProperties().containsKey(CACHE_RETRIEVE_MODE)
                ? (CacheRetrieveMode) entityManager.getProperties().get(CACHE_RETRIEVE_MODE)
                : CacheRetrieveMode.BYPASS;
    }

    protected void setCacheRetrieveMode(final CacheRetrieveMode retrieveMode) {
        if (retrieveMode != null) {
            entityManager.getProperties().put(CACHE_RETRIEVE_MODE, retrieveMode);
        }
    }

    protected CacheStoreMode getCacheStoreMode() {
        return entityManager.getProperties().containsKey(CACHE_STORE_MODE)
                ? (CacheStoreMode) entityManager.getProperties().get(CACHE_STORE_MODE)
                : CacheStoreMode.BYPASS;
    }

    protected void setCacheStoreMode(final CacheStoreMode storeMode) {
        if (storeMode != null) {
            entityManager.getProperties().put(CACHE_STORE_MODE, storeMode);
        }
    }

    protected String toOrderByStatement(final Class<? extends Entity<KEY>> beanClass, final String prefix,
            final List<OrderByClause> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        for (OrderByClause clause : orderByClauses) {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(beanClass, field) != null) {
                if (StringUtils.isNotBlank(prefix)) {
                    statement.append(prefix).append('.');
                }
                statement.append(field).append(' ').append(clause.getDirection().name());
            }
        }

        if (statement.length() > 0) {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    @Override
    public void refresh(final E entity) {
        entityManager.refresh(entity);
    }

    @Override
    public void detach(final E entity) {
        entityManager.detach(entity);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }

    @Override
    public void clear() {
        entityManager.clear();
    }
}
