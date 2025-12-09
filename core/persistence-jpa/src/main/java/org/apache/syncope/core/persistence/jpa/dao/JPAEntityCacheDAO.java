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

import jakarta.persistence.EntityManagerFactory;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.hibernate.Cache;
import org.hibernate.internal.SessionFactoryImpl;

public class JPAEntityCacheDAO implements EntityCacheDAO {

    protected final EntityManagerFactory entityManagerFactory;

    public JPAEntityCacheDAO(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    protected Cache cache() {
        return entityManagerFactory.unwrap(SessionFactoryImpl.class).getCache();
    }

    @Override
    public void evict(final Class<? extends Entity> entityClass, final String key) {
        cache().evict(entityClass, key);
    }

    @Override
    public void clearCache() {
        Cache cache = cache();

        cache.evictAll();
        cache.evictQueryRegions();
    }
}
