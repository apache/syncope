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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.jpa.entity.JPADelegation;

public class DelegationRepoExtImpl implements DelegationRepoExt {

    protected final EntityManager entityManager;

    public DelegationRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<String> findValidFor(final String delegating, final String delegated) {
        TypedQuery<Delegation> query = entityManager.createQuery(
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
        TypedQuery<Delegation> query = entityManager.createQuery(
                "SELECT e FROM " + JPADelegation.class.getSimpleName() + " e "
                + "WHERE e.delegated.id=:delegated "
                + "AND e.start <= :now AND (e.end IS NULL OR e.end >= :now)", Delegation.class);
        query.setParameter("delegated", delegated);
        query.setParameter("now", OffsetDateTime.now());

        return query.getResultList().stream().
                map(delegation -> delegation.getDelegating().getUsername()).
                collect(Collectors.toList());
    }
}
