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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.AttrTemplateDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DerSchemaDAOImpl extends AbstractDAOImpl implements DerSchemaDAO {

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private AttrTemplateDAO attrTemplateDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    public <T extends AbstractDerSchema> T find(final String name, final Class<T> reference) {
        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractDerSchema> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery("SELECT e FROM " + reference.getSimpleName() + " e", reference);
        return query.getResultList();
    }

    @Override
    public <T extends AbstractDerAttr> List<T> findAttrs(
            final AbstractDerSchema schema, final Class<T> reference) {

        final StringBuilder queryString =
                new StringBuilder("SELECT e FROM ").append(reference.getSimpleName()).append(" e WHERE e.");
        if (reference.equals(UDerAttr.class)) {
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
    public <T extends AbstractDerSchema> T save(final T derSchema) {
        return entityManager.merge(derSchema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(final String name, final AttributableUtil attributableUtil) {
        final AbstractDerSchema schema = find(name, attributableUtil.derSchemaClass());
        if (schema == null) {
            return;
        }

        final Set<Long> attrIds = new HashSet<Long>();
        for (AbstractDerAttr attr : findAttrs(schema, attributableUtil.derAttrClass())) {
            attrIds.add(attr.getId());
        }
        for (Long attrId : attrIds) {
            derAttrDAO.delete(attrId, attributableUtil.derAttrClass());
        }

        if (attributableUtil.getType() != AttributableType.USER) {
            for (Iterator<Number> it = attrTemplateDAO.
                    findBySchemaName(schema.getName(), attributableUtil.derAttrTemplateClass()).iterator();
                    it.hasNext();) {

                attrTemplateDAO.delete(it.next().longValue(), attributableUtil.derAttrTemplateClass());
            }
        }

        resourceDAO.deleteMapping(name, attributableUtil.derIntMappingType(), UMappingItem.class);

        entityManager.remove(schema);
    }
}
