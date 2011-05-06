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
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.VirAttrDAO;
import org.syncope.core.persistence.dao.VirSchemaDAO;
import org.syncope.core.util.AttributableUtil;

@Repository
public class VirSchemaDAOImpl extends AbstractDAOImpl implements VirSchemaDAO {

    @Autowired
    private VirAttrDAO virtualAttributeDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    public <T extends AbstractVirSchema> T find(final String name,
            final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractVirSchema> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractVirSchema> T save(final T virtualSchema) {
        return entityManager.merge(virtualSchema);
    }

    @Override
    public void delete(final String name,
            final AttributableUtil attributableUtil) {

        final AbstractVirSchema virtualSchema =
                find(name, attributableUtil.virtualSchemaClass());

        if (virtualSchema == null) {
            return;
        }

        List<? extends AbstractVirAttr> attributes = getAttributes(
                virtualSchema,
                attributableUtil.virtualAttributeClass());

        final Set<Long> virtualAttributeIds =
                new HashSet<Long>(attributes.size());

        Class attributeClass = null;

        for (AbstractVirAttr attribute : attributes) {
            virtualAttributeIds.add(attribute.getId());
            attributeClass = attribute.getClass();
        }

        for (Long virtualAttributeId : virtualAttributeIds) {
            virtualAttributeDAO.delete(virtualAttributeId, attributeClass);
        }

        resourceDAO.deleteMappings(
                name, attributableUtil.virtualSourceMappingType());

        entityManager.remove(virtualSchema);
    }

    @Override
    public <T extends AbstractVirAttr> List<T> getAttributes(
            final AbstractVirSchema virtualSchema, final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e"
                + " WHERE e.virtualSchema=:schema");
        query.setParameter("schema", virtualSchema);

        return query.getResultList();
    }
}
