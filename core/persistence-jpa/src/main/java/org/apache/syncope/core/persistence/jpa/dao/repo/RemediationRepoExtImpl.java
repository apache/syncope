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
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.jpa.entity.JPARemediation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class RemediationRepoExtImpl implements RemediationRepoExt {

    protected static final Logger LOG = LoggerFactory.getLogger(RemediationRepoExt.class);

    protected final EntityManager entityManager;

    public RemediationRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected StringBuilder query(
            final StringBuilder select,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        StringBuilder query = select.
                append(JPARemediation.class.getSimpleName()).
                append(" e WHERE 1=1 ");
        if (before != null) {
            query.append("AND e.instant <= :before ");
        }
        if (after != null) {
            query.append("AND e.instant >= :after ");
        }
        return query;
    }

    @Override
    public long count(final OffsetDateTime before, final OffsetDateTime after) {
        StringBuilder queryString = query(new StringBuilder("SELECT COUNT(e) FROM "), before, after);

        Query query = entityManager.createQuery(queryString.toString());
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<Remediation> findAll(
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        StringBuilder queryString = query(new StringBuilder("SELECT e FROM "), before, after);

        if (!pageable.getSort().isEmpty()) {
            queryString.append(" ORDER BY ");
            pageable.getSort().forEach(clause -> {
                String field = clause.getProperty().trim();
                boolean ack = true;
                if ("resource".equals(field)) {
                    queryString.append("e.pullTask.resource.id");
                } else {
                    Field beanField = ReflectionUtils.findField(JPARemediation.class, field);
                    if (beanField == null) {
                        ack = false;
                        LOG.warn("Remediation sort request by {}: unsupported, ignoring", field);
                    } else {
                        queryString.append("e.").append(field);
                    }
                }
                if (ack) {
                    if (clause.getDirection() == Sort.Direction.ASC) {
                        queryString.append(" ASC");
                    } else {
                        queryString.append(" DESC");
                    }
                    queryString.append(',');
                }
            });

            queryString.deleteCharAt(queryString.length() - 1);
        }

        TypedQuery<Remediation> query = entityManager.createQuery(queryString.toString(), Remediation.class);
        if (before != null) {
            query.setParameter("before", before);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    @Transactional
    @Override
    public void deleteById(final String key) {
        Optional.ofNullable(entityManager.find(JPARemediation.class, key)).ifPresent(entityManager::remove);
    }
}
