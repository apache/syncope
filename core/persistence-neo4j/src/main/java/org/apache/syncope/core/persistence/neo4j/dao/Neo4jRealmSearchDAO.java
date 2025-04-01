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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jRealmSearchDAO extends AbstractDAO implements RealmSearchDAO {

    protected static StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final String keyword,
            final Map<String, Object> parameters) {

        AtomicInteger index = new AtomicInteger(0);
        String basesClause = bases.stream().map(base -> {
            int idx = index.incrementAndGet();
            parameters.put("base" + idx, base);
            parameters.put("like" + idx, SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*");
            return "n.fullPath = $base" + idx + " OR n.fullPath =~ $like" + idx;
        }).collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("MATCH (n:").append(Neo4jRealm.NODE).append(") ").
                append("WHERE (").append(basesClause).append(')');

        if (keyword != null) {
            queryString.append(" AND toLower(n.name) =~ $name");
            parameters.put("name", keyword.replace("%", ".*").replaceAll("_", "\\\\_").toLowerCase() + ".*");
        }

        return queryString;
    }

    protected final Cache<EntityCacheKey, Neo4jRealm> cache;

    public Neo4jRealmSearchDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final Cache<EntityCacheKey, Neo4jRealm> cache) {

        super(neo4jTemplate, neo4jClient);
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (StringUtils.isBlank(fullPath)
                || (!SyncopeConstants.ROOT_REALM.equals(fullPath)
                && !RealmDAO.PATH_PATTERN.matcher(fullPath).matches())) {

            throw new MalformedPathException(fullPath);
        }

        return neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + ") WHERE n.fullPath = $fullPath RETURN n.id").
                bindAll(Map.of("fullPath", fullPath)).fetch().one().
                flatMap(toOptional("n.id", Neo4jRealm.class, cache));
    }

    @Override
    public List<Realm> findByName(final String name) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + ") WHERE n.name = $name RETURN n.id").
                bindAll(Map.of("name", name)).fetch().all(), "n.id", Neo4jRealm.class, cache);
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + " {id: $id})<-[r:" + Neo4jRealm.PARENT_REL + "]-(c) RETURN c.id").
                bindAll(Map.of("id", realm.getKey())).fetch().all(), "c.id", Neo4jRealm.class, cache);
    }

    @Override
    public long countDescendants(final String base, final String keyword) {
        return countDescendants(Set.of(base), keyword);
    }

    @Override
    public long countDescendants(final Set<String> bases, final String keyword) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters).append(" RETURN COUNT(n)");
        return neo4jTemplate.count(queryString.toString(), parameters);
    }

    @Override
    public List<Realm> findDescendants(final String base, final String keyword, final Pageable pageable) {
        return findDescendants(Set.of(base), keyword, pageable);
    }

    @Override
    public List<Realm> findDescendants(final Set<String> bases, final String keyword, final Pageable pageable) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters).
                append(" RETURN n.id ORDER BY n.fullPath");
        if (pageable.isPaged()) {
            queryString.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                queryString.toString()).bindAll(parameters).fetch().all(), "n.id", Neo4jRealm.class, cache);
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder queryString = buildDescendantsQuery(Set.of(base), null, parameters).
                append(" AND (n.fullPath = $prefix OR n.fullPath =~ $likePrefix)").
                append(" RETURN n.id ORDER BY n.fullPath");
        parameters.put("prefix", prefix);
        parameters.put("likePrefix", SyncopeConstants.ROOT_REALM.equals(prefix) ? "/.*" : prefix + "/.*");

        return neo4jClient.query(queryString.toString()).
                bindAll(parameters).fetch().all().stream().
                map(found -> (String) found.values().iterator().next()).toList();
    }
}
