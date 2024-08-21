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
import java.util.Optional;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jLinkedAccount;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class PlainSchemaRepoExtImpl extends AbstractSchemaRepoExt implements PlainSchemaRepoExt {

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final ExternalResourceDAO resourceDAO;

    protected final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache;

    public PlainSchemaRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache) {

        super(neo4jTemplate, neo4jClient, nodeValidator);
        this.anyUtilsFactory = anyUtilsFactory;
        this.resourceDAO = resourceDAO;
        this.plainSchemaCache = plainSchemaCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <S extends Schema> Cache<EntityCacheKey, S> cache() {
        return (Cache<EntityCacheKey, S>) plainSchemaCache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends PlainSchema> findById(final String key) {
        return findById(key, Neo4jPlainSchema.class, plainSchemaCache);
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends PlainSchema> findByIdLike(final String keyword) {
        return findByIdLike(Neo4jPlainSchema.NODE, Neo4jPlainSchema.class, keyword);
    }

    @Override
    public List<? extends PlainSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, Neo4jPlainSchema.class, PlainSchema.class);
    }

    @Override
    public <T extends PlainAttr<?>> boolean hasAttrs(final PlainSchema schema, final Class<T> reference) {
        String label;
        if (reference.isAssignableFrom(GPlainAttr.class)) {
            label = Neo4jGroup.NODE;
        } else if (reference.isAssignableFrom(APlainAttr.class)) {
            label = Neo4jAnyObject.NODE;
        } else if (reference.isAssignableFrom(LAPlainAttr.class)) {
            label = Neo4jLinkedAccount.NODE;
        } else {
            label = Neo4jUser.NODE;
        }

        return neo4jTemplate.count(
                "MATCH (n:" + label + ") "
                + "WHERE n.`plainAttrs." + schema.getKey() + "` "
                + "IS NOT NULL RETURN COUNT(n)") > 0;
    }

    @Override
    public PlainSchema save(final PlainSchema schema) {
        // unlink any implementation that was unlinked from plain schema
        neo4jTemplate.findById(schema.getKey(), Neo4jPlainSchema.class).ifPresent(before -> {
            if (before.getDropdownValueProvider() != null && schema.getDropdownValueProvider() == null) {
                deleteRelationship(
                        Neo4jPlainSchema.NODE,
                        Neo4jImplementation.NODE,
                        schema.getKey(),
                        before.getDropdownValueProvider().getKey(),
                        Neo4jPlainSchema.PLAIN_SCHEMA_DROPDOWN_VALUE_PROVIDER_REL);
            }
            if (before.getValidator() != null && schema.getValidator() == null) {
                deleteRelationship(
                        Neo4jPlainSchema.NODE,
                        Neo4jImplementation.NODE,
                        schema.getKey(),
                        before.getValidator().getKey(),
                        Neo4jPlainSchema.PLAIN_SCHEMA_ATTR_VALUE_VALIDATOR_REL);
            }
        });

        ((Neo4jSchema) schema).map2json();
        PlainSchema saved = neo4jTemplate.save(nodeValidator.validate(schema));
        ((Neo4jSchema) saved).postSave();

        plainSchemaCache.put(EntityCacheKey.of(schema.getKey()), (Neo4jPlainSchema) saved);

        return saved;
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(schema -> {
            resourceDAO.deleteMapping(key);

            Optional.ofNullable(schema.getAnyTypeClass()).ifPresent(atc -> atc.getPlainSchemas().remove(schema));

            plainSchemaCache.remove(EntityCacheKey.of(key));

            neo4jTemplate.deleteById(key, Neo4jPlainSchema.class);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttr<?>> void delete(final T plainAttr) {
        if (plainAttr.getOwner() != null) {
            ((Attributable<T>) plainAttr.getOwner()).remove(plainAttr);
        }
    }
}
