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
import org.syncope.core.persistence.beans.SyncopeRole;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;

@Repository
public class SyncopeRoleDAOImpl extends AbstractDAOImpl
        implements SyncopeRoleDAO {

    @Override
    public SyncopeRole find(String name, String parent) {
        Query query = null;

        if (parent != null) {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "name=:name AND parent=:parent");
            query.setParameter("parent", parent);
        } else {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "name=:name AND parent IS NULL");
        }
        query.setParameter("name", name);

        List<SyncopeRole> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public SyncopeRole find(Long id) {
        return entityManager.find(SyncopeRole.class, id);
    }

    @Override
    public List<SyncopeRole> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeRole e");
        return query.getResultList();
    }

    @Override
    public SyncopeRole save(SyncopeRole syncopeRole) {
        SyncopeRole result = entityManager.merge(syncopeRole);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(String name, String parent) {
        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "parent=:role");
        query.setParameter("role", name);
        List<SyncopeRole> childrenRoles = query.getResultList();

        if (!childrenRoles.isEmpty()) {
            for (SyncopeRole child : childrenRoles) {
                delete(child.getName(), child.getParent());
            }
        }

        entityManager.remove(find(name, parent));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SyncopeRole role = find(id);
        delete(role.getName(), role.getParent());
    }
}
