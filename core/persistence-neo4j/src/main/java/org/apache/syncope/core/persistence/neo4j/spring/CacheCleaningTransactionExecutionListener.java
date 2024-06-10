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
package org.apache.syncope.core.persistence.neo4j.spring;

import javax.cache.Cache;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
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
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

public class CacheCleaningTransactionExecutionListener implements TransactionExecutionListener {

    protected final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache;

    protected final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache;

    protected final Cache<EntityCacheKey, Neo4jDelegation> delegationCache;

    protected final Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache;

    protected final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache;

    protected final Cache<EntityCacheKey, Neo4jGroup> groupCache;

    protected final Cache<EntityCacheKey, Neo4jImplementation> implementationCache;

    protected final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache;

    protected final Cache<EntityCacheKey, Neo4jRealm> realmCache;

    protected final Cache<EntityCacheKey, Neo4jRole> roleCache;

    protected final Cache<EntityCacheKey, Neo4jUser> userCache;

    protected final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache;

    public CacheCleaningTransactionExecutionListener(
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

        this.anyTypeCache = anyTypeCache;
        this.anyObjectCache = anyObjectCache;
        this.delegationCache = delegationCache;
        this.derSchemaCache = derSchemaCache;
        this.externalResourceCache = externalResourceCache;
        this.groupCache = groupCache;
        this.implementationCache = implementationCache;
        this.plainSchemaCache = plainSchemaCache;
        this.realmCache = realmCache;
        this.roleCache = roleCache;
        this.userCache = userCache;
        this.virSchemaCache = virSchemaCache;
    }

    @Override
    public void afterRollback(
            final TransactionExecution transaction,
            final Throwable rollbackFailure) {

        if (transaction.getTransactionName().contains("AnyType")
                && !transaction.getTransactionName().contains("AnyTypeClass")) {

            anyTypeCache.removeAll();
        } else if (transaction.getTransactionName().contains("AnyObject")) {
            anyObjectCache.removeAll();
        } else if (transaction.getTransactionName().contains("Delegation")) {
            delegationCache.removeAll();
        } else if (transaction.getTransactionName().contains("Schema")) {
            derSchemaCache.removeAll();
            plainSchemaCache.removeAll();
            virSchemaCache.removeAll();
        } else if (transaction.getTransactionName().contains("Resource")) {
            externalResourceCache.removeAll();
        } else if (transaction.getTransactionName().contains("Group")) {
            groupCache.removeAll();
        } else if (transaction.getTransactionName().contains("Implementation")) {
            implementationCache.removeAll();
        } else if (transaction.getTransactionName().contains("Realm")) {
            realmCache.removeAll();
        } else if (transaction.getTransactionName().contains("Role")) {
            roleCache.removeAll();
        } else if (transaction.getTransactionName().contains("User")) {
            userCache.removeAll();
        }
    }
}
