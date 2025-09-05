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

import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4JPasswordManagement;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PasswordManagementRepoExtImpl implements PasswordManagementRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    protected final NodeValidator nodeValidator;

    public PasswordManagementRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean isAnotherInstanceEnabled(final String key) {
        Long count = neo4jClient.query(
                        "MATCH (pm:" + Neo4JPasswordManagement.NODE + ") "
                                + "WHERE pm.enabled = 'true' AND pm.id <> $id "
                                + "RETURN count(pm) AS cnt")
                .bindAll(Map.of("id", key))
                .fetch()
                .one()
                .map(record -> ((Number) record.get("cnt")).longValue())
                .orElse(0L);

        return count > 0;
    }

    @Override
    public PasswordManagement save(final PasswordManagement passwordManagement) {
        PasswordManagement saved = neo4jTemplate.save(nodeValidator.validate(passwordManagement));
        return saved;
    }

    @Override
    public void delete(final PasswordManagement passwordManagement) {
        neo4jTemplate.deleteById(passwordManagement.getKey(), Neo4JPasswordManagement.class);
    }
}
