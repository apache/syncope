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
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.hibernate.Cache;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;

public class JPAEntityCacheDAO implements EntityCacheDAO {

    protected final EntityManagerFactory entityManagerFactory;

    public JPAEntityCacheDAO(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    protected Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactoryImpl.class).getStatistics();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Statistics statistics = statistics();

        Map<String, Object> general = new HashMap<>();
        general.put("start time", statistics.getStart());
        general.put("sessions opened", statistics.getSessionOpenCount());
        general.put("sessions closed", statistics.getSessionCloseCount());
        general.put("transactions", statistics.getTransactionCount());
        general.put("successful transactions", statistics.getSuccessfulTransactionCount());
        general.put("optimistic lock failures", statistics.getOptimisticFailureCount());
        general.put("flushes", statistics.getFlushCount());
        general.put("connections obtained", statistics.getConnectCount());
        general.put("statements prepared", statistics.getPrepareStatementCount());
        general.put("statements closed", statistics.getCloseStatementCount());
        general.put("second level cache puts", statistics.getSecondLevelCachePutCount());
        general.put("second level cache hits", statistics.getSecondLevelCacheHitCount());
        general.put("second level cache misses", statistics.getSecondLevelCacheMissCount());
        general.put("entities loaded", statistics.getEntityLoadCount());
        general.put("entities updated", statistics.getEntityUpdateCount());
        general.put("entities upserted", statistics.getEntityUpsertCount());
        general.put("entities inserted", statistics.getEntityInsertCount());
        general.put("entities deleted", statistics.getEntityDeleteCount());
        general.put("entities fetched", statistics.getEntityFetchCount());
        general.put("collections loaded", statistics.getCollectionLoadCount());
        general.put("collections updated", statistics.getCollectionUpdateCount());
        general.put("collections removed", statistics.getCollectionRemoveCount());
        general.put("collections recreated", statistics.getCollectionRecreateCount());
        general.put("collections fetched", statistics.getCollectionFetchCount());
        general.put("naturalId queries executed to database", statistics.getNaturalIdQueryExecutionCount());
        general.put("naturalId cache puts", statistics.getNaturalIdCachePutCount());
        general.put("naturalId cache hits", statistics.getNaturalIdCacheHitCount());
        general.put("naturalId cache misses", statistics.getNaturalIdCacheMissCount());
        general.put("naturalId max query time", statistics.getNaturalIdQueryExecutionMaxTime());
        general.put("queries executed to database", statistics.getQueryExecutionCount());
        general.put("query cache puts", statistics.getQueryCachePutCount());
        general.put("query cache hits", statistics.getQueryCacheHitCount());
        general.put("query cache misses", statistics.getQueryCacheMissCount());
        general.put("update timestamps cache puts", statistics.getUpdateTimestampsCachePutCount());
        general.put("update timestamps cache hits", statistics.getUpdateTimestampsCacheHitCount());
        general.put("update timestamps cache misses", statistics.getUpdateTimestampsCacheMissCount());
        general.put("max query time", statistics.getQueryExecutionMaxTime());
        general.put("query plan cache hits", statistics.getQueryPlanCacheHitCount());
        general.put("query plan cache misses", statistics.getQueryPlanCacheMissCount());

        Map<String, Object> entities = new HashMap<>();
        for (String entityName : statistics.getEntityNames()) {
            EntityStatistics es = statistics.getEntityStatistics(entityName);

            Map<String, Object> entity = new HashMap<>();
            entity.put("cache hits", es.getCacheHitCount());
            entity.put("cache misses", es.getCacheMissCount());
            entity.put("cache puts", es.getCachePutCount());
            entity.put("cache removes", es.getCacheRemoveCount());
            entity.put("deletes", es.getDeleteCount());
            entity.put("fetches", es.getFetchCount());
            entity.put("inserts", es.getInsertCount());
            entity.put("loads", es.getLoadCount());
            entity.put("updates", es.getUpdateCount());
            entity.put("optimistic lock failures", es.getOptimisticFailureCount());
            entities.put(entityName, entity);
        }

        return Map.of("general", general, "entities", entities);
    }

    @Override
    public void enableStatistics() {
        statistics().setStatisticsEnabled(true);
    }

    @Override
    public void disableStatistics() {
        statistics().setStatisticsEnabled(false);
    }

    @Override
    public void resetStatistics() {
        statistics().clear();
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
