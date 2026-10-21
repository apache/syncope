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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.openjpa.datacache.CacheStatistics;
import org.apache.openjpa.datacache.CacheStatisticsSPI;
import org.apache.openjpa.datacache.QueryKey;
import org.apache.openjpa.kernel.QueryStatistics;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.QueryResultCacheImpl;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;

public class JPAEntityCacheDAO implements EntityCacheDAO {

    protected final EntityManagerFactory entityManagerFactory;

    public JPAEntityCacheDAO(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    protected CacheStatisticsSPI cacheStatisticsSPI() {
        return (CacheStatisticsSPI) entityManagerFactory.unwrap(OpenJPAEntityManagerFactory.class).
                getStoreCache().getStatistics();
    }

    protected QueryStatistics<QueryKey> queryStatistics() {
        return ((QueryResultCacheImpl) entityManagerFactory.unwrap(OpenJPAEntityManagerFactory.class).
                getQueryResultCache()).getDelegate().getStatistics();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new LinkedHashMap<>();

        CacheStatistics cacheStats = cacheStatisticsSPI();

        Map<String, Object> storeCache = new LinkedHashMap<>();
        result.put("storeCache", storeCache);

        storeCache.put("enabled", cacheStats.isEnabled());
        storeCache.put("activation", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                cacheStats.start().toInstant().atOffset(FormatUtils.DEFAULT_OFFSET)));
        storeCache.put("last_update", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                cacheStats.since().toInstant().atOffset(FormatUtils.DEFAULT_OFFSET)));
        storeCache.put("hits", cacheStats.getHitCount());
        storeCache.put("reads", cacheStats.getReadCount());
        storeCache.put("writes", cacheStats.getWriteCount());
        storeCache.put("total_hits", cacheStats.getTotalHitCount());
        storeCache.put("total_reads", cacheStats.getTotalReadCount());
        storeCache.put("total_writes", cacheStats.getTotalWriteCount());

        List<Map<String, Object>> storeCacheDetails = new ArrayList<>();
        storeCache.put("details", storeCacheDetails);
        cacheStats.classNames().forEach(className -> {
            Map<String, Object> classMap = new LinkedHashMap<>();
            classMap.put("region", className);
            classMap.put("hits", cacheStats.getHitCount(className));
            classMap.put("reads", cacheStats.getReadCount(className));
            classMap.put("writes", cacheStats.getWriteCount(className));
            storeCache.put("total_hits", cacheStats.getTotalHitCount(className));
            storeCache.put("total_reads", cacheStats.getTotalReadCount(className));
            storeCache.put("total_writes", cacheStats.getTotalWriteCount(className));
            storeCacheDetails.add(classMap);
        });

        QueryStatistics<QueryKey> queryStats = queryStatistics();

        Map<String, Object> queryCache = new LinkedHashMap<>();
        result.put("queryCache", queryCache);

        queryCache.put("activation", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                queryStats.start().toInstant().atOffset(FormatUtils.DEFAULT_OFFSET)));
        queryCache.put("last_update", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                queryStats.since().toInstant().atOffset(FormatUtils.DEFAULT_OFFSET)));
        queryCache.put("hits", queryStats.getHitCount());
        queryCache.put("executions", queryStats.getExecutionCount());
        queryCache.put("evictions", queryStats.getEvictionCount());
        queryCache.put("total_hits", queryStats.getTotalHitCount());
        queryCache.put("total_executions", queryStats.getTotalExecutionCount());
        queryCache.put("total_evictions", queryStats.getTotalEvictionCount());

        List<Map<String, Object>> queryCacheDetails = new ArrayList<>();
        queryCache.put("details", queryCacheDetails);

        queryStats.keys().forEach(queryKey -> {
            Map<String, Object> queryKeyMap = new LinkedHashMap<>();
            queryKeyMap.put("query_key", queryKey.toString());
            queryCache.put("hits", queryStats.getHitCount(queryKey));
            queryCache.put("executions", queryStats.getExecutionCount(queryKey));
            queryCache.put("total_hits", queryStats.getTotalHitCount(queryKey));
            queryCache.put("total_executions", queryStats.getTotalExecutionCount(queryKey));
            queryCacheDetails.add(queryKeyMap);
        });

        return result;
    }

    @Override
    public void enableStatistics() {
        cacheStatisticsSPI().enable();
    }

    @Override
    public void disableStatistics() {
        cacheStatisticsSPI().disable();
    }

    @Override
    public void resetStatistics() {
        cacheStatisticsSPI().reset();
        queryStatistics().reset();
    }

    @Override
    public void evict(final Class<? extends Entity> entityClass, final String key) {
        entityManagerFactory.unwrap(OpenJPAEntityManagerFactory.class).getStoreCache().evict(entityClass, key);
    }

    @Override
    public void clearCache() {
        OpenJPAEntityManagerFactory emf = entityManagerFactory.unwrap(OpenJPAEntityManagerFactory.class);

        emf.getStoreCache().evictAll();
        emf.getQueryResultCache().evictAll();
    }
}
