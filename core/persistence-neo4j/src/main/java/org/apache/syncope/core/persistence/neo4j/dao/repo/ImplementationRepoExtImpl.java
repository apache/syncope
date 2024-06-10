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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class ImplementationRepoExtImpl extends AbstractDAO implements ImplementationRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityCacheDAO entityCacheDAO;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jImplementation> cache;

    public ImplementationRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final EntityCacheDAO entityCacheDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jImplementation> cache) {

        super(neo4jTemplate, neo4jClient);
        this.resourceDAO = resourceDAO;
        this.entityCacheDAO = entityCacheDAO;
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends Implementation> findById(final String key) {
        EntityCacheKey cacheKey = EntityCacheKey.of(key);
        return Optional.ofNullable(cache.get(cacheKey)).
                or(() -> neo4jTemplate.findById(key, Neo4jImplementation.class).
                map(value -> {
                    cache.put(cacheKey, value);
                    return value;
                }));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Implementation> findByTypeAndKeyword(final String type, final String keyword) {
        StringBuilder query = new StringBuilder("MATCH (n:" + Neo4jImplementation.NODE + " {type: $type}) ");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", type);

        if (StringUtils.isNotBlank(keyword)) {
            query.append("WHERE n.id =~ $keyword ");
            parameters.put("keyword", keyword.replace("%", ".*").replaceAll("_", "\\\\_") + ".*");
        }

        query.append("RETURN n.id");

        return toList(
                neo4jClient.query(query.toString()).bindAll(parameters).fetch().all(),
                "n.id",
                Neo4jImplementation.class,
                cache);
    }

    @Override
    public Implementation save(final Implementation implementation) {
        Implementation saved = neo4jTemplate.save(nodeValidator.validate(implementation));

        ImplementationManager.purge(saved.getKey());

        cache.put(EntityCacheKey.of(saved.getKey()), (Neo4jImplementation) saved);

        resourceDAO.findByProvisionSorter(saved).
                forEach(resource -> entityCacheDAO.evict(Neo4jExternalResource.class, resource.getKey()));

        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jImplementation.class).ifPresent(implementation -> {
            cache.remove(EntityCacheKey.of(key));

            neo4jTemplate.deleteById(key, Neo4jImplementation.class);

            ImplementationManager.purge(key);
        });
    }
}
