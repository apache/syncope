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
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAOIDCJWKS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class JPAOIDCJWKSDAO implements OIDCJWKSDAO {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCJWKSDAO.class);

    protected final EntityManager entityManager;

    public JPAOIDCJWKSDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OIDCJWKS> get() {
        try {
            TypedQuery<OIDCJWKS> query = entityManager.
                    createQuery("SELECT e FROM " + JPAOIDCJWKS.class.getSimpleName() + " e", OIDCJWKS.class);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            LOG.debug("No OIDC JWKS found", e);
        }
        return Optional.empty();
    }

    @Override
    public OIDCJWKS save(final OIDCJWKS jwks) {
        return entityManager.merge(jwks);
    }

    @Override
    public void delete() {
        entityManager.
                createQuery("DELETE FROM " + JPAOIDCJWKS.class.getSimpleName()).
                executeUpdate();
    }
}
