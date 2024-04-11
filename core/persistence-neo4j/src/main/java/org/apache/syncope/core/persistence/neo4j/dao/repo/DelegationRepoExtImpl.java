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
import java.util.Map;
import java.util.Optional;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDelegation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class DelegationRepoExtImpl extends AbstractDAO implements DelegationRepoExt {

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jDelegation> cache;

    public DelegationRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jDelegation> cache) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Override
    public Optional<? extends Delegation> findById(final String key) {
        return findById(key, Neo4jDelegation.class, cache);
    }

    @Override
    public List<Delegation> findByDelegating(final User user) {
        return toList(neo4jClient.query(
                "MATCH (l:" + Neo4jUser.NODE + " {id: $id})-"
                + "[:" + Neo4jDelegation.DELEGATING_REL + "]-"
                + "(n:" + Neo4jDelegation.NODE + ") "
                + "RETURN n.id").bindAll(Map.of("id", user.getKey())).fetch().all(),
                "n.id",
                Neo4jDelegation.class,
                cache);
    }

    @Override
    public List<Delegation> findByDelegated(final User user) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jDelegation.NODE + ")-"
                + "[:" + Neo4jDelegation.DELEGATED_REL + "]-"
                + "(u:" + Neo4jUser.NODE + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", user.getKey())).fetch().all(),
                "n.id",
                Neo4jDelegation.class,
                cache);
    }

    @Override
    public List<Delegation> findByRoles(final Role role) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jDelegation.NODE + ")-[]-(r:" + Neo4jRole.NODE + " {id: $id}) "
                + "RETURN n.id").bindAll(Map.of("id", role.getKey())).fetch().all(),
                "n.id",
                Neo4jDelegation.class,
                cache);
    }

    @Override
    public Delegation save(final Delegation delegation) {
        Delegation saved = neo4jTemplate.save(nodeValidator.validate(delegation));
        cache.put(EntityCacheKey.of(delegation.getKey()), (Neo4jDelegation) saved);
        return saved;
    }

    @Override
    public void delete(final Delegation delegation) {
        cache.remove(EntityCacheKey.of(delegation.getKey()));

        neo4jTemplate.deleteById(delegation.getKey(), Neo4jDelegation.class);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
