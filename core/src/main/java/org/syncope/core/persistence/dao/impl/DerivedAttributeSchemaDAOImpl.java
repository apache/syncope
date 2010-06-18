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

import java.util.List;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.DerivedAttribute;
import org.syncope.core.persistence.beans.DerivedAttributeSchema;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedAttributeSchemaDAO;

@Repository
public class DerivedAttributeSchemaDAOImpl extends AbstractDAOImpl
        implements DerivedAttributeSchemaDAO {

    @Autowired
    DerivedAttributeDAO derivedAttributeDAO;

    @Override
    public DerivedAttributeSchema find(String name) {
        return entityManager.find(DerivedAttributeSchema.class, name);
    }

    @Override
    public List<DerivedAttributeSchema> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM DerivedAttributeSchema e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public DerivedAttributeSchema save(
            DerivedAttributeSchema attributeSchema) {

        DerivedAttributeSchema result = entityManager.merge(attributeSchema);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(String name) {
        DerivedAttributeSchema schema = find(name);
        if (schema == null) {
            return;
        }

        for (DerivedAttribute attribute : schema.getDerivedAttributes()) {
            derivedAttributeDAO.delete(attribute.getId());
        }
        for (AttributeSchema attributeSchema :
                schema.getAttributeSchemas()) {

            attributeSchema.removeDerivedAttributeSchema(schema);
            entityManager.merge(attributeSchema);
        }

        entityManager.remove(schema);
    }
}
