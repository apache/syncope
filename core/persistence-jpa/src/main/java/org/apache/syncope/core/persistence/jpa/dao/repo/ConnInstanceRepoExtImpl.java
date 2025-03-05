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
import java.util.Set;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.transaction.annotation.Transactional;

public class ConnInstanceRepoExtImpl implements ConnInstanceRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityManager entityManager;

    public ConnInstanceRepoExtImpl(final ExternalResourceDAO resourceDAO, final EntityManager entityManager) {
        this.resourceDAO = resourceDAO;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public ConnInstance authFind(final String key) {
        ConnInstance connInstance = entityManager.find(JPAConnInstance.class, key);
        if (connInstance == null) {
            return null;
        }

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_READ);
        if (authRealms == null || authRealms.isEmpty()
                || authRealms.stream().noneMatch(
                        realm -> connInstance.getAdminRealm().getFullPath().startsWith(realm))) {

            throw new DelegatedAdministrationException(
                    connInstance.getAdminRealm().getFullPath(),
                    ConnInstance.class.getSimpleName(),
                    connInstance.getKey());
        }

        return connInstance;
    }

    @Override
    public List<? extends ConnInstance> findAll() {
        final Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_LIST);
        if (authRealms == null || authRealms.isEmpty()) {
            return List.of();
        }

        TypedQuery<ConnInstance> query = entityManager.createQuery(
                "SELECT e FROM " + JPAConnInstance.class.getSimpleName() + " e", ConnInstance.class);

        return query.getResultList().stream().filter(connInstance -> authRealms.stream().
                anyMatch(realm -> connInstance.getAdminRealm().getFullPath().startsWith(realm))).
                toList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        ((JPAConnInstance) connector).list2json();
        return entityManager.merge(connector);
    }

    @Override
    public void deleteById(final String key) {
        ConnInstance connInstance = entityManager.find(JPAConnInstance.class, key);
        if (connInstance == null) {
            return;
        }

        connInstance.getResources().stream().map(ExternalResource::getKey).toList().forEach(resourceDAO::deleteById);

        entityManager.remove(connInstance);
    }
}
