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
import java.util.Optional;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAAuthProfile;

public class JPAAuthProfileDAO extends AbstractDAO<AuthProfile> implements AuthProfileDAO {

    @Override
    public AuthProfile find(final String key) {
        return entityManager().find(JPAAuthProfile.class, key);
    }

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAAuthProfile.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<AuthProfile> findAll(final int page, final int itemsPerPage) {
        TypedQuery<AuthProfile> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthProfile.class.getSimpleName() + " e ORDER BY e.owner ASC",
                AuthProfile.class);

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public Optional<AuthProfile> findByOwner(final String owner) {
        TypedQuery<AuthProfile> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthProfile.class.getSimpleName()
                + " e WHERE e.owner=:owner", AuthProfile.class);
        query.setParameter("owner", owner);

        List<AuthProfile> result = query.getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public AuthProfile save(final AuthProfile profile) {
        return entityManager().merge(profile);
    }

    @Override
    public void delete(final String key) {
        AuthProfile authProfile = find(key);
        if (authProfile == null) {
            return;
        }
        delete(authProfile);
    }

    @Override
    public void delete(final AuthProfile authProfile) {
        entityManager().remove(authProfile);
    }
}
