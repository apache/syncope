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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jConnInstance;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class ConnInstanceRepoExtImpl implements ConnInstanceRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public ConnInstanceRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        this.resourceDAO = resourceDAO;
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public ConnInstance authFind(final String key) {
        ConnInstance connInstance = neo4jTemplate.findById(key, Neo4jConnInstance.class).orElse(null);
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

        return neo4jTemplate.findAll(Neo4jConnInstance.class).stream().filter(connInstance -> authRealms.stream().
                anyMatch(realm -> connInstance.getAdminRealm().getFullPath().startsWith(realm))).
                toList();
    }

    @Override
    public ConnInstance save(final ConnInstance connector) {
        ((Neo4jConnInstance) connector).list2json();
        ConnInstance saved = neo4jTemplate.save(nodeValidator.validate(connector));
        ((Neo4jConnInstance) saved).postSave();
        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jConnInstance.class).ifPresent(connInstance -> {
            connInstance.getResources().stream().
            map(ExternalResource::getKey).toList().forEach(resourceDAO::deleteById);

            neo4jTemplate.deleteById(key, Neo4jConnInstance.class);
        });
    }
}
