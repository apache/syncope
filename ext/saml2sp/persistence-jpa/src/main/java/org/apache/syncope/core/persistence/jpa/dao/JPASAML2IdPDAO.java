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
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.jpa.entity.JPASAML2IdP;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPASAML2IdPDAO extends AbstractDAO<SAML2IdP> implements SAML2IdPDAO {

    @Transactional(readOnly = true)
    @Override
    public SAML2IdP find(final String key) {
        return entityManager().find(JPASAML2IdP.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public SAML2IdP findByEntityID(final String entityID) {
        TypedQuery<SAML2IdP> query = entityManager().createQuery("SELECT e FROM " + JPASAML2IdP.class.getSimpleName()
                + " e WHERE e.entityID = :entityID", SAML2IdP.class);
        query.setParameter("entityID", entityID);

        SAML2IdP result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No IdP found with entityID {}", entityID, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SAML2IdP> findAll() {
        TypedQuery<SAML2IdP> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2IdP.class.getSimpleName() + " e", SAML2IdP.class);
        return query.getResultList();
    }

    @Override
    public SAML2IdP save(final SAML2IdP idp) {
        return entityManager().merge(idp);
    }

    @Override
    public void delete(final String key) {
        SAML2IdP idp = find(key);
        if (idp != null) {
            entityManager().remove(idp);
        }
    }

}
