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
import org.syncope.core.persistence.beans.DerivedAttribute;
import org.syncope.core.persistence.beans.SyncopeRole;
import org.syncope.core.persistence.beans.SyncopeUser;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;

@Repository
public class DerivedAttributeDAOImpl extends AbstractDAOImpl
        implements DerivedAttributeDAO {

    @Override
    public DerivedAttribute find(long id) {
        DerivedAttribute result = entityManager.find(
                DerivedAttribute.class, id);
        if (isDeletedOrNotManaged(result)) {
            result = null;
        }

        return result;
    }

    @Override
    public List<DerivedAttribute> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM DerivedAttribute e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public DerivedAttribute save(DerivedAttribute attribute) {
        DerivedAttribute result = entityManager.merge(attribute);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(long id) {
        DerivedAttribute derivedAttribute = find(id);
        if (derivedAttribute == null) {
            return;
        }

        boolean shouldRemoveDerivedAttribute = true;

        Query query = entityManager.createQuery(
                "SELECT u FROM SyncopeUser u "
                + "WHERE :derivedAttribute MEMBER OF u.derivedAttributes");
        query.setParameter("derivedAttribute", derivedAttribute);
        List<SyncopeUser> users = query.getResultList();
        shouldRemoveDerivedAttribute = !users.isEmpty();
        for (SyncopeUser user : users) {
            user.removeDerivedAttribute(derivedAttribute);
            entityManager.merge(user);
        }

        query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r "
                + "WHERE :derivedAttribute MEMBER OF r.derivedAttributes");
        query.setParameter("derivedAttribute", derivedAttribute);
        List<SyncopeRole> roles = query.getResultList();
        shouldRemoveDerivedAttribute = !roles.isEmpty();
        for (SyncopeRole role : roles) {
            role.removeDerivedAttribute(derivedAttribute);
            entityManager.merge(role);
        }

        if (shouldRemoveDerivedAttribute) {
            entityManager.remove(find(id));
        }
    }
}
