/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.util.AttributableUtil;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl
        implements SchemaDAO {

    @Autowired
    private AttrDAO attributeDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    public <T extends AbstractSchema> T find(final String name,
            final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractSchema> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttr> List<T> getAttributes(
            final AbstractSchema schema, final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e"
                + " WHERE e.schema=:schema");
        query.setParameter("schema", schema);

        return query.getResultList();
    }

    @Override
    public <T extends AbstractSchema> T save(final T schema) {
        return entityManager.merge(schema);
    }

    @Override
    public void delete(final String name,
            final AttributableUtil attributableUtil) {

        AbstractSchema schema = find(name, attributableUtil.schemaClass());
        if (schema == null) {
            return;
        }

        List<? extends AbstractAttr> attributes = getAttributes(schema,
                attributableUtil.attributeClass());

        Set<Long> attributeIds = new HashSet<Long>(attributes.size());
        for (AbstractAttr attribute : attributes) {
            attributeIds.add(attribute.getId());
        }
        for (Long attributeId : attributeIds) {
            attributeDAO.delete(attributeId, attributableUtil.attributeClass());
        }

        resourceDAO.deleteMappings(name, attributableUtil.sourceMappingType());

        entityManager.remove(schema);
    }
}
