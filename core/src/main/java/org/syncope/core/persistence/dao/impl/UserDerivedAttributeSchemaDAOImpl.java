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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.UserDerivedAttributeSchema;
import org.syncope.core.persistence.dao.UserDerivedAttributeSchemaDAO;

@Repository
public class UserDerivedAttributeSchemaDAOImpl extends AbstractDAOImpl
        implements UserDerivedAttributeSchemaDAO {

    @Override
    public UserDerivedAttributeSchema find(String name) {
        return entityManager.find(UserDerivedAttributeSchema.class, name);
    }

    @Override
    public List<UserDerivedAttributeSchema> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM UserDerivedAttributeSchema e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public UserDerivedAttributeSchema save(
            UserDerivedAttributeSchema attributeSchema) {

        UserDerivedAttributeSchema result = entityManager.merge(attributeSchema);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(String name) {
        entityManager.remove(find(name));
    }
}
