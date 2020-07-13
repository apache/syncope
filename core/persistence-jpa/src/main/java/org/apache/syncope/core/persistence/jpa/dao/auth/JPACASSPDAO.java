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

import org.apache.syncope.core.persistence.api.dao.auth.CASSPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.CASSP;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPACASSP;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAOIDCRP;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import java.util.List;

@Repository
public class JPACASSPDAO extends AbstractDAO<CASSP> implements CASSPDAO {

    @Override
    public CASSP find(final String key) {
        return entityManager().find(JPACASSP.class, key);
    }

    private CASSP find(final String column, final Object value) {
        TypedQuery<CASSP> query = entityManager().createQuery(
                "SELECT e FROM " + JPACASSP.class.getSimpleName() + " e WHERE e." + column + "=:value",
            CASSP.class);
        query.setParameter("value", value);

        CASSP result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No OIDCRP found with " + column + " {}", value, e);
        }

        return result;
    }

    @Override
    public CASSP findByClientAppId(final Long clientAppId) {
        return find("clientAppId", clientAppId);
    }

    @Override
    public CASSP findByName(final String name) {
        return find("name", name);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CASSP> findAll() {
        TypedQuery<CASSP> query = entityManager().createQuery(
                "SELECT e FROM " + JPACASSP.class.getSimpleName() + " e", CASSP.class);

        return query.getResultList();
    }

    @Override
    public CASSP save(final CASSP clientApp) {
        return entityManager().merge(clientApp);
    }

    @Override
    public void delete(final String key) {
        CASSP rpTO = find(key);
        if (rpTO == null) {
            return;
        }

        delete(rpTO);
    }

    @Override
    public void delete(final CASSP clientApp) {
        entityManager().remove(clientApp);
    }
}
