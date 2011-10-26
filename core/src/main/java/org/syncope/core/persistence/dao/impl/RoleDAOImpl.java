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

import java.util.Collections;
import java.util.List;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.util.EntitlementUtil;

@Repository
public class RoleDAOImpl extends AbstractDAOImpl implements RoleDAO {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Override
    public SyncopeRole find(final Long id) {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeRole e WHERE e.id = :id");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
        query.setParameter("id", id);

        try {
            return (SyncopeRole) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<SyncopeRole> find(final String name) {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeRole e WHERE e.name = :name");
        query.setHint("javax.persistence.cache.retrieveMode",
                CacheRetrieveMode.USE);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public SyncopeRole find(final String name, final Long parentId) {
        Query query;
        if (parentId != null) {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "name=:name AND parent.id=:parentId");
            query.setParameter("parentId", parentId);
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
    public List<SyncopeRole> findChildren(final Long roleId) {
        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "parent.id=:roleId");
        query.setParameter("roleId", roleId);
        return query.getResultList();
    }

    @Override
    public List<SyncopeRole> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeRole e");
        return query.getResultList();
    }

    @Override
    public List<Membership> findMemberships(final SyncopeRole role) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + Membership.class.getSimpleName() + " e"
                + " WHERE e.syncopeRole=:role");
        query.setParameter("role", role);

        return query.getResultList();
    }

    @Override
    public SyncopeRole save(final SyncopeRole role) {
        // reset account policy in case of inheritance
        if (role.isInheritAccountPolicy()) {
            role.setAccountPolicy(null);
        }

        // reset password policy in case of inheritance
        if (role.isInheritPasswordPolicy()) {
            role.setPasswordPolicy(null);
        }

        final SyncopeRole savedRole = entityManager.merge(role);
        entitlementDAO.save(savedRole);

        return savedRole;
    }

    @Override
    public void delete(final Long id) {
        SyncopeRole role = find(id);
        if (role == null) {
            return;
        }

        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "parent_id=:id");
        query.setParameter("id", id);
        List<SyncopeRole> childrenRoles = query.getResultList();
        for (SyncopeRole child : childrenRoles) {
            delete(child.getId());
        }

        for (Membership membership : findMemberships(role)) {
            membership.setSyncopeRole(null);
            membership.getSyncopeUser().removeMembership(membership);
            membership.setSyncopeUser(null);

            entityManager.remove(membership);
        }

        for (Entitlement entitlement : role.getEntitlements()) {
            entitlement.removeRole(role);
        }
        role.setEntitlements(Collections.EMPTY_SET);

        role.setParent(null);
        entityManager.remove(role);

        entitlementDAO.delete(EntitlementUtil.getEntitlementName(id));
    }
}
