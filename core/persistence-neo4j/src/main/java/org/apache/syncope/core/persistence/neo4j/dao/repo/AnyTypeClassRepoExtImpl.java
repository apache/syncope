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
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class AnyTypeClassRepoExtImpl implements AnyTypeClassRepoExt {

    protected final AnyTypeDAO anyTypeDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final GroupDAO groupDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache;

    protected final Cache<EntityCacheKey, Neo4jExternalResource> resourceCache;

    protected final Cache<EntityCacheKey, Neo4jGroup> groupCache;

    public AnyTypeClassRepoExtImpl(
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final GroupDAO groupDAO,
            final ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache,
            final Cache<EntityCacheKey, Neo4jExternalResource> resourceCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache) {

        this.anyTypeDAO = anyTypeDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.groupDAO = groupDAO;
        this.resourceDAO = resourceDAO;
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
        this.anyTypeCache = anyTypeCache;
        this.resourceCache = resourceCache;
        this.groupCache = groupCache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends AnyTypeClass> findById(final String key) {
        return neo4jTemplate.findById(key, Neo4jAnyTypeClass.class);
    }

    @Override
    public AnyTypeClass save(final AnyTypeClass anyTypeClass) {
        AnyTypeClass merged = neo4jTemplate.save(nodeValidator.validate(anyTypeClass));

        for (PlainSchema schema : merged.getPlainSchemas()) {
            schema.setAnyTypeClass(merged);
        }
        for (DerSchema schema : merged.getDerSchemas()) {
            schema.setAnyTypeClass(merged);
        }
        for (VirSchema schema : merged.getVirSchemas()) {
            schema.setAnyTypeClass(merged);
        }

        for (AnyType anyType : anyTypeDAO.findByClassesContaining(merged)) {
            anyType.getClasses().remove(merged);
            anyType.add(merged);

            anyTypeCache.put(EntityCacheKey.of(anyType.getKey()), (Neo4jAnyType) anyType);
        }

        resourceDAO.findAll().stream().filter(resource -> resource.getProvisions().stream().
                anyMatch(provision -> provision.getAuxClasses().contains(merged.getKey()))).
                forEach(resource -> {

                    resource.getProvisions().stream().
                            filter(provision -> provision.getAuxClasses().contains(merged.getKey())).
                            forEach(provision -> provision.getAuxClasses().remove(anyTypeClass.getKey()));

                    resourceCache.put(EntityCacheKey.of(resource.getKey()), (Neo4jExternalResource) resource);
                });

        return merged;
    }

    @Override
    public void deleteById(final String key) {
        AnyTypeClass anyTypeClass = findById(key).orElse(null);
        if (anyTypeClass == null) {
            return;
        }

        for (PlainSchema schema : plainSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }
        for (DerSchema schema : derSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }
        for (VirSchema schema : virSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        for (AnyType anyType : anyTypeDAO.findByClassesContaining(anyTypeClass)) {
            anyType.getClasses().remove(anyTypeClass);

            Optional.of(anyTypeCache.get(EntityCacheKey.of(anyType.getKey()))).
                    ifPresent(cached -> cached.getClasses().remove(anyTypeClass));
        }

        for (TypeExtension typeExt : groupDAO.findTypeExtensions(anyTypeClass)) {
            typeExt.getAuxClasses().remove(anyTypeClass);

            if (typeExt.getAuxClasses().isEmpty()) {
                typeExt.getGroup().getTypeExtensions().remove(typeExt);
                typeExt.setGroup(null);

                groupCache.remove(EntityCacheKey.of(typeExt.getGroup().getKey()));
            }
        }

        resourceDAO.findAll().stream().filter(resource -> resource.getProvisions().stream().
                anyMatch(provision -> provision.getAuxClasses().contains(anyTypeClass.getKey()))).
                forEach(resource -> {

                    resource.getProvisions().stream().
                            filter(provision -> provision.getAuxClasses().contains(anyTypeClass.getKey())).
                            forEach(provision -> provision.getAuxClasses().remove(anyTypeClass.getKey()));

                    resourceCache.put(EntityCacheKey.of(resource.getKey()), (Neo4jExternalResource) resource);
                });

        neo4jTemplate.deleteById(key, Neo4jAnyTypeClass.class);
    }
}
