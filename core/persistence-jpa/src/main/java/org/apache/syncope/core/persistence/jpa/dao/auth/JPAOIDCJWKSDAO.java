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

import org.apache.syncope.core.persistence.api.dao.auth.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCJWKS;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAOIDCJWKS;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

public class JPAOIDCJWKSDAO extends AbstractDAO<OIDCJWKS> implements OIDCJWKSDAO {

    @Transactional(readOnly = true)
    @Override
    public OIDCJWKS get() {
        try {
            TypedQuery<OIDCJWKS> query = entityManager().
                    createQuery("SELECT e FROM " + JPAOIDCJWKS.class.getSimpleName() + " e", OIDCJWKS.class);
            return query.getSingleResult();
        } catch (final NoResultException e) {
            LOG.debug(e.getMessage());
        }
        return null;
    }

    @Override
    public OIDCJWKS save(final OIDCJWKS jwks) {
        return entityManager().merge(jwks);
    }

    @Override
    public void delete() {
        entityManager().
                createQuery("DELETE FROM " + JPAOIDCJWKS.class.getSimpleName()).
                executeUpdate();
    }
}
