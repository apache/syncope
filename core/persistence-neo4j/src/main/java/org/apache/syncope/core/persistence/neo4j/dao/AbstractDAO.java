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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractNode;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public abstract class AbstractDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractDAO.class);

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    public AbstractDAO(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    protected <N extends AbstractNode> Optional<N> findById(
            final String key,
            final Class<N> domainType,
            final Cache<EntityCacheKey, N> cache) {

        if (cache == null) {
            return neo4jTemplate.findById(key, domainType);
        }

        EntityCacheKey cacheKey = EntityCacheKey.of(key);
        return Optional.ofNullable(cache.get(cacheKey)).
                or(() -> neo4jTemplate.findById(key, domainType).
                map(value -> {
                    cache.put(cacheKey, value);
                    return value;
                }));
    }

    protected <E extends Entity, N extends AbstractNode> List<E> findByRelationship(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey,
            final Class<N> leftDomainType,
            final Cache<EntityCacheKey, N> cache) {

        return toList(neo4jClient.query(
                "MATCH (n:" + leftNode + ")-[]-(p:" + rightNode + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", rightNodeKey)).fetch().all(),
                "n.id",
                leftDomainType,
                cache);
    }

    protected <E extends Entity, N extends AbstractNode> List<E> findByRelationship(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey,
            final String relationshipType,
            final Class<N> leftDomainType,
            final Cache<EntityCacheKey, N> cache) {

        return toList(neo4jClient.query(
                "MATCH (n:" + leftNode + ")-[:" + relationshipType + "]-(p:" + rightNode + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", rightNodeKey)).fetch().all(),
                "n.id",
                leftDomainType,
                cache);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Entity, N extends AbstractNode> Function<Map<String, Object>, Optional<E>> toOptional(
            final String property,
            final Class<N> domainType,
            final Cache<EntityCacheKey, N> cache) {

        return found -> findById(found.get(property).toString(), domainType, cache).map(n -> (E) n);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Entity, N extends AbstractNode> List<E> toList(
            final Collection<Map<String, Object>> result,
            final String property,
            final Class<N> domainType,
            final Cache<EntityCacheKey, N> cache) {

        return result.stream().
                map(found -> findById(found.get(property).toString(), domainType, cache)).
                flatMap(Optional::stream).map(n -> (E) n).toList();
    }

    protected void cascadeDelete(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey) {

        try {
            neo4jClient.query(
                    "MATCH (n:" + leftNode + ")-[]-(p:" + rightNode + " {id: $id}) DETACH DELETE n").
                    bindAll(Map.of("id", rightNodeKey)).
                    run();
        } catch (Exception e) {
            LOG.error("While removing n via (n:{})-[]-(p:{} {id: {}})", leftNode, rightNode, rightNodeKey, e);
        }
    }

    protected void deleteRelationship(
            final String leftNode,
            final String rightNode,
            final String leftNodeKey,
            final String rightNodeKey,
            final String relationshipType) {

        try {
            neo4jClient.query(
                    "MATCH (n:" + leftNode + " {id: $tid})-"
                    + "[r:" + relationshipType + "]-"
                    + "(p:" + rightNode + "{id: $iid}) "
                    + "DELETE r").
                    bindAll(Map.of("tid", leftNodeKey, "iid", rightNodeKey)).
                    run();
        } catch (Exception e) {
            LOG.error("While removing r via (n:{} {id: {}})-[r]-(p:{} {id: {}})",
                    leftNode, leftNodeKey, rightNode, rightNodeKey, e);
        }
    }
}
