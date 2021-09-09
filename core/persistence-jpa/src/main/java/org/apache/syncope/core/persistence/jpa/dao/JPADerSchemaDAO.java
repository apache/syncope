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
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPADerSchema;

public class JPADerSchemaDAO extends AbstractDAO<DerSchema> implements DerSchemaDAO {

    protected final ExternalResourceDAO resourceDAO;

    public JPADerSchemaDAO(final ExternalResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    @Override
    public DerSchema find(final String key) {
        return entityManager().find(JPADerSchema.class, key);
    }

    @Override
    public List<DerSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPADerSchema.class.getSimpleName()).
                append(" e WHERE ");
        anyTypeClasses.forEach(anyTypeClass -> queryString.
                append("e.anyTypeClass.id='").
                append(anyTypeClass.getKey()).append("' OR "));

        TypedQuery<DerSchema> query = entityManager().createQuery(
                queryString.substring(0, queryString.length() - 4), DerSchema.class);

        return query.getResultList();
    }

    @Override
    public List<DerSchema> findByKeyword(final String keyword) {
        TypedQuery<DerSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPADerSchema.class.getSimpleName() + " e"
                + " WHERE e.id LIKE :keyword", DerSchema.class);
        query.setParameter("keyword", keyword);
        return query.getResultList();
    }

    @Override
    public List<DerSchema> findAll() {
        TypedQuery<DerSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPADerSchema.class.getSimpleName() + " e", DerSchema.class);
        return query.getResultList();
    }

    @Override
    public DerSchema save(final DerSchema derSchema) {
        return entityManager().merge(derSchema);
    }

    @Override
    public void delete(final String key) {
        final DerSchema schema = find(key);
        if (schema == null) {
            return;
        }

        schema.getLabels().forEach(label -> label.setSchema(null));

        resourceDAO.deleteMapping(key);

        if (schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().getDerSchemas().remove(schema);
        }

        entityManager().remove(schema);
    }
}
