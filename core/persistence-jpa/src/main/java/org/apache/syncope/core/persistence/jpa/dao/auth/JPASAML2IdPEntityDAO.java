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
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPASAML2IdPEntity;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPEntity;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPEntityDAO;

public class JPASAML2IdPEntityDAO extends AbstractDAO<SAML2IdPEntity> implements SAML2IdPEntityDAO {

    @Transactional(readOnly = true)
    @Override
    public SAML2IdPEntity find(final String key) {
        return entityManager().find(JPASAML2IdPEntity.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SAML2IdPEntity> findAll() {
        TypedQuery<SAML2IdPEntity> query = entityManager().createQuery(
                "SELECT e FROM " + JPASAML2IdPEntity.class.getSimpleName() + " e",
                SAML2IdPEntity.class);
        return query.getResultList();
    }

    @Override
    public SAML2IdPEntity save(final SAML2IdPEntity saml2IdPEntity) {
        return entityManager().merge(saml2IdPEntity);
    }
}
