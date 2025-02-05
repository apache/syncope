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
import java.util.Optional;
import javax.cache.Cache;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class AnyTypeRepoExtImpl extends AbstractDAO implements AnyTypeRepoExt {

    protected final RemediationDAO remediationDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jAnyType> cache;

    public AnyTypeRepoExtImpl(
            final RemediationDAO remediationDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyType> cache) {

        super(neo4jTemplate, neo4jClient);
        this.remediationDAO = remediationDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends AnyType> findById(final String key) {
        return findById(key, Neo4jAnyType.class, cache);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getUser() {
        return findById(AnyTypeKind.USER.name()).orElseThrow();
    }

    @Transactional(readOnly = true)
    @Override
    public AnyType getGroup() {
        return findById(AnyTypeKind.GROUP.name()).orElseThrow();
    }

    @Override
    public List<AnyType> findByClassesContaining(final AnyTypeClass anyTypeClass) {
        return findByRelationship(
                Neo4jAnyType.NODE,
                Neo4jAnyTypeClass.NODE,
                anyTypeClass.getKey(),
                Neo4jAnyType.class,
                cache);
    }

    @Transactional
    @Override
    public AnyType save(final AnyType anyType) {
        AnyType saved = neo4jTemplate.save(nodeValidator.validate(anyType));
        cache.put(EntityCacheKey.of(saved.getKey()), (Neo4jAnyType) saved);
        return saved;
    }

    @Override
    public void deleteById(final String key) {
        AnyType anyType = findById(key).orElse(null);
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

        relationshipTypeDAO.findByEndAnyType(anyType).forEach(relationshipTypeDAO::deleteById);

        cache.remove(EntityCacheKey.of(key));

        neo4jTemplate.deleteById(key, Neo4jAnyType.class);
    }
}
