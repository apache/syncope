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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAccessToken;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jBatch;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class Neo4jBatchDAO implements BatchDAO {

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    protected final NodeValidator nodeValidator;

    public Neo4jBatchDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsById(final String key) {
        return neo4jTemplate.existsById(key, Neo4jBatch.class);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Batch> findById(final String key) {
        return neo4jTemplate.findById(key, Neo4jBatch.class).map(Batch.class::cast);
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        return neo4jTemplate.count(Neo4jBatch.class);
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends Batch> findAll() {
        return neo4jTemplate.findAll(Neo4jBatch.class);
    }

    @Override
    public <S extends Batch> S save(final S batch) {
        return neo4jTemplate.save(nodeValidator.validate(batch));
    }

    @Override
    public void delete(final Batch batch) {
        neo4jTemplate.deleteById(batch.getKey(), Neo4jBatch.class);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public long deleteExpired() {
        Map<String, Object> result = neo4jClient.query(
                "MATCH (n:" + Neo4jAccessToken.NODE + " WHERE n.expirationTime < $now) "
                + "DETACH DELETE n "
                + "RETURN count(*) AS deleted").bindAll(Map.of("now", OffsetDateTime.now())).
                fetch().one().orElse(Map.of("deleted", 0L));

        return (long) result.getOrDefault(result.get("deleted"), 0L);
    }
}
