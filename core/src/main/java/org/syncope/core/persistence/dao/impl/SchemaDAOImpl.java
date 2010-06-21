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
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.SchemaDAO;

@Repository
public class SchemaDAOImpl extends AbstractDAOImpl
        implements SchemaDAO {

    @Autowired
    AttributeDAO attributeDAO;

    @Override
    public <T extends AbstractSchema> T find(String name, Class<T> reference) {
        T result = entityManager.find(reference, name);
        if (isDeletedOrNotManaged(result)) {
            result = null;
        }

        return (T) result;
    }

    @Override
    public <T extends AbstractSchema> List<T> findAll(Class<T> reference) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public <T extends AbstractSchema> T save(T schema) {
        T result = entityManager.merge(schema);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <T extends AbstractSchema> void delete(
            String name, Class<T> reference) {

        T schema = find(name, reference);
        if (schema == null) {
            return;
        }

        entityManager.remove(schema);
    }
}
