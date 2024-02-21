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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class AnyTypeRepoExtImpl extends AbstractDAO implements AnyTypeRepoExt {

    protected final RemediationDAO remediationDAO;

    public AnyTypeRepoExtImpl(
            final RemediationDAO remediationDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(neo4jTemplate, neo4jClient);
        this.remediationDAO = remediationDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getUser() {
        return neo4jTemplate.findById(AnyTypeKind.USER.name(), Neo4jAnyType.class).orElseThrow();
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getGroup() {
        return neo4jTemplate.findById(AnyTypeKind.GROUP.name(), Neo4jAnyType.class).orElseThrow();
    }

    @Override
    public List<AnyType> findByClassesContaining(final AnyTypeClass anyTypeClass) {
        return findByRelationship(Neo4jAnyType.NODE, Neo4jAnyTypeClass.NODE, anyTypeClass.getKey(), Neo4jAnyType.class);
    }

    @Override
    public void deleteById(final String key) {
        AnyType anyType = neo4jTemplate.findById(key, Neo4jAnyType.class).orElse(null);
        if (anyType == null) {
            return;
        }

        if (anyType.equals(getUser()) || anyType.equals(getGroup())) {
            throw new IllegalArgumentException(key + " cannot be deleted");
        }

        remediationDAO.findByAnyType(anyType).forEach(remediation -> {
            remediation.setAnyType(null);
            remediationDAO.delete(remediation);
        });

        neo4jTemplate.deleteById(key, Neo4jAnyType.class);
    }
}
