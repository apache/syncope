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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jVirSchema;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class VirSchemaRepoExtImpl extends AbstractSchemaRepoExt implements VirSchemaRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache;

    public VirSchemaRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache) {

        super(neo4jTemplate, neo4jClient, nodeValidator);
        this.resourceDAO = resourceDAO;
        this.virSchemaCache = virSchemaCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <S extends Schema> Cache<EntityCacheKey, S> cache() {
        return (Cache<EntityCacheKey, S>) virSchemaCache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends VirSchema> findById(final String key) {
        return findById(key, Neo4jVirSchema.class, virSchemaCache);
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends VirSchema> findByIdLike(final String keyword) {
        return findByIdLike(Neo4jVirSchema.NODE, Neo4jVirSchema.class, keyword);
    }

    @Override
    public List<? extends VirSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, Neo4jVirSchema.class, VirSchema.class);
    }

    @Override
    public List<? extends VirSchema> findByResource(final ExternalResource resource) {
        return findByRelationship(
                Neo4jVirSchema.NODE,
                Neo4jExternalResource.NODE,
                resource.getKey(),
                Neo4jVirSchema.VIRSCHEMA_RESOURCE_REL,
                Neo4jVirSchema.class,
                virSchemaCache);
    }

    @Override
    public List<VirSchema> findByResourceAndAnyType(final String resource, final String anyType) {
        return toList(neo4jClient.query(
                "MATCH (a:" + Neo4jAnyType.NODE + " {id: $anyTypeId})-"
                + "[:" + Neo4jVirSchema.VIRSCHEMA_ANYTYPE_REL + "]-"
                + "(n:" + Neo4jVirSchema.NODE + ")-"
                + "[:" + Neo4jVirSchema.VIRSCHEMA_RESOURCE_REL + "]-"
                + "(r:" + Neo4jExternalResource.NODE + " {id: $resourceId}) "
                + "RETURN n.id").bindAll(Map.of("resourceId", resource, "anyTypeId", anyType)).fetch().all(),
                "n.id",
                Neo4jVirSchema.class,
                virSchemaCache);
    }

    @Override
    public VirSchema save(final VirSchema schema) {
        ((Neo4jSchema) schema).map2json();
        VirSchema saved = neo4jTemplate.save(nodeValidator.validate(schema));
        ((Neo4jSchema) saved).postSave();

        virSchemaCache.put(EntityCacheKey.of(schema.getKey()), (Neo4jVirSchema) saved);

        return saved;
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void delete(final VirSchema schema) {
        resourceDAO.deleteMapping(schema.getKey());

        Optional.ofNullable(schema.getAnyTypeClass()).ifPresent(atc -> atc.getVirSchemas().remove(schema));

        virSchemaCache.remove(EntityCacheKey.of(schema.getKey()));

        neo4jTemplate.deleteById(schema.getKey(), Neo4jVirSchema.class);
    }
}
