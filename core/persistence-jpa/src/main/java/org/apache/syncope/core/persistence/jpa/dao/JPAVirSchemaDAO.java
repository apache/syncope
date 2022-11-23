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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAVirSchema;

public class JPAVirSchemaDAO extends AbstractDAO<VirSchema> implements VirSchemaDAO {

    protected final ExternalResourceDAO resourceDAO;

    public JPAVirSchemaDAO(final ExternalResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    @Override
    public VirSchema find(final String key) {
        return entityManager().find(JPAVirSchema.class, key);
    }

    @Override
    public List<VirSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAVirSchema.class.getSimpleName()).
                append(" e WHERE ");
        anyTypeClasses.forEach(anyTypeClass -> queryString.
                append("e.anyTypeClass.id='").
                append(anyTypeClass.getKey()).append("' OR "));

        TypedQuery<VirSchema> query = entityManager().createQuery(
                queryString.substring(0, queryString.length() - 4), VirSchema.class);

        return query.getResultList();
    }

    @Override
    public List<String> find(final ExternalResource resource) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + JPAVirSchema.TABLE + " e WHERE e.resource_id=?");
        query.setParameter(1, resource.getKey());

        @SuppressWarnings("unchecked")
        List<Object> results = query.getResultList();
        return results.stream().
                map(Object::toString).
                collect(Collectors.toList());
    }

    @Override
    public List<VirSchema> find(final String resource, final String anyType) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + JPAVirSchema.TABLE + " e WHERE e.resource_id=? AND e.anyType_id=?");
        query.setParameter(1, resource);
        query.setParameter(2, anyType);

        @SuppressWarnings("unchecked")
        List<Object> results = query.getResultList();
        return results.stream().
                map(row -> find(row.toString())).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    @Override
    public List<VirSchema> findByKeyword(final String keyword) {
        TypedQuery<VirSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAVirSchema.class.getSimpleName() + " e"
                + " WHERE e.id LIKE :keyword", VirSchema.class);
        query.setParameter("keyword", keyword);
        return query.getResultList();
    }

    @Override
    public List<VirSchema> findAll() {
        TypedQuery<VirSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAVirSchema.class.getSimpleName() + " e", VirSchema.class);
        return query.getResultList();
    }

    @Override
    public VirSchema save(final VirSchema schema) {
        ((JPAVirSchema) schema).map2json();
        return entityManager().merge(schema);
    }

    @Override
    public void delete(final String key) {
        VirSchema schema = find(key);
        if (schema == null) {
            return;
        }

        resourceDAO.deleteMapping(key);

        if (schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().getVirSchemas().remove(schema);
        }

        entityManager().remove(schema);
    }
}
