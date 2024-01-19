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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.spring.implementation.ImplementationManager;

public class ImplementationRepoExtImpl implements ImplementationRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityCacheDAO entityCacheDAO;

    protected final EntityManager entityManager;

    public ImplementationRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final EntityCacheDAO entityCacheDAO,
            final EntityManager entityManager) {

        this.resourceDAO = resourceDAO;
        this.entityCacheDAO = entityCacheDAO;
        this.entityManager = entityManager;
    }

    @Override
    public List<Implementation> findByTypeAndKeyword(final String type, final String keyword) {
        StringBuilder queryString = new StringBuilder(
                "SELECT e FROM ").append(JPAImplementation.class.getSimpleName()).append(" e ").
                append("WHERE e.type=:type ");
        if (StringUtils.isNotBlank(keyword)) {
            queryString.append("AND e.id LIKE :keyword ");
        }
        queryString.append("ORDER BY e.id ASC");

        TypedQuery<Implementation> query = entityManager.createQuery(queryString.toString(), Implementation.class);
        query.setParameter("type", type);
        if (StringUtils.isNotBlank(keyword)) {
            query.setParameter("keyword", keyword);
        }

        return query.getResultList();
    }

    @Override
    public Implementation save(final Implementation implementation) {
        Implementation merged = entityManager.merge(implementation);

        ImplementationManager.purge(merged.getKey());

        resourceDAO.findByProvisionSorter(merged).
                forEach(resource -> entityCacheDAO.evict(JPAExternalResource.class, resource.getKey()));

        return merged;
    }

    @Override
    public void deleteById(final String key) {
        Implementation implementation = entityManager.find(JPAImplementation.class, key);
        if (implementation == null) {
            return;
        }

        entityManager.remove(implementation);
        ImplementationManager.purge(key);
    }
}
