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

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAOIDCOpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class JPAOIDCOpEntityDAO implements OIDCOpEntityDAO {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCOpEntityDAO.class);

    protected final EntityManager entityManager;

    public JPAOIDCOpEntityDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OIDCOpEntity> get() {
        try {
            TypedQuery<OIDCOpEntity> query = entityManager.createQuery(
                    "SELECT e FROM " + JPAOIDCOpEntity.class.getSimpleName() + " e", OIDCOpEntity.class);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            LOG.debug("No OIDC JWKS found", e);
        }
        return Optional.empty();
    }

    @Override
    public OIDCOpEntity save(final OIDCOpEntity oidcOpEntity) {
        ((JPAOIDCOpEntity) oidcOpEntity).map2json();
        OIDCOpEntity merged = entityManager.merge(oidcOpEntity);
        ((JPAOIDCOpEntity) merged).postSave();
        return merged;
    }

    @Override
    public void delete() {
        entityManager.createQuery("DELETE FROM " + JPAOIDCOpEntity.class.getSimpleName()).executeUpdate();
    }
}
