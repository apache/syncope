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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDelegation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jVirSchema;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;

public class Neo4jEntityCacheDAO implements EntityCacheDAO {

    protected final Map<Class<? extends Entity>, Cache<EntityCacheKey, ? extends Entity>> caches;

    public Neo4jEntityCacheDAO(
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache,
            final Cache<EntityCacheKey, Neo4jDelegation> delegationCache,
            final Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache,
            final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache,
            final Cache<EntityCacheKey, Neo4jImplementation> implementationCache,
            final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache,
            final Cache<EntityCacheKey, Neo4jRealm> realmCache,
            final Cache<EntityCacheKey, Neo4jRole> roleCache,
            final Cache<EntityCacheKey, Neo4jUser> userCache,
            final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache) {

        caches = new HashMap<>();
        caches.put(Neo4jAnyTypeClass.class, anyTypeCache);
        caches.put(Neo4jAnyObject.class, anyObjectCache);
        caches.put(Neo4jDelegation.class, delegationCache);
        caches.put(Neo4jDerSchema.class, derSchemaCache);
        caches.put(Neo4jExternalResource.class, externalResourceCache);
        caches.put(Neo4jGroup.class, groupCache);
        caches.put(Neo4jImplementation.class, implementationCache);
        caches.put(Neo4jPlainSchema.class, plainSchemaCache);
        caches.put(Neo4jRealm.class, realmCache);
        caches.put(Neo4jRole.class, roleCache);
        caches.put(Neo4jUser.class, userCache);
        caches.put(Neo4jVirSchema.class, virSchemaCache);
    }

    @Override
    public Map<String, Object> getStatistics() {
        return Map.of();
    }

    @Override
    public void enableStatistics() {
        // not supported
    }

    @Override
    public void disableStatistics() {
        // not supported
    }

    @Override
    public void resetStatistics() {
        // not supported
    }

    @Override
    public void evict(final Class<? extends Entity> entityClass, final String key) {
        Optional.ofNullable(caches.get(entityClass)).ifPresent(c -> c.remove(EntityCacheKey.of(key)));
    }

    @Override
    public void clearCache() {
        caches.values().forEach(Cache::removeAll);
    }
}
