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
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPAFIQLQuery;

public class JPAFIQLQueryDAO extends AbstractDAO<FIQLQuery> implements FIQLQueryDAO {

    @Override
    public FIQLQuery find(final String key) {
        return entityManager().find(JPAFIQLQuery.class, key);
    }

    @Override
    public List<FIQLQuery> findByOwner(final User user, final String target) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAFIQLQuery.class.getSimpleName()).append(" e WHERE e.owner=:user");
        if (StringUtils.isNotBlank(target)) {
            queryString.append(" AND e.target=:target");
        }

        TypedQuery<FIQLQuery> query = entityManager().createQuery(queryString.toString(), FIQLQuery.class);
        query.setParameter("user", user);
        if (StringUtils.isNotBlank(target)) {
            query.setParameter("target", target);
        }

        return query.getResultList();
    }

    @Override
    public List<FIQLQuery> findAll() {
        TypedQuery<FIQLQuery> query = entityManager().createQuery(
                "SELECT e FROM " + JPAFIQLQuery.class.getSimpleName() + " e ", FIQLQuery.class);
        return query.getResultList();
    }

    @Override
    public FIQLQuery save(final FIQLQuery fiqlQuery) {
        return entityManager().merge(fiqlQuery);
    }

    @Override
    public void delete(final FIQLQuery fiqlQuery) {
        entityManager().remove(fiqlQuery);
    }

    @Override
    public void delete(final String key) {
        FIQLQuery fiqlQuery = find(key);
        if (fiqlQuery == null) {
            return;
        }

        delete(fiqlQuery);
    }
}
