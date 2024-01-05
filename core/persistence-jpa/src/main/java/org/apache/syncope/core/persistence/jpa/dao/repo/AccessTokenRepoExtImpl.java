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
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.jpa.entity.JPAAccessToken;
import org.springframework.util.ReflectionUtils;

public class AccessTokenRepoExtImpl implements AccessTokenRepoExt {

    protected static StringBuilder buildFindAllQuery() {
        return new StringBuilder("SELECT e FROM ").
                append(JPAAccessToken.class.getSimpleName()).
                append(" e WHERE 1=1");
    }

    protected static String toOrderByStatement(final List<OrderByClause> orderByClauses) {
        StringBuilder statement = new StringBuilder();

        orderByClauses.forEach(clause -> {
            String field = clause.getField().trim();
            if (ReflectionUtils.findField(JPAAccessToken.class, field) != null) {
                statement.append("e.").append(field).append(' ').append(clause.getDirection().name());
            }
        });

        if (statement.length() == 0) {
            statement.append(" ORDER BY e.expirationTime DESC");
        } else {
            statement.insert(0, "ORDER BY ");
        }
        return statement.toString();
    }

    protected final EntityManager entityManager;

    public AccessTokenRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<AccessToken> findAll(
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        StringBuilder queryString = buildFindAllQuery().append(toOrderByStatement(orderByClauses));

        TypedQuery<AccessToken> query = entityManager.createQuery(queryString.toString(), AccessToken.class);

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public long deleteExpired() {
        Query query = entityManager.createQuery(
                "DELETE FROM " + JPAAccessToken.class.getSimpleName() + " e "
                + "WHERE e.expirationTime < :now");
        query.setParameter("now", OffsetDateTime.now());
        return query.executeUpdate();
    }
}
