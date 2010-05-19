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
package org.syncope.core.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.beans.UserAttributeSchema;
import org.syncope.core.dao.UserAttributeSchemaDAO;

public class UserAttributeSchemaDAOImpl extends AbstractDAOImpl
        implements UserAttributeSchemaDAO {

    @Override
    public UserAttributeSchema find(String name) {
        return entityManager.find(UserAttributeSchema.class, name);
    }

    @Override
    public List<UserAttributeSchema> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM UserAttributeSchema e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public UserAttributeSchema save(UserAttributeSchema userAttributeSchema) {
        UserAttributeSchema result = entityManager.merge(userAttributeSchema);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(String name) {
        entityManager.remove(find(name));
    }
}
