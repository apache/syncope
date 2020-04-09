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

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAOIDCRP;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;

@Repository
public class JPAOIDCRPDAO extends AbstractDAO<OIDCRP> implements OIDCRPDAO {

    @Override
    public OIDCRP find(final String key) {
        return entityManager().find(JPAOIDCRP.class, key);
    }

    private OIDCRP find(final String column, final Object value) {
        TypedQuery<OIDCRP> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCRP.class.getSimpleName() + " e WHERE e." + column + "=:value",
                OIDCRP.class);
        query.setParameter("value", value);

        OIDCRP result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No OIDCRP found with " + column + " {}", value, e);
        }

        return result;
    }

    @Override
    public OIDCRP findByClientAppId(final Long clientAppId) {
        return find("clientAppId", clientAppId);
    }

    @Override
    public OIDCRP findByName(final String name) {
        return find("name", name);
    }

    @Override
    public OIDCRP findByClientId(final String clientId) {
        return find("clientId", clientId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OIDCRP> findAll() {
        TypedQuery<OIDCRP> query = entityManager().createQuery(
                "SELECT e FROM " + JPAOIDCRP.class.getSimpleName() + " e", OIDCRP.class);

        return query.getResultList();
    }

    @Override
    public OIDCRP save(final OIDCRP clientApp) {
        return entityManager().merge(clientApp);
    }

    @Override
    public void delete(final String key) {
        OIDCRP rpTO = find(key);
        if (rpTO == null) {
            return;
        }

        delete(rpTO);
    }

    @Override
    public void deleteByClientId(final String clientId) {
        OIDCRP rpTO = findByClientId(clientId);
        if (rpTO == null) {
            return;
        }
        delete(rpTO);
    }

    @Override
    public void delete(final OIDCRP clientApp) {
        entityManager().remove(clientApp);
    }
}
