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
package org.apache.syncope.core.provisioning.java.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;

/**
 * In-memory (HashMap) virtual attribute value cache implementation.
 */
public class MemoryVirAttrCache implements VirAttrCache {

    /**
     * Elapsed time in seconds.
     */
    protected int ttl;

    /**
     * Max cache size.
     */
    protected int maxCacheSize;

    /**
     * Cache entries.
     */
    protected final Map<VirAttrCacheKey, VirAttrCacheValue> cache = new HashMap<>();

    public MemoryVirAttrCache(final int ttl, final int maxCacheSize) {
        this.ttl = ttl;
        this.maxCacheSize = maxCacheSize;
    }

    @Override
    public void put(
            final String type,
            final String key,
            final String schemaKey,
            final VirAttrCacheValue value) {

        synchronized (cache) {
            // this operations (retrieve cache space and put entry on) have to be thread safe.
            if (this.cache.size() >= this.maxCacheSize) {
                free();
            }

            cache.put(new VirAttrCacheKey(type, key, schemaKey), value);
        }
    }

    @Override
    public VirAttrCacheValue get(final String type, final String key, final String schemaKey) {
        return cache.get(new VirAttrCacheKey(type, key, schemaKey));
    }

    @Override
    public void expire(final String type, final String key, final String schemaKey) {
        final VirAttrCacheValue value = cache.get(new VirAttrCacheKey(type, key, schemaKey));
        if (isValidEntry(value)) {
            synchronized (cache) {
                value.forceExpiring();
            }
        }
    }

    /**
     * Remove expired entries if exist. If required, one entry at least (the latest recently used) will be taken off.
     * This method is not thread safe: the caller have to take care to synchronize the call.
     */
    private void free() {
        final Set<VirAttrCacheKey> toBeRemoved = new HashSet<>();

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

        if (toBeRemoved.isEmpty() && latest != null) {
            // remove the oldest entry
            cache.remove(latest.getKey());
        } else {
            // remove expired entries
            cache.keySet().removeAll(toBeRemoved);
        }
    }

    /**
     * Cache entry is valid if and only if value exist and it is not expired.
     *
     * @param value cache entry value.
     * @return TRUE if the value is valid; FALSE otherwise.
     */
    @Override
    public boolean isValidEntry(final VirAttrCacheValue value) {
        final Date expiringDate = new Date(value == null ? 0 : value.getCreationDate().getTime() + ttl * 1000);
        return expiringDate.after(new Date());
    }
}
