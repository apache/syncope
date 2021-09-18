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
import org.apache.syncope.core.persistence.jpa.entity.JPAOIDCC4UIProvider;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;

public class JPAOIDCC4UIProviderDAO extends AbstractDAO<OIDCC4UIProvider> implements OIDCC4UIProviderDAO {

    @Transactional(readOnly = true)
    @Override
    public OIDCC4UIProvider find(final String key) {
        return entityManager().find(JPAOIDCC4UIProvider.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public OIDCC4UIProvider findByName(final String name) {
        TypedQuery<OIDCC4UIProvider> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCC4UIProvider.class.getSimpleName()
                + " e WHERE e.name = :name", OIDCC4UIProvider.class);
        query.setParameter("name", name);

        OIDCC4UIProvider result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No OIDC Provider found with name {}", name, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<OIDCC4UIProvider> findAll() {
        TypedQuery<OIDCC4UIProvider> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCC4UIProvider.class.getSimpleName() + " e", OIDCC4UIProvider.class);
        return query.getResultList();
    }

    @Override
    public OIDCC4UIProvider save(final OIDCC4UIProvider op) {
        return entityManager().merge(op);
    }

    @Override
    public void delete(final String key) {
        OIDCC4UIProvider op = find(key);
        if (op != null) {
            entityManager().remove(op);
        }
    }
}
