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
package org.apache.syncope.core.persistence.jpa.dao.auth;

import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAAuthProfile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

@Repository
public class JPAAuthProfileDAO extends AbstractDAO<AuthProfile> implements AuthProfileDAO {

    @Override
    @Transactional(readOnly = true)
    public List<AuthProfile> findAll() {
        TypedQuery<AuthProfile> query = entityManager().createQuery(
            "SELECT e FROM " + JPAAuthProfile.class.getSimpleName() + " e ",
            AuthProfile.class);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthProfile> findByOwner(final String owner) {
        try {
            TypedQuery<AuthProfile> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthProfile.class.getSimpleName()
                    + " e WHERE e.owner=:owner", AuthProfile.class);
            query.setParameter("owner", owner);
            return Optional.ofNullable(query.getSingleResult());
        } catch (final NoResultException e) {
            LOG.debug("No auth profile could be found for owner {}", owner);
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthProfile> findByKey(final String key) {
        try {
            return Optional.ofNullable(entityManager().find(JPAAuthProfile.class, key));
        } catch (final NoResultException e) {
            LOG.debug("No auth profile could be found for {}", key);
        }
        return Optional.empty();
    }

    @Override
    public AuthProfile save(final AuthProfile profile) {
        return entityManager().merge(profile);
    }

    @Override
    public void deleteByKey(final String key) {
        findByKey(key).ifPresent(this::delete);
    }

    @Override
    public void deleteByOwner(final String owner) {
        findByOwner(owner).ifPresent(this::delete);
    }

    @Override
    public void delete(final AuthProfile authProfile) {
        entityManager().remove(authProfile);
    }
}
