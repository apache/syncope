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

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractAny;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyRepoExt<A extends Any, N extends AbstractAny>
        extends AbstractDAO implements AnyRepoExt<A> {

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return split value
     */
    protected static List<String> split(final String attrValue, final List<String> literals) {
        final List<String> attrValues = new ArrayList<>();

        if (literals.isEmpty()) {
            attrValues.add(attrValue);
        } else {
            for (String token : attrValue.split(Pattern.quote(literals.getFirst()))) {
                if (!token.isEmpty()) {
                    attrValues.addAll(split(token, literals.subList(1, literals.size())));
                }
            }
        }

        return attrValues;
    }

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final AnyChecker anyChecker;

    protected final AnyFinder anyFinder;

    protected final AnyUtils anyUtils;

    protected AbstractAnyRepoExt(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final AnyChecker anyChecker,
            final AnyFinder anyFinder,
            final AnyUtils anyUtils,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(neo4jTemplate, neo4jClient);
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.anyChecker = anyChecker;
        this.anyFinder = anyFinder;
        this.anyUtils = anyUtils;
    }

    protected abstract Cache<EntityCacheKey, N> cache();

    @Override
    public List<A> findByKeys(final List<String> keys) {
        return toList(neo4jClient.query(
                "MATCH (n:" + AnyRepoExt.node(anyUtils.anyTypeKind()) + ") WHERE n.id IN $keys RETURN n.id").
                bindAll(Map.of("keys", keys)).fetch().all(),
                "n.id",
                anyUtils.anyClass(),
                cache());
    }

    protected Optional<OffsetDateTime> findLastChange(final String key, final String node) {
        return neo4jClient.query("MATCH (n:" + node + " {id: $id}) "
                + "RETURN n.creationDate, n.lastChangeDate").
                bindAll(Map.of("id", key)).fetch().one().map(n -> {
            ZonedDateTime creationDate = (ZonedDateTime) n.get("n.creationDate");
            ZonedDateTime lastChangeDate = (ZonedDateTime) n.get("n.lastChangeDate");
            return Optional.ofNullable(lastChangeDate).orElse(creationDate).toOffsetDateTime();
        });
    }

    protected abstract void securityChecks(A any);

    @SuppressWarnings("unchecked")
    protected Optional<A> findById(final String key) {
        return findById(key, anyUtils.anyClass(), cache()).map(any -> (A) any);
    }

    @Transactional(readOnly = true)
    @Override
    public A authFind(final String key) {
        if (key == null) {
            throw new NotFoundException("Null key");
        }

        A any = findById(key).orElseThrow(() -> new NotFoundException(anyUtils.anyTypeKind().name() + ' ' + key));

        securityChecks(any);

        return any;
    }

    @Override
    public List<A> findByDerAttrValue(final String expression, final String value, final boolean ignoreCaseMatch) {
        return anyFinder.findByDerAttrValue(anyUtils.anyTypeKind(), expression, value, ignoreCaseMatch);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> findDynRealms(final String key) {
        return neo4jClient.query(
                "MATCH (n {id: $id})-"
                + "[:" + DynRealmRepoExt.DYN_REALM_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jDynRealm.NODE + ") "
                + "RETURN p.id").
                bindAll(Map.of("id", key)).fetch().all().stream().
                map(found -> found.get("p.id").toString()).distinct().toList();
    }

    @Override
    public List<A> findByResourcesContaining(final ExternalResource resource) {
        return findByRelationship(
                AnyRepoExt.node(anyUtils.anyTypeKind()),
                Neo4jExternalResource.NODE,
                resource.getKey(),
                anyUtils.anyClass(),
                cache());
    }

    @Override
    public void deleteRelationship(final Relationship<? extends A, AnyObject> relationship) {
        var domainType = relationship instanceof URelationship
                ? Neo4jURelationship.class
                : relationship instanceof GRelationship
                        ? Neo4jGRelationship.class
                        : Neo4jARelationship.class;
        neo4jTemplate.deleteById(relationship.getKey(), domainType);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
