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
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GMappingItem;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAPlainSchemaDAO extends AbstractDAO<PlainSchema, String> implements PlainSchemaDAO {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private AttrTemplateDAO<PlainSchema> attrTemplateDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    private <T extends PlainSchema> Class<? extends AbstractPlainSchema> getJPAEntityReference(
            final Class<T> reference) {

        return CPlainSchema.class.isAssignableFrom(reference)
                ? JPACPlainSchema.class
                : GPlainSchema.class.isAssignableFrom(reference)
                        ? JPAGPlainSchema.class
                        : MPlainSchema.class.isAssignableFrom(reference)
                                ? JPAMPlainSchema.class
                                : UPlainSchema.class.isAssignableFrom(reference)
                                        ? JPAUPlainSchema.class
                                        : null;
    }

    @Override
    public <T extends PlainSchema> T find(final String key, final Class<T> reference) {
        return reference.cast(entityManager.find(getJPAEntityReference(reference), key));
    }

    @Override
    public <T extends PlainSchema> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery(
                "SELECT e FROM " + getJPAEntityReference(reference).getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends PlainAttr> List<T> findAttrs(final PlainSchema schema, final Class<T> reference) {
        final StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(((JPAPlainAttrDAO) plainAttrDAO).getJPAEntityReference(reference).getSimpleName()).
                append(" e WHERE e.");
        if (GPlainAttr.class.isAssignableFrom(reference) || MPlainAttr.class.isAssignableFrom(reference)) {
            queryString.append("template.");
        }
        queryString.append("schema=:schema");

        TypedQuery<T> query = entityManager.createQuery(queryString.toString(), reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends PlainSchema> T save(final T schema) {
        return entityManager.merge(schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(final String key, final AttributableUtils attributableUtil) {
        PlainSchema schema = find(key, attributableUtil.plainSchemaClass());
        if (schema == null) {
            return;
        }

        CollectionUtils.forAllDo(findAttrs(schema, attributableUtil.plainAttrClass()), new Closure<PlainAttr>() {

            @Override
            public void execute(final PlainAttr input) {
                plainAttrDAO.delete(input.getKey(), attributableUtil.plainAttrClass());
            }

        });

        if (attributableUtil.getType() == AttributableType.GROUP
                || attributableUtil.getType() == AttributableType.MEMBERSHIP) {

            CollectionUtils.forAllDo(attrTemplateDAO.
                    findBySchemaName(schema.getKey(), attributableUtil.plainAttrTemplateClass()).iterator(),
                    new Closure<Number>() {

                        @Override
                        public void execute(final Number input) {
                            attrTemplateDAO.delete(input.longValue(), attributableUtil.plainAttrTemplateClass());
                        }

                    });
        }

        resourceDAO.deleteMapping(key, attributableUtil.plainIntMappingType(), UMappingItem.class);
        resourceDAO.deleteMapping(key, attributableUtil.plainIntMappingType(), GMappingItem.class);

        entityManager.remove(schema);
    }
}
