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
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;

public class JPAPlainSchemaDAO extends AbstractDAO<PlainSchema> implements PlainSchemaDAO {

    private final AnyUtilsFactory anyUtilsFactory;

    private final PlainAttrDAO plainAttrDAO;

    private final ExternalResourceDAO resourceDAO;

    public JPAPlainSchemaDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrDAO plainAttrDAO,
            final ExternalResourceDAO resourceDAO) {

        this.anyUtilsFactory = anyUtilsFactory;
        this.plainAttrDAO = plainAttrDAO;
        this.resourceDAO = resourceDAO;
    }

    @Override
    public PlainSchema find(final String key) {
        return entityManager().find(JPAPlainSchema.class, key);
    }

    @Override
    public List<PlainSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAPlainSchema.class.getSimpleName()).
                append(" e WHERE ");
        anyTypeClasses.forEach(anyTypeClass -> queryString.
                append("e.anyTypeClass.id='").
                append(anyTypeClass.getKey()).append("' OR "));

        TypedQuery<PlainSchema> query = entityManager().createQuery(
                queryString.substring(0, queryString.length() - 4), PlainSchema.class);

        return query.getResultList();
    }

    @Override
    public List<PlainSchema> findByValidator(final Implementation validator) {
        TypedQuery<PlainSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPlainSchema.class.getSimpleName()
                + " e WHERE e.validator=:validator", PlainSchema.class);
        query.setParameter("validator", validator);

        return query.getResultList();
    }

    @Override
    public List<PlainSchema> findByKeyword(final String keyword) {
        TypedQuery<PlainSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPlainSchema.class.getSimpleName() + " e"
                + " WHERE e.id LIKE :keyword", PlainSchema.class);
        query.setParameter("keyword", keyword);
        return query.getResultList();
    }

    @Override
    public List<PlainSchema> findAll() {
        TypedQuery<PlainSchema> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPlainSchema.class.getSimpleName() + " e", PlainSchema.class);
        return query.getResultList();
    }

    @Override
    public <T extends PlainAttr<?>> List<T> findAttrs(final PlainSchema schema, final Class<T> reference) {
        TypedQuery<T> query = entityManager().createQuery(
                "SELECT e FROM " + JPAPlainAttrDAO.getEntityReference(reference).getSimpleName()
                + " e WHERE e.schema=:schema", reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends PlainAttr<?>> boolean hasAttrs(final PlainSchema schema, final Class<T> reference) {
        String plainAttrTable = getPlainAttrTable(reference);
        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(" + plainAttrTable + ".id) FROM " + JPAPlainSchema.TABLE
                + " JOIN " + plainAttrTable + " ON " + JPAPlainSchema.TABLE + ".id = " + plainAttrTable
                + ".schema_id WHERE " + JPAPlainSchema.TABLE + ".id = ?1");
        query.setParameter(1, schema.getKey());

        return ((Number) query.getSingleResult()).intValue() > 0;
    }

    @Override
    public PlainSchema save(final PlainSchema schema) {
        return entityManager().merge(schema);
    }

    protected void deleteAttrs(final PlainSchema schema) {
        for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
            findAttrs(schema, anyUtilsFactory.getInstance(anyTypeKind).plainAttrClass()).
                    forEach(attr -> plainAttrDAO.delete(attr));
        }
    }

    @Override
    public void delete(final String key) {
        PlainSchema schema = find(key);
        if (schema == null) {
            return;
        }

        schema.getLabels().forEach(label -> label.setSchema(null));

        deleteAttrs(schema);

        resourceDAO.deleteMapping(key);

        if (schema.getAnyTypeClass() != null) {
            schema.getAnyTypeClass().getPlainSchemas().remove(schema);
        }

        entityManager().remove(schema);
    }

    private <T extends PlainAttr<?>> String getPlainAttrTable(final Class<T> plainAttrClass) {
        if (GPlainAttr.class.isAssignableFrom(plainAttrClass)) {
            return JPAGPlainAttr.TABLE;
        }
        if (APlainAttr.class.isAssignableFrom(plainAttrClass)) {
            return JPAAPlainAttr.TABLE;
        }
        return JPAUPlainAttr.TABLE;
    }
}
