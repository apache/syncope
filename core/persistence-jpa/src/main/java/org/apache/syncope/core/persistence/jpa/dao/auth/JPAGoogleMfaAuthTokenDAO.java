/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.persistence.jpa.dao.auth;

import org.apache.syncope.core.persistence.api.dao.auth.GoogleMfaAuthTokenDAO;
import org.apache.syncope.core.persistence.api.entity.auth.GoogleMfaAuthToken;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAGoogleMfaAuthToken;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import java.util.Date;
import java.util.List;

@Repository
public class JPAGoogleMfaAuthTokenDAO extends AbstractDAO<GoogleMfaAuthToken> implements GoogleMfaAuthTokenDAO {
    @Override
    @Transactional(readOnly = true)
    public GoogleMfaAuthToken find(final String key) {
        return entityManager().find(JPAGoogleMfaAuthToken.class, key);
    }

    @Override
    @Transactional(readOnly = true)
    public GoogleMfaAuthToken find(final String owner, final Integer otp) {
        TypedQuery<GoogleMfaAuthToken> query = entityManager().createQuery(
            "SELECT e FROM " + JPAGoogleMfaAuthToken.class.getSimpleName()
                + " e WHERE e.owner=:owner AND e.token=:token",
            GoogleMfaAuthToken.class);
        query.setParameter("owner", owner);
        query.setParameter("token", otp);
        GoogleMfaAuthToken result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No Google Mfa Token found for owner = {} and otp = {}", owner, otp);
        }
        return result;
    }

    @Override
    public GoogleMfaAuthToken save(final GoogleMfaAuthToken token) {
        return entityManager().merge(token);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(final String owner) {
        TypedQuery<Long> query = entityManager().createQuery(
            "SELECT COUNT(e.user) FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e WHERE e.owner=:owner",
            Long.class);
        query.setParameter("owner", owner);
        return query.getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        TypedQuery<Long> query = entityManager().createQuery(
            "SELECT COUNT(e.owner) FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e",
            Long.class);
        return query.getSingleResult();
    }

    @Override
    public void deleteAll() {
        entityManager()
            .createQuery("DELETE FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e")
            .executeUpdate();
    }

    @Override
    public void delete(final Integer otp) {
        entityManager()
            .createQuery("DELETE FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e WHERE e.token=:token")
            .setParameter("token", otp)
            .executeUpdate();
    }

    @Override
    public void delete(final String owner) {
        entityManager()
            .createQuery("DELETE FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e WHERE e.owner=:owner")
            .setParameter("owner", owner)
            .executeUpdate();
    }

    @Override
    public void delete(final String owner, final Integer otp) {
        entityManager()
            .createQuery("DELETE FROM " + JPAGoogleMfaAuthToken.class.getSimpleName()
                + " e WHERE e.owner=:user AND e.token=:token")
            .setParameter("owner", owner)
            .setParameter("token", otp)
            .executeUpdate();
    }

    @Override
    public void delete(final Date expirationDate) {
        entityManager()
            .createQuery("DELETE FROM " + JPAGoogleMfaAuthToken.class.getSimpleName()
                + " e WHERE e.issuedDateTime>=:expired")
            .setParameter("expired", expirationDate)
            .executeUpdate();
    }

    @Override
    public List<GoogleMfaAuthToken> findForOwner(final String owner) {
        TypedQuery<GoogleMfaAuthToken> query = entityManager().createQuery(
            "SELECT e FROM " + JPAGoogleMfaAuthToken.class.getSimpleName() + " e WHERE e.owner=:owner",
            GoogleMfaAuthToken.class);
        query.setParameter("owner", owner);
        try {
            return query.getResultList();
        } catch (final NoResultException e) {
            LOG.debug("No Google Mfa Token found for user = {}", owner);
        }
        return List.of();
    }
}
