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

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.JPAVirSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAVirSchemaDAO extends AbstractDAO<VirSchema, String> implements VirSchemaDAO {

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Override
    public VirSchema find(final String key) {
        return entityManager.find(JPAVirSchema.class, key);
    }

    @Override
    public List<VirSchema> findByAnyTypeClass(final AnyTypeClass anyTypeClass) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAVirSchema.class.getSimpleName()).
                append(" e WHERE e.anyTypeClass=:anyTypeClass");

        TypedQuery<VirSchema> query = entityManager.createQuery(queryString.toString(), VirSchema.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    public List<VirSchema> findAll() {
        TypedQuery<VirSchema> query = entityManager.createQuery(
                "SELECT e FROM " + JPAVirSchema.class.getSimpleName() + " e", VirSchema.class);
        return query.getResultList();
    }

    @Override
    public <T extends VirAttr<?>> List<T> findAttrs(final VirSchema schema, final Class<T> reference) {
        final StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(((JPAVirAttrDAO) virAttrDAO).getJPAEntityReference(reference).getSimpleName()).
                append(" e WHERE e.schema=:schema");

        TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public VirSchema save(final VirSchema virSchema) {
        return entityManager.merge(virSchema);
    }

    @Override
    public void delete(final String key) {
        final VirSchema schema = find(key);
        if (schema == null) {
            return;
        }

        AnyUtilsFactory anyUtilsFactory = new JPAAnyUtilsFactory();
        for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
            AnyUtils anyUtils = anyUtilsFactory.getInstance(anyTypeKind);

            for (VirAttr<?> attr : findAttrs(schema, anyUtils.virAttrClass())) {
                virAttrDAO.delete(attr.getKey(), anyUtils.virAttrClass());
            }

            resourceDAO.deleteMapping(key, anyUtils.virIntMappingType());
        }

        schema.getAnyTypeClass().remove(schema);

        entityManager.remove(schema);
    }
}
