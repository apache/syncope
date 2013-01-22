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
import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.user.UMappingItem;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl implements SchemaDAO {

    @Autowired
    private AttrDAO attributeDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    public <T extends AbstractSchema> T find(final String name, final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractSchema> List<T> findAll(final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery("SELECT e FROM " + reference.getSimpleName() + " e", reference);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttr> List<T> getAttributes(final AbstractSchema schema, final Class<T> reference) {
        TypedQuery<T> query = entityManager.createQuery("SELECT e FROM " + reference.getSimpleName() + " e"
                + " WHERE e.schema=:schema", reference);
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractSchema> T save(final T schema) {
        return entityManager.merge(schema);
    }

    @Override
    public void delete(final String name, final AttributableUtil attributableUtil) {
        AbstractSchema schema = find(name, attributableUtil.schemaClass());
        if (schema == null) {
            return;
        }

        List<? extends AbstractAttr> attributes = getAttributes(schema, attributableUtil.attrClass());

        Set<Long> attributeIds = new HashSet<Long>(attributes.size());
        for (AbstractAttr attribute : attributes) {
            attributeIds.add(attribute.getId());
        }
        for (Long attributeId : attributeIds) {
            attributeDAO.delete(attributeId, attributableUtil.attrClass());
        }

        resourceDAO.deleteMapping(name, attributableUtil.intMappingType(), UMappingItem.class);

        entityManager.remove(schema);
    }
}
