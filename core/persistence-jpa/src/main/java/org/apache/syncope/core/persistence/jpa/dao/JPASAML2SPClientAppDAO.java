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
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.jpa.entity.am.JPASAML2SPClientApp;
import org.springframework.transaction.annotation.Transactional;

public class JPASAML2SPClientAppDAO extends AbstractClientAppDAO<SAML2SPClientApp> implements SAML2SPClientAppDAO {

    @Override
    public SAML2SPClientApp find(final String key) {
        return entityManager().find(JPASAML2SPClientApp.class, key);
    }

    private SAML2SPClientApp find(final String column, final Object value) {
        TypedQuery<SAML2SPClientApp> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SPClientApp.class.getSimpleName() + " e WHERE e." + column + "=:value",
                SAML2SPClientApp.class);
        query.setParameter("value", value);

        SAML2SPClientApp result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No SAML2SP found with " + column + " {}", value, e);
        }

        return result;
    }

    @Override
    public SAML2SPClientApp findByClientAppId(final Long clientAppId) {
        return find("clientAppId", clientAppId);
    }

    @Override
    public SAML2SPClientApp findByName(final String name) {
        return find("name", name);
    }

    @Override
    public SAML2SPClientApp findByEntityId(final String entityId) {
        return find("entityId", entityId);
    }

    @Override
    public List<SAML2SPClientApp> findByPolicy(final Policy policy) {
        return findByPolicy(policy, SAML2SPClientApp.class, JPASAML2SPClientApp.class);
    }

    @Override
    public List<SAML2SPClientApp> findByRealm(final Realm realm) {
        return findByRealm(realm, SAML2SPClientApp.class, JPASAML2SPClientApp.class);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SAML2SPClientApp> findAll() {
        TypedQuery<SAML2SPClientApp> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SPClientApp.class.getSimpleName() + " e", SAML2SPClientApp.class);

        return query.getResultList();
    }

    @Override
    public SAML2SPClientApp save(final SAML2SPClientApp clientApp) {
        ((JPASAML2SPClientApp) clientApp).list2json();
        return entityManager().merge(clientApp);
    }

    @Override
    public void delete(final String key) {
        SAML2SPClientApp policy = find(key);
        if (policy == null) {
            return;
        }

        delete(policy);
    }

    @Override
    public void deleteByEntityId(final String entityId) {
        SAML2SPClientApp app = findByEntityId(entityId);
        if (app == null) {
            return;
        }
        delete(app);
    }

    @Override
    public void delete(final SAML2SPClientApp clientApp) {
        entityManager().remove(clientApp);
    }
}
