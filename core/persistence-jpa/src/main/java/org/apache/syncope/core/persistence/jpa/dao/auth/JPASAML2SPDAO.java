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

import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPASAML2SP;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;

@Repository
public class JPASAML2SPDAO extends AbstractDAO<SAML2SP> implements SAML2SPDAO {

    @Override
    public SAML2SP find(final String key) {
        return entityManager().find(JPASAML2SP.class, key);
    }

    private SAML2SP find(final String column, final Object value) {
        TypedQuery<SAML2SP> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SP.class.getSimpleName() + " e WHERE e." + column + "=:value",
                SAML2SP.class);
        query.setParameter("value", value);

        SAML2SP result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No SAML2SP found with " + column + " {}", value, e);
        }

        return result;
    }

    @Override
    public SAML2SP findByClientAppId(final Long clientAppId) {
        return find("clientAppId", clientAppId);
    }

    @Override
    public SAML2SP findByName(final String name) {
        return find("name", name);
    }

    @Override
    public SAML2SP findByEntityId(final String entityId) {
        return find("entityId", entityId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SAML2SP> findAll() {
        TypedQuery<SAML2SP> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SP.class.getSimpleName() + " e", SAML2SP.class);

        return query.getResultList();
    }

    @Override
    public SAML2SP save(final SAML2SP clientApp) {
        return entityManager().merge(clientApp);
    }

    @Override
    public void delete(final String key) {
        SAML2SP policy = find(key);
        if (policy == null) {
            return;
        }

        delete(policy);
    }

    @Override
    public void deleteByEntityId(final String entityId) {
        SAML2SP app = findByEntityId(entityId);
        if (app == null) {
            return;
        }
        delete(app);
    }

    @Override
    public void delete(final SAML2SP clientApp) {
        entityManager().remove(clientApp);
    }
}
