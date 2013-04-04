/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.MembershipDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipDAOImpl extends AbstractDAOImpl implements MembershipDAO {

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
        } catch (NoResultException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No membership was found for user " + user + " and role " + role);
            }
        }

        return result;
    }

    @Override
    public List<Membership> findAll() {
        TypedQuery<Membership> query = entityManager.createQuery("SELECT e FROM Membership e", Membership.class);
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

        entityManager.remove(membership);
    }
}
