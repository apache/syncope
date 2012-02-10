/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.syncope.core.persistence.beans.AbstractBaseBean;
import org.syncope.core.persistence.dao.DAO;

@Configurable
public abstract class AbstractDAOImpl implements DAO {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(AbstractDAOImpl.class);

    private String CACHE_STORE_MODE =
            "javax.persistence.cache.storeMode";

    private String CACHE_RETRIEVE_MODE =
            "javax.persistence.cache.retrieveMode";

    @Value("#{entityManager}")
    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    protected EntityManager entityManager;

    protected CacheRetrieveMode getCacheRetrieveMode() {
        return entityManager.getProperties().containsKey(CACHE_RETRIEVE_MODE)
                ? (CacheRetrieveMode) entityManager.getProperties().get(
                CACHE_RETRIEVE_MODE) : CacheRetrieveMode.BYPASS;
    }

    protected void setCacheRetrieveMode(final CacheRetrieveMode retrieveMode) {
        if (retrieveMode != null) {
            entityManager.getProperties().
                    put(CACHE_RETRIEVE_MODE, retrieveMode);
        }
    }

    protected CacheStoreMode getCacheStoreMode() {
        return entityManager.getProperties().containsKey(CACHE_STORE_MODE)
                ? (CacheStoreMode) entityManager.getProperties().get(
                CACHE_STORE_MODE) : CacheStoreMode.BYPASS;
    }

    protected void setCacheStoreMode(final CacheStoreMode storeMode) {
        if (storeMode != null) {
            entityManager.getProperties().
                    put(CACHE_STORE_MODE, storeMode);
        }
    }

    @Override
    public <T extends AbstractBaseBean> void refresh(final T entity) {
        entityManager.refresh(entity);
    }

    @Override
    public void detach(final Object object) {
        entityManager.detach(object);
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
