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

import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.jpa.entity.JPAAccessToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class JPAAccessTokenDAO extends AbstractDAO<AccessToken> implements AccessTokenDAO {

    @Transactional(readOnly = true)
    @Override
    public AccessToken find(final String key) {
        return entityManager().find(JPAAccessToken.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public AccessToken findByOwner(final String username) {
        TypedQuery<AccessToken> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAccessToken.class.getSimpleName() + " e "
                + "WHERE e.owner=:username", AccessToken.class);
        query.setParameter("username", username);

        AccessToken result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No token for user {} could be found", username, e);
        }

        return result;
    }

    private static StringBuilder buildFindAllQuery() {
        return new StringBuilder("SELECT e FROM ").
                append(JPAAccessToken.class.getSimpleName()).
                append(" e WHERE 1=1");
    }

    @Transactional(readOnly = true)
    @Override
    public int count() {
        StringBuilder queryString = buildFindAllQuery();

        Query query = entityManager().createQuery(StringUtils.replaceOnce(
                queryString.toString(), "SELECT e", "SELECT COUNT(e)"));
        return ((Number) query.getSingleResult()).intValue();
    }

    private static String toOrderByStatement(final List<OrderByClause> orderByClauses) {
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

    @Transactional(readOnly = true)
    @Override
    public List<AccessToken> findAll(final int page, final int itemsPerPage, final List<OrderByClause> orderByClauses) {
        StringBuilder queryString = buildFindAllQuery().append(toOrderByStatement(orderByClauses));

        TypedQuery<AccessToken> query = entityManager().createQuery(queryString.toString(), AccessToken.class);

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public AccessToken save(final AccessToken accessToken) {
        return entityManager().merge(accessToken);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void delete(final String key) {
        AccessToken accessToken = find(key);
        if (accessToken == null) {
            return;
        }

        delete(accessToken);
    }

    @Override
    public void delete(final AccessToken accessToken) {
        entityManager().remove(accessToken);
    }

    @Override
    public int deleteExpired() {
        Query query = entityManager().createQuery(
                "DELETE FROM " + JPAAccessToken.class.getSimpleName() + " e "
                + "WHERE e.expirationTime < :now");
        query.setParameter("now", OffsetDateTime.now());
        return query.executeUpdate();
    }
}
