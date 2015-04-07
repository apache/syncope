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
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.dao.AttrTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDerSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPADerSchemaDAO extends AbstractDAO<DerSchema, String> implements DerSchemaDAO {

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private AttrTemplateDAO<DerSchema> attrTemplateDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    private <T extends DerSchema> Class<? extends AbstractDerSchema> getJPAEntityReference(final Class<T> reference) {
        return GDerSchema.class.isAssignableFrom(reference)
                ? JPAGDerSchema.class
                : MDerSchema.class.isAssignableFrom(reference)
                        ? JPAMDerSchema.class
                        : UDerSchema.class.isAssignableFrom(reference)
                                ? JPAUDerSchema.class
                                : null;
    }

    @Override
    public <T extends DerSchema> T find(final String key, final Class<T> reference) {
        return reference.cast(entityManager.find(getJPAEntityReference(reference), key));
    }

    @Override
    public <T extends DerSchema> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + getJPAEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends DerAttr> List<T> findAttrs(final DerSchema schema, final Class<T> reference) {
        final StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(((JPADerAttrDAO) derAttrDAO).getJPAEntityReference(reference).getSimpleName()).
                append(" e WHERE e.");
        if (UDerAttr.class.isAssignableFrom(reference)) {
            queryString.append("derSchema");
        } else {
            queryString.append("template.schema");
        }
        queryString.append("=:schema");

        TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends DerSchema> T save(final T derSchema) {
        return entityManager.merge(derSchema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(final String key, final AttributableUtil attributableUtil) {
        final DerSchema schema = find(key, attributableUtil.derSchemaClass());
        if (schema == null) {
            return;
        }

        CollectionUtils.forAllDo(findAttrs(schema, attributableUtil.derAttrClass()), new Closure<DerAttr>() {

            @Override
            public void execute(final DerAttr input) {
                derAttrDAO.delete(input.getKey(), attributableUtil.derAttrClass());
            }

        });

        if (attributableUtil.getType() != AttributableType.USER) {
            CollectionUtils.forAllDo(attrTemplateDAO.
                    findBySchemaName(schema.getKey(), attributableUtil.derAttrTemplateClass()).iterator(),
                    new Closure<Number>() {

                        @Override
                        public void execute(final Number input) {
                            attrTemplateDAO.delete(input.longValue(), attributableUtil.derAttrTemplateClass());
                        }

                    });
        }

        resourceDAO.deleteMapping(key, attributableUtil.derIntMappingType(), UMappingItem.class);

        entityManager.remove(schema);
    }
}
