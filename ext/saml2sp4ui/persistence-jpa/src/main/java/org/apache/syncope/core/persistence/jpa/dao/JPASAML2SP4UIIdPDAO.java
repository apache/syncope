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
import org.apache.syncope.core.persistence.jpa.entity.JPASAML2SP4UIIdP;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;

public class JPASAML2SP4UIIdPDAO extends AbstractDAO<SAML2SP4UIIdP> implements SAML2SP4UIIdPDAO {

    @Transactional(readOnly = true)
    @Override
    public SAML2SP4UIIdP find(final String key) {
        return entityManager().find(JPASAML2SP4UIIdP.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public SAML2SP4UIIdP findByEntityID(final String entityID) {
        TypedQuery<SAML2SP4UIIdP> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SP4UIIdP.class.getSimpleName()
                + " e WHERE e.entityID = :entityID", SAML2SP4UIIdP.class);
        query.setParameter("entityID", entityID);

        SAML2SP4UIIdP result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No IdP found with entityID {}", entityID, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SAML2SP4UIIdP> findAll() {
        TypedQuery<SAML2SP4UIIdP> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2SP4UIIdP.class.getSimpleName() + " e", SAML2SP4UIIdP.class);
        return query.getResultList();
    }

    @Override
    public SAML2SP4UIIdP save(final SAML2SP4UIIdP idp) {
        return entityManager().merge(idp);
    }

    @Override
    public void delete(final String key) {
        SAML2SP4UIIdP idp = find(key);
        if (idp != null) {
            entityManager().remove(idp);
        }
    }
}
