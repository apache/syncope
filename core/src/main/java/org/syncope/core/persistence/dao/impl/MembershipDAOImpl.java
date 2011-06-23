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
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;

@Repository
public class MembershipDAOImpl extends AbstractDAOImpl
        implements MembershipDAO {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private RoleDAO roleDAO;

    @Override
    public Membership find(final Long id) {
        return entityManager.find(Membership.class, id);
    }

    @Override
    public Membership find(final SyncopeUser user, final SyncopeRole role) {
        Query query = entityManager.createQuery("SELECT e FROM Membership e "
                + "WHERE e.syncopeUser = :user AND e.syncopeRole = :role");
        query.setParameter("user", user);
        query.setParameter("role", role);

        Membership result = null;

        try {
            result = (Membership) query.getSingleResult();
        }
        catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No membership was found for user "
                        + user + " and role " + role);
            }
        }

        return result;
    }

    @Override
    public List<Membership> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM Membership e");
        return query.getResultList();
    }

    @Override
    public Membership save(final Membership membership) {
        return entityManager.merge(membership);
    }

    @Override
    public void delete(final Long id) {
        Membership membership = find(id);
        if (membership == null) {
            return;
        }

        membership.getSyncopeUser().removeMembership(membership);
        userDAO.save(membership.getSyncopeUser());
        membership.setSyncopeUser(null);

        roleDAO.save(membership.getSyncopeRole());
        membership.setSyncopeRole(null);

        entityManager.remove(membership);
    }
}
