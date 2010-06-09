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
import org.syncope.core.persistence.beans.UserAttribute;
import org.syncope.core.persistence.dao.UserAttributeDAO;

@Repository
public class UserAttributeDAOImpl extends AbstractDAOImpl
        implements UserAttributeDAO {

    @Override
    public UserAttribute find(long id) {
        return entityManager.find(UserAttribute.class, id);
    }

    @Override
    public List<UserAttribute> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM UserAttribute e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public UserAttribute save(UserAttribute attribute) {
        UserAttribute result = entityManager.merge(attribute);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(long id) {
        entityManager.remove(find(id));
    }
}
