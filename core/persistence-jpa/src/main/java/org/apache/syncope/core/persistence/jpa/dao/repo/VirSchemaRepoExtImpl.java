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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAVirSchema;

public class VirSchemaRepoExtImpl extends AbstractSchemaRepoExt implements VirSchemaRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    public VirSchemaRepoExtImpl(final ExternalResourceDAO resourceDAO, final EntityManager entityManager) {
        super(entityManager);
        this.resourceDAO = resourceDAO;
    }

    @Override
    public List<? extends VirSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, JPAVirSchema.class.getSimpleName(), VirSchema.class);
    }

    @Override
    public List<VirSchema> findByResourceAndAnyType(final String resource, final String anyType) {
        Query query = entityManager.createNativeQuery(
                "SELECT id FROM " + JPAVirSchema.TABLE + " e WHERE e.resource_id=? AND e.anyType_id=?");
        query.setParameter(1, resource);
        query.setParameter(2, anyType);

        @SuppressWarnings("unchecked")
        List<Object> results = query.getResultList();
        return results.stream().
                map(row -> entityManager.find(JPAVirSchema.class, row.toString())).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    @Override
    public VirSchema save(final VirSchema schema) {
        ((JPAVirSchema) schema).map2json();
        VirSchema merged = entityManager.merge(schema);
        ((JPAVirSchema) merged).postSave();
        return merged;

    }

    @Override
    public void deleteById(final String key) {
        Optional.ofNullable(entityManager.find(JPAVirSchema.class, key)).ifPresent(this::delete);
    }

    @Override
    public void delete(final VirSchema schema) {
        resourceDAO.deleteMapping(schema.getKey());

        Optional.ofNullable(schema.getAnyTypeClass()).
                ifPresent(anyTypeClass -> anyTypeClass.getVirSchemas().remove(schema));

        entityManager.remove(schema);
    }
}
