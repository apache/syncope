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
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.OIDCProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.persistence.jpa.entity.JPAOIDCProvider;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAOIDCProviderDAO extends AbstractDAO<OIDCProvider> implements OIDCProviderDAO {

    @Transactional(readOnly = true)
    @Override
    public OIDCProvider find(final String key) {
        return entityManager().find(JPAOIDCProvider.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public OIDCProvider findByName(final String name) {
        TypedQuery<OIDCProvider> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCProvider.class.getSimpleName()
                + " e WHERE e.name = :name", OIDCProvider.class);
        query.setParameter("name", name);

        OIDCProvider result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No OIDC Provider found with name {}", name, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OIDCProvider> findAll() {
        TypedQuery<OIDCProvider> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCProvider.class.getSimpleName() + " e", OIDCProvider.class);
        return query.getResultList();
    }

    @Override
    public OIDCProvider save(final OIDCProvider op) {
        return entityManager().merge(op);
    }

    @Override
    public void delete(final String key) {
        OIDCProvider op = find(key);
        if (op != null) {
            entityManager().remove(op);
        }
    }

}
