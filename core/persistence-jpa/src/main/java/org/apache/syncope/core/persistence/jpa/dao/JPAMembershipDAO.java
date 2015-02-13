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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.MembershipDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JPAMembershipDAO extends AbstractDAO<Membership, Long> implements MembershipDAO {

    @Autowired
    private UserDAO userDAO;

    @Override
    public Membership find(final Long key) {
        return entityManager.find(JPAMembership.class, key);
    }

    @Override
    public Membership find(final User user, final Role role) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + JPAMembership.class.getSimpleName() + " e WHERE e.user = :user AND e.role = :role");
        query.setParameter("user", user);
        query.setParameter("role", role);

        Membership result = null;

        try {
            result = (Membership) query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No membership was found for user {} and role {}", user, role, e);
        }

        return result;
    }

    @Override
    public List<Membership> findAll() {
        TypedQuery<Membership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAMembership.class.getSimpleName() + " e", Membership.class);
        return query.getResultList();
    }

    @Override
    public Membership save(final Membership membership) {
        return entityManager.merge(membership);
    }

    @Override
    public void delete(final Long key) {
        Membership membership = find(key);
        if (membership == null) {
            return;
        }

        membership.getUser().removeMembership(membership);
        userDAO.save(membership.getUser());

        entityManager.remove(membership);
    }

    @Override
    public Membership authFetch(final Long key) {
        if (key == null) {
            throw new NotFoundException("Null membership key");
        }

        Membership membership = find(key);
        if (membership == null) {
            throw new NotFoundException("Membership " + key);
        }

        return membership;
    }

}
