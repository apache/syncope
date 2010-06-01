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
import org.syncope.core.beans.UserDerivedAttribute;
import org.syncope.core.dao.UserDerivedAttributeDAO;

public class UserDerivedAttributeDAOImpl extends AbstractDAOImpl
        implements UserDerivedAttributeDAO {

    @Override
    public UserDerivedAttribute find(long id) {
        return entityManager.find(UserDerivedAttribute.class, id);
    }

    @Override
    public List<UserDerivedAttribute> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM UserDerivedAttribute e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public UserDerivedAttribute save(UserDerivedAttribute attribute) {
        UserDerivedAttribute result = entityManager.merge(attribute);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(long id) {
        entityManager.remove(find(id));
    }
}
