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
import org.hibernate.CacheMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.util.EntitlementUtil;

@Repository
public class EntitlementDAOImpl extends AbstractDAOImpl
        implements EntitlementDAO {

    @Autowired
    private RoleDAO roleDAO;

    @Override
    public Entitlement find(final String name) {
        return entityManager.find(Entitlement.class, name);
    }

    @Override
    public List<Entitlement> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Entitlement e");
        query.setHint("org.hibernate.cacheable", true);
        query.setHint("org.hibernate.cacheMode", CacheMode.REFRESH);

        return query.getResultList();
    }

    @Override
    public Entitlement save(final Entitlement entitlement) {
        return entityManager.merge(entitlement);
    }

    @Override
    public Entitlement save(final SyncopeRole role) {
        Entitlement roleEnt = new Entitlement();
        roleEnt.setName(EntitlementUtil.getEntitlementName(role.getId()));
        roleEnt.setDescription("Entitlement for managing role " + role.getId());

        return save(roleEnt);
    }

    @Override
    public void delete(final String name) {
        Entitlement entitlement = find(name);
        if (entitlement == null) {
            return;
        }

        delete(entitlement);
    }

    @Override
    public void delete(final Entitlement entitlement) {
        for (SyncopeRole role : entitlement.getRoles()) {
            role.removeEntitlement(entitlement);
            roleDAO.save(role);
        }

        entityManager.remove(entitlement);
    }
}
