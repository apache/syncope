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
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;
import org.syncope.types.SchemaType;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl
        implements SchemaDAO {

    @Autowired
    private AttributeDAO attributeDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> T find(final String name,
            final Class<T> reference) {

        return entityManager.find(reference, name);
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends AbstractSchema> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createNamedQuery(
                reference.getSimpleName() + ".findAll");

        return query.getResultList();
    }

    @Override
    public <T extends AbstractSchema> T save(final T schema)
            throws MultiUniqueValueException {

        if (schema.isMultivalue() && schema.isUniquevalue()) {
            throw new MultiUniqueValueException(schema);
        }

        return entityManager.merge(schema);
    }

    @Override
    public <T extends AbstractSchema> void delete(String name,
            Class<T> reference) {

        T schema = find(name, reference);
        if (schema == null) {
            return;
        }

        for (AbstractDerivedSchema derivedSchema : schema.getDerivedSchemas()) {
            derivedSchema.removeSchema(schema);
        }
        schema.getDerivedSchemas().clear();

        Set<Long> attributeIds =
                new HashSet<Long>(schema.getAttributes().size());
        Class attributeClass = null;
        for (AbstractAttribute attribute : schema.getAttributes()) {
            attributeIds.add(attribute.getId());
            attributeClass = attribute.getClass();
        }
        for (Long attributeId : attributeIds) {
            attributeDAO.delete(attributeId, attributeClass);
        }

        resourceDAO.deleteMappings(name, SchemaType.byClass(reference));

        entityManager.remove(schema);
    }
}
