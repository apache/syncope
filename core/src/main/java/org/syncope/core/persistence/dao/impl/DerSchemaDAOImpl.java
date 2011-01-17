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
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.DerAttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;

@Repository
public class DerSchemaDAOImpl extends AbstractDAOImpl implements DerSchemaDAO {

    @Autowired
    private DerAttrDAO derivedAttributeDAO;

    @Override
    public <T extends AbstractDerSchema> T find(final String name,
            final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    public <T extends AbstractDerSchema> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractDerSchema> T save(final T derivedSchema) {
        return entityManager.merge(derivedSchema);
    }

    @Override
    public <T extends AbstractDerSchema> void delete(final String name,
            final Class<T> reference) {

        T derivedSchema = find(name, reference);
        if (derivedSchema == null) {
            return;
        }

        for (AbstractSchema schema : derivedSchema.getSchemas()) {
            schema.removeDerivedSchema(derivedSchema);
        }
        derivedSchema.getSchemas().clear();

        Set<Long> derivedAttributeIds =
                new HashSet<Long>(derivedSchema.getDerivedAttributes().size());
        Class attributeClass = null;
        for (AbstractDerAttr attribute :
                derivedSchema.getDerivedAttributes()) {

            derivedAttributeIds.add(attribute.getId());
            attributeClass = attribute.getClass();
        }
        for (Long derivedAttributeId : derivedAttributeIds) {
            derivedAttributeDAO.delete(derivedAttributeId, attributeClass);
        }

        entityManager.remove(derivedSchema);
    }
}
