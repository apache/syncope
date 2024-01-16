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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAuthProfile;
import org.springframework.data.domain.Pageable;

public class AuthProfileRepoExtImpl implements AuthProfileRepoExt {

    protected final EntityManager entityManager;

    public AuthProfileRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<AuthProfile> findAll(final Pageable pageable) {
        TypedQuery<AuthProfile> query = entityManager.createQuery(
                "SELECT e FROM " + JPAAuthProfile.class.getSimpleName() + " e ORDER BY e.owner ASC",
                AuthProfile.class);

        // page starts from 1, while setFirtResult() starts from 0
        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * (pageable.getPageNumber() - 1));
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }
}
