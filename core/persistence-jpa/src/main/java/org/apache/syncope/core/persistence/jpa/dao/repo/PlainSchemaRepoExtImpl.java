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
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;

public class PlainSchemaRepoExtImpl extends AbstractSchemaRepoExt implements PlainSchemaRepoExt {

    protected static <T extends PlainAttr<?>> String getTable(final Class<T> plainAttrClass) {
        if (GPlainAttr.class.isAssignableFrom(plainAttrClass)) {
            return JPAGPlainAttr.TABLE;
        }
        if (APlainAttr.class.isAssignableFrom(plainAttrClass)) {
            return JPAAPlainAttr.TABLE;
        }
        return JPAUPlainAttr.TABLE;
    }

    public static <T extends PlainAttr<?>> Class<? extends AbstractPlainAttr<?>> getEntityReference(
            final Class<T> plainAttrClass) {

        return GPlainAttr.class.isAssignableFrom(plainAttrClass)
                ? JPAGPlainAttr.class
                : APlainAttr.class.isAssignableFrom(plainAttrClass)
                ? JPAAPlainAttr.class
                : UPlainAttr.class.isAssignableFrom(plainAttrClass)
                ? JPAUPlainAttr.class
                : null;
    }

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final ExternalResourceDAO resourceDAO;

    public PlainSchemaRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        super(entityManager);
        this.anyUtilsFactory = anyUtilsFactory;
        this.resourceDAO = resourceDAO;
    }

    @Override
    public List<? extends PlainSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, JPAPlainSchema.class.getSimpleName(), PlainSchema.class);
    }

    @Override
    public <T extends PlainAttr<?>> List<T> findAttrs(final PlainSchema schema, final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + getEntityReference(reference).getSimpleName()
                + " e WHERE e.schema=:schema", reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends PlainAttr<?>> boolean hasAttrs(final PlainSchema schema, final Class<T> reference) {
        String plainAttrTable = getTable(reference);
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(" + plainAttrTable + ".id) FROM " + JPAPlainSchema.TABLE
                + " JOIN " + plainAttrTable + " ON " + JPAPlainSchema.TABLE + ".id = " + plainAttrTable
                + ".schema_id WHERE " + JPAPlainSchema.TABLE + ".id = ?1");
        query.setParameter(1, schema.getKey());

        return ((Number) query.getSingleResult()).intValue() > 0;
    }

    @Override
    public PlainSchema save(final PlainSchema schema) {
        ((JPAPlainSchema) schema).map2json();
        return entityManager.merge(schema);
    }

    protected void deleteAttrs(final PlainSchema schema) {
        for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
            findAttrs(schema, anyUtilsFactory.getInstance(anyTypeKind).plainAttrClass()).forEach(this::delete);
        }
    }

    @Override
    public void deleteById(final String key) {
        PlainSchema schema = entityManager.find(JPAPlainSchema.class, key);
        if (schema == null) {
            return;
        }

        deleteAttrs(schema);

        resourceDAO.deleteMapping(key);

        Optional.ofNullable(schema.getAnyTypeClass()).ifPresent(c -> c.getPlainSchemas().remove(schema));

        entityManager.remove(schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlainAttr<?>> void delete(final T plainAttr) {
        if (plainAttr.getOwner() != null) {
            ((Attributable<T>) plainAttr.getOwner()).remove(plainAttr);
        }

        entityManager.remove(plainAttr);
    }
}
