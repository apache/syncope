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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jConnInstance;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class ConnInstanceRepoExtImpl extends AbstractDAO implements ConnInstanceRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final Cache<EntityCacheKey, Neo4jExternalResource> resourceCache;

    protected final Cache<EntityCacheKey, Neo4jConnInstance> cache;

    protected final NodeValidator nodeValidator;

    public ConnInstanceRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final Cache<EntityCacheKey, Neo4jExternalResource> resourceCache,
            final Cache<EntityCacheKey, Neo4jConnInstance> connInstanceCache,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.resourceDAO = resourceDAO;
        this.nodeValidator = nodeValidator;
        this.resourceCache = resourceCache;
        this.cache = connInstanceCache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends Neo4jConnInstance> findById(final String key) {
        return findById(key, Neo4jConnInstance.class, cache);
    }

    @Transactional(readOnly = true)
    @Override
    public ConnInstance authFind(final String key) {
        ConnInstance connInstance = neo4jTemplate.findById(key, Neo4jConnInstance.class).orElse(null);
        if (connInstance == null) {
            return null;
        }

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_READ);
        if (authRealms == null || authRealms.isEmpty()
                || authRealms.stream().noneMatch(
                        realm -> connInstance.getAdminRealm().getFullPath().startsWith(realm))) {

            throw new DelegatedAdministrationException(
                    connInstance.getAdminRealm().getFullPath(),
                    ConnInstance.class.getSimpleName(),
                    connInstance.getKey());
        }

        return connInstance;
    }

    @Override
    public List<? extends ConnInstance> findAll() {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_LIST);
        if (CollectionUtils.isEmpty(authRealms)) {
            return List.of();
        }

        List<Neo4jConnInstance> all = toList(neo4jClient.query(
                "MATCH (n:" + Neo4jConnInstance.NODE + ") RETURN n.id").fetch().all(),
                "n.id",
                Neo4jConnInstance.class,
                cache);
        return all.stream().filter(connInstance -> authRealms.stream().
                anyMatch(realm -> connInstance.getAdminRealm().getFullPath().startsWith(realm))).
                toList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        ((Neo4jConnInstance) connector).list2json();
        ConnInstance saved = neo4jTemplate.save(nodeValidator.validate(connector));
        ((Neo4jConnInstance) saved).postSave();

        resourceDAO.findByConnInstance(saved.getKey()).
                forEach(resource -> resourceCache.remove(EntityCacheKey.of(resource.getKey())));

        cache.put(EntityCacheKey.of(saved.getKey()), (Neo4jConnInstance) saved);

        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jConnInstance.class).ifPresent(connInstance -> {
            resourceDAO.findByConnInstance(connInstance.getKey()).stream().
                    map(ExternalResource::getKey).toList().forEach(resourceDAO::deleteById);

            cache.remove(EntityCacheKey.of(key));

            neo4jTemplate.deleteById(key, Neo4jConnInstance.class);
        });
    }
}
