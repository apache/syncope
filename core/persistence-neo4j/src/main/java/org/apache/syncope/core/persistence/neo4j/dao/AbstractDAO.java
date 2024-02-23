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
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractNode;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public abstract class AbstractDAO {

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    public AbstractDAO(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    protected <E extends Entity, N extends AbstractNode> List<E> findByRelationship(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey,
            final Class<N> leftDomainType) {

        return toList(neo4jClient.query(
                "MATCH (n:" + leftNode + ")-[]-(p:" + rightNode + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", rightNodeKey)).fetch().all(),
                "n.id",
                leftDomainType);
    }

    protected <E extends Entity, N extends AbstractNode> List<E> findByRelationship(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey,
            final String relationshipType,
            final Class<N> leftDomainType) {

        return toList(neo4jClient.query(
                "MATCH (n:" + leftNode + ")-[:" + relationshipType + "]-(p:" + rightNode + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", rightNodeKey)).fetch().all(),
                "n.id",
                leftDomainType);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Entity, N extends AbstractNode> Function<Map<String, Object>, Optional<E>> toOptional(
            final String property, final Class<N> domainType) {

        return found -> neo4jTemplate.findById(found.get(property), domainType).map(n -> (E) n);
    }

    @SuppressWarnings("unchecked")
    protected <E extends Entity, N extends AbstractNode> List<E> toList(
            final Collection<Map<String, Object>> result,
            final String property,
            final Class<N> domainType) {

        return result.stream().map(found -> neo4jTemplate.findById(found.get(property), domainType)).
                filter(Optional::isPresent).map(Optional::get).map(n -> (E) n).toList();
    }

    protected void cascadeDelete(
            final String leftNode,
            final String rightNode,
            final String rightNodeKey,
            final Class<? extends AbstractNode> leftDomainType) {

        neo4jClient.query(
                "MATCH (n:" + leftNode + ")-[]-(p:" + rightNode + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", rightNodeKey)).fetch().all().
                forEach(r -> neo4jTemplate.deleteById(r.get("n.id").toString(), leftDomainType));
    }
}
