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
package org.apache.syncope.core.util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.types.AttributableType;

/**
 * Virtual Attribute Value cache.
 */
public final class VirAttrCache {

    /**
     * Elapsed time in seconds.
     */
    private final int ttl;

    /**
     * Max cache size.
     */
    private final int maxCacheSize;

    /**
     * Clean period.
     */
    private final int cleanPeriod;

    /**
     * Cache entries.
     */
    private final Map<VirAttrCacheKey, VirAttrCacheValue> cache = new HashMap<VirAttrCacheKey, VirAttrCacheValue>();

    public VirAttrCache(final int ttl, final int maxCacheSize, final int cleanPeriod) {
        this.ttl = ttl;
        this.maxCacheSize = maxCacheSize;
        this.cleanPeriod = cleanPeriod;

        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(
                new Runnable() {

                    @Override
                    public void run() {
                        synchronized (cache) {
                            freeCacheSpace(false);
                        }
                    }
                }, cleanPeriod, cleanPeriod, TimeUnit.MINUTES);
    }

    /**
     * Cache virtual attribute values.
     *
     * @param userId user id.
     * @param schemaName virtual attribute name.
     * @param values virtual attribute values.
     */
    public void put(final AttributableType type, final Long userId, final String schemaName, final List<String> values) {
        synchronized (cache) {
            // this operations (retrieve cache space and put entry on) have to be thread safe.

            if (cache.size() >= maxCacheSize) {
                freeCacheSpace(true);
            }

            cache.put(new VirAttrCacheKey(type, userId, schemaName), new VirAttrCacheValue(values));
        }
    }

    /**
     * Retrieve cached value. Return null in case of virtual attribute not cached.
     *
     * @param userId user id.
     * @param schemaName virtual attribute schema name.
     * @return cached values or null in case of virtual attribute not found.
     */
    public List<String> get(final AttributableType type, final Long userId, final String schemaName) {
        final VirAttrCacheValue value = cache.get(new VirAttrCacheKey(type, userId, schemaName));
        return isValidEntry(value) ? value.getValues() : null;
    }

    /**
     * Force entry expiring.
     *
     * @param userId user id.
     * @param schemaName virtual attribute schema name.
     */
    public void expire(final AttributableType type, final Long userId, final String schemaName) {
        final VirAttrCacheValue value = cache.get(new VirAttrCacheKey(type, userId, schemaName));
        if (isValidEntry(value)) {
            synchronized (cache) {
                value.forceExpiring();
            }
        }
    }

    /**
     * Remove expired entries if exist. If required, one entry at least (the latest recently used) will be taken off.
     * This method is not thread safe: the caller have to take care to synchronize the call.
     *
     * @param forceEscape if TRUE the latest recently used entry at least will be taken off.
     */
    private void freeCacheSpace(final boolean forceEscape) {
        final Set<VirAttrCacheKey> toBeRemoved = new HashSet<VirAttrCacheKey>();

        Map.Entry<VirAttrCacheKey, VirAttrCacheValue> latest = null;

        for (Map.Entry<VirAttrCacheKey, VirAttrCacheValue> entry : cache.entrySet()) {
            if (isValidEntry(entry.getValue())) {
                final Date date = entry.getValue().getLastAccessDate();
                if (latest == null || latest.getValue().getLastAccessDate().after(date)) {
                    latest = entry;
                }
            } else {
                toBeRemoved.add(entry.getKey());
            }
        }

        if (toBeRemoved.isEmpty() && forceEscape) {
            // remove the oldest entry.
            cache.remove(latest.getKey());
        } else {
            // remove expired entries.
            cache.keySet().removeAll(toBeRemoved);
        }
    }

    /**
     * Cache entry is valid if and only if value exist and it is not expired.
     *
     * @param value cache entry value.
     * @return TRUE if the value is valid; FALSE otherwise.
     */
    private boolean isValidEntry(final VirAttrCacheValue value) {
        final Date expiringDate = new Date(value == null ? 0 : value.getCreationDate().getTime() + ttl * 1000);
        return expiringDate.after(new Date());
    }
}
