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

import java.lang.reflect.Field;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.entity.JPARemediation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

public class JPARemediationDAO extends AbstractDAO<Remediation> implements RemediationDAO {

    @Transactional(readOnly = true)
    @Override
    public Remediation find(final String key) {
        return entityManager().find(JPARemediation.class, key);
    }

    @Override
    public List<Remediation> findByAnyType(final AnyType anyType) {
        TypedQuery<Remediation> query = entityManager().createQuery(
                "SELECT e FROM " + JPARemediation.class.getSimpleName() + " e WHERE e.anyType=:anyType",
                Remediation.class);
        query.setParameter("anyType", anyType);
        return query.getResultList();
    }

    @Override
    public List<Remediation> findByPullTask(final PullTask pullTask) {
        TypedQuery<Remediation> query = entityManager().createQuery(
                "SELECT e FROM " + JPARemediation.class.getSimpleName() + " e WHERE e.pullTask=:pullTask",
                Remediation.class);
        query.setParameter("pullTask", pullTask);
        return query.getResultList();
    }

    @Override
    public int count() {
        Query query = entityManager().createNativeQuery("SELECT COUNT(id) FROM " + JPARemediation.TABLE);
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<Remediation> findAll(
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderByClauses) {

        StringBuilder queryString = new StringBuilder(
                "SELECT e FROM " + JPARemediation.class.getSimpleName() + " e");

        if (!orderByClauses.isEmpty()) {
            queryString.append(" ORDER BY ");
            orderByClauses.forEach(clause -> {
                String field = clause.getField().trim();
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
                    if (clause.getDirection() == OrderByClause.Direction.ASC) {
                        queryString.append(" ASC");
                    } else {
                        queryString.append(" DESC");
                    }
                    queryString.append(',');
                }
            });

            queryString.deleteCharAt(queryString.length() - 1);
        }

        TypedQuery<Remediation> query = entityManager().createQuery(queryString.toString(), Remediation.class);

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public Remediation save(final Remediation remediation) {
        return entityManager().merge(remediation);
    }

    @Override
    public void delete(final Remediation remediation) {
        entityManager().remove(remediation);
    }

    @Transactional
    @Override
    public void delete(final String key) {
        Remediation remediation = find(key);
        if (remediation == null) {
            return;
        }

        delete(remediation);
    }
}
