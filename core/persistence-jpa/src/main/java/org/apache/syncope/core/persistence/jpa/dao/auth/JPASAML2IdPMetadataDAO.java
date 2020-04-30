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

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPMetadataDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPASAML2IdPMetadata;

@Repository
public class JPASAML2IdPMetadataDAO extends AbstractDAO<SAML2IdPMetadata> implements SAML2IdPMetadataDAO {

    @Transactional(readOnly = true)
    @Override
    public SAML2IdPMetadata find(final String key) {
        return entityManager().find(JPASAML2IdPMetadata.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public SAML2IdPMetadata findByOwner(final String appliesTo) {
        TypedQuery<SAML2IdPMetadata> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2IdPMetadata.class.getSimpleName() + " e WHERE e.appliesTo=:appliesTo",
                SAML2IdPMetadata.class);
        query.setParameter("appliesTo", appliesTo);

        SAML2IdPMetadata result = null;
        try {
            result = query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug("No SAML2 IdP Metadata found with appliesTo = {}", appliesTo);
        }

        return result;
    }

    @Override
    public SAML2IdPMetadata save(final SAML2IdPMetadata saml2IdPMetadata) {
        return entityManager().merge(saml2IdPMetadata);
    }

}
