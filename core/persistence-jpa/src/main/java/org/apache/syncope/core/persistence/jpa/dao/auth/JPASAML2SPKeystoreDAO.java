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

import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPKeystoreDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPKeystore;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPASAML2SPKeystore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

@Repository
public class JPASAML2SPKeystoreDAO extends AbstractDAO<SAML2SPKeystore> implements SAML2SPKeystoreDAO {

    @Transactional(readOnly = true)
    @Override
    public SAML2SPKeystore find(final String key) {
        return entityManager().find(JPASAML2SPKeystore.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public SAML2SPKeystore findByOwner(final String owner) {
        TypedQuery<SAML2SPKeystore> query = entityManager().createQuery(
            "SELECT e FROM " + JPASAML2SPKeystore.class.getSimpleName() + " e WHERE e.owner=:owner",
            SAML2SPKeystore.class);
        query.setParameter("owner", owner);

        SAML2SPKeystore result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No SAML2 SP Keystore found with appliesTo = {}", owner);
        }
        return result;
    }

    @Override
    public SAML2SPKeystore save(final SAML2SPKeystore saml2IdPMetadata) {
        return entityManager().merge(saml2IdPMetadata);
    }

}
