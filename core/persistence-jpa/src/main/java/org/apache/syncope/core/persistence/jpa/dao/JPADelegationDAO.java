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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPADelegation;

public class JPADelegationDAO extends AbstractDAO<Delegation> implements DelegationDAO {

    @Override
    public Delegation find(final String key) {
        return entityManager().find(JPADelegation.class, key);
    }

    @Override
    public Optional<String> findValidFor(final String delegating, final String delegated) {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE e.delegating.id=:delegating AND e.delegated.id=:delegated "
                + "AND e.start <= :now AND (e.end IS NULL OR e.end >= :now)", Delegation.class);
        query.setParameter("delegating", delegating);
        query.setParameter("delegated", delegated);
        query.setParameter("now", OffsetDateTime.now());
        query.setMaxResults(1);

        List<Delegation> raw = query.getResultList();
        return raw.isEmpty() ? Optional.empty() : Optional.of(raw.get(0).getKey());
    }

    @Override
    public List<String> findValidDelegating(final String delegated) {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE e.delegated.id=:delegated "
                + "AND e.start <= :now AND (e.end IS NULL OR e.end >= :now)", Delegation.class);
        query.setParameter("delegated", delegated);
        query.setParameter("now", OffsetDateTime.now());

        return query.getResultList().stream().
                map(delegation -> delegation.getDelegating().getUsername()).
                collect(Collectors.toList());
    }

    @Override
    public List<Delegation> findByDelegating(final User user) {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE e.delegating=:user", Delegation.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Delegation> findByDelegated(final User user) {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE e.delegated=:user", Delegation.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Override
    public List<Delegation> findByRole(final Role role) {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE :role MEMBER OF e.roles", Delegation.class);
        query.setParameter("role", role);
        return query.getResultList();
    }

    @Override
    public List<Delegation> findAll() {
        TypedQuery<Delegation> query = entityManager().createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e ", Delegation.class);
        return query.getResultList();
    }

    @Override
    public Delegation save(final Delegation delegation) {
        return entityManager().merge(delegation);
    }

    @Override
    public void delete(final String key) {
        Delegation delegation = find(key);
        if (delegation == null) {
            return;
        }

        entityManager().remove(delegation);
    }
}
