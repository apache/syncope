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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AnyObjectRepoExtImpl extends AbstractAnyRepoExt<AnyObject, Neo4jAnyObject> implements AnyObjectRepoExt {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache;

    public AnyObjectRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyFinder anyFinder,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache) {

        super(
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT),
                neo4jTemplate,
                neo4jClient);
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.nodeValidator = nodeValidator;
        this.anyObjectCache = anyObjectCache;
    }

    @Override
    protected Cache<EntityCacheKey, Neo4jAnyObject> cache() {
        return anyObjectCache;
    }

    @Override
    public Optional<? extends AnyObject> findByName(final String type, final String name) {
        return neo4jClient.query(
                "MATCH (n:" + Neo4jAnyObject.NODE + ")-[]-(a:" + Neo4jAnyType.NODE + " {id: $type}) "
                + "WHERE n.name = $name "
                + "RETURN n.id").
                bindAll(Map.of("type", type, "name", name)).fetch().one().
                flatMap(toOptional("n.id", Neo4jAnyObject.class, cache()));
    }

    @Override
    public List<AnyObject> findByName(final String name) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jAnyObject.NODE + ") WHERE n.name = $name RETURN n.id").
                bindAll(Map.of("name", name)).fetch().all(),
                "n.id",
                Neo4jAnyObject.class,
                cache());
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OffsetDateTime> findLastChange(final String key) {
        return findLastChange(key, Neo4jAnyObject.NODE);
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm,
            final Collection<String> groups) {

        // 1. check if AuthContextUtils.getUsername() is owner of at least one group of which anyObject is member
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.parseGroupOwnerRealm(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(pair -> groups.contains(pair.getRight()));

        // 2. check if anyObject is in at least one DynRealm for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized && key != null) {
            authorized = findDynRealms(key).stream().anyMatch(authRealms::contains);
        }

        // 3. check if anyObject is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = authRealms.stream().anyMatch(realm::startsWith);
        }

        if (!authorized) {
            Optional.ofNullable(key).map(EntityCacheKey::of).ifPresent(anyObjectCache::remove);
            throw new DelegatedAdministrationException(realm, AnyTypeKind.ANY_OBJECT.name(), key);
        }
    }

    @Override
    protected void securityChecks(final AnyObject anyObject) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().
                getOrDefault(AnyEntitlement.READ.getFor(anyObject.getType().getKey()), Set.of());

        securityChecks(authRealms, anyObject.getKey(), anyObject.getRealm().getFullPath(), findAllGroupKeys(anyObject));
    }

    @Override
    public Map<AnyType, Long> countByType() {
        Collection<Map<String, Object>> result = neo4jClient.query(
                "MATCH (n:" + Neo4jAnyObject.NODE + ")-[]-(a:" + Neo4jAnyType.NODE + ") "
                + "RETURN a.id, COUNT(n) AS counted").fetch().all();

        return result.stream().collect(Collectors.toMap(
                r -> neo4jTemplate.findById(r.get("a.id"), Neo4jAnyType.class).orElseThrow(),
                r -> (Long) r.get("counted")));
    }

    @Override
    public Map<String, Long> countByRealm(final AnyType anyType) {
        Collection<Map<String, Object>> result = neo4jClient.query(
                "MATCH (r:" + Neo4jRealm.NODE + ")-[]-"
                + "(n:" + Neo4jAnyObject.NODE + ")-[]-"
                + "(a:" + Neo4jAnyType.NODE + " {id: $aid}) "
                + "RETURN r.fullPath AS realm, COUNT(n) AS counted").
                bindAll(Map.of("aid", anyType.getKey())).fetch().all();

        return result.stream().collect(Collectors.toMap(r -> r.get("realm").toString(), r -> (Long) r.get("counted")));
    }

    @Override
    public AMembership findMembership(final String key) {
        return neo4jTemplate.findById(key, Neo4jAMembership.class).orElse(null);
    }

    @Override
    public void deleteMembership(final AMembership membership) {
        neo4jTemplate.deleteById(membership.getKey(), Neo4jAMembership.class);
    }

    @Override
    public List<Relationship<Any, AnyObject>> findAllRelationships(final AnyObject anyObject) {
        List<Relationship<Any, AnyObject>> result = new ArrayList<>();

        result.addAll(toList(neo4jClient.query(
                "MATCH (n:" + Neo4jARelationship.NODE + ")-[]-(a:" + Neo4jAnyObject.NODE + " {id: $aid}) "
                + "RETURN n.id").bindAll(Map.of("aid", anyObject.getKey())).fetch().all(),
                "n.id",
                Neo4jARelationship.class,
                null));

        result.addAll(toList(neo4jClient.query(
                "MATCH (n:" + Neo4jURelationship.NODE + ")-[]-(a:" + Neo4jAnyObject.NODE + " {id: $aid}) "
                + "RETURN n.id").bindAll(Map.of("aid", anyObject.getKey())).fetch().all(),
                "n.id",
                Neo4jURelationship.class,
                null));

        return result;
    }

    protected <S extends AnyObject> Pair<S, Pair<Set<String>, Set<String>>> doSave(final S anyObject) {
        checkBeforeSave(anyObject);

        // unlink any resource or aux class that was unlinked from anyObject
        // delete any membership or relationship that was removed from anyObject
        neo4jTemplate.findById(anyObject.getKey(), Neo4jAnyObject.class).ifPresent(before -> {
            before.getResources().stream().filter(resource -> !anyObject.getResources().contains(resource)).
                    forEach(resource -> deleteRelationship(
                    Neo4jAnyObject.NODE,
                    Neo4jExternalResource.NODE,
                    anyObject.getKey(),
                    resource.getKey(),
                    Neo4jAnyObject.ANY_OBJECT_RESOURCE_REL));
            before.getAuxClasses().stream().filter(auxClass -> !anyObject.getAuxClasses().contains(auxClass)).
                    forEach(auxClass -> deleteRelationship(
                    Neo4jAnyObject.NODE,
                    Neo4jAnyTypeClass.NODE,
                    anyObject.getKey(),
                    auxClass.getKey(),
                    Neo4jAnyObject.ANY_OBJECT_AUX_CLASSES_REL));

            Set<String> beforeMembs = before.getMemberships().stream().map(AMembership::getKey).
                    collect(Collectors.toSet());
            beforeMembs.removeAll(anyObject.getMemberships().stream().map(AMembership::getKey).toList());
            beforeMembs.forEach(m -> neo4jTemplate.deleteById(m, Neo4jAMembership.class));

            Set<String> beforeRels = before.getRelationships().stream().map(ARelationship::getKey).
                    collect(Collectors.toSet());
            beforeRels.removeAll(anyObject.getRelationships().stream().map(ARelationship::getKey).toList());
            beforeRels.forEach(r -> neo4jTemplate.deleteById(r, Neo4jARelationship.class));
        });

        S merged = neo4jTemplate.save(nodeValidator.validate(anyObject));

        anyObjectCache.put(EntityCacheKey.of(merged.getKey()), (Neo4jAnyObject) merged);

        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    public <S extends AnyObject> S save(final S anyObject) {
        return doSave(anyObject).getLeft();
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final AnyObject anyObject) {
        return doSave(anyObject).getRight();
    }

    protected List<ARelationship> findARelationships(final AnyObject anyObject) {
        return findByRelationship(
                Neo4jARelationship.NODE,
                Neo4jAnyObject.NODE,
                anyObject.getKey(),
                Neo4jARelationship.DEST_REL,
                Neo4jARelationship.class,
                null);
    }

    protected List<URelationship> findURelationships(final AnyObject anyObject) {
        return findByRelationship(
                Neo4jURelationship.NODE,
                Neo4jAnyObject.NODE,
                anyObject.getKey(),
                Neo4jURelationship.DEST_REL,
                Neo4jURelationship.class,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroups(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jAnyObject.NODE + " {id: $id})-"
                + "[:" + GroupRepoExt.DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jGroup.class,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final AnyObject anyObject) {
        Set<Group> result = new HashSet<>();
        result.addAll(anyObject.getMemberships().stream().
                map(AMembership::getRightEnd).collect(Collectors.toSet()));
        result.addAll(findDynGroups(anyObject.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final AnyObject anyObject) {
        return findAllGroups(anyObject).stream().map(Group::getKey).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final AnyObject anyObject) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(anyObject.getResources());
        findAllGroups(anyObject).forEach(group -> result.addAll(group.getResources()));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findAllResources(authFind(key)).stream().map(ExternalResource::getKey).toList();
    }

    @Override
    public void delete(final AnyObject anyObject) {
        groupDAO.removeDynMemberships(anyObject);
        dynRealmDAO.removeDynMemberships(anyObject.getKey());

        findARelationships(anyObject).forEach(relationship -> {
            findById(relationship.getLeftEnd().getKey()).ifPresent(le -> {
                le.getRelationships().remove(relationship);
                save(le);
            });

            neo4jTemplate.deleteById(relationship.getKey(), Neo4jARelationship.class);
        });
        findURelationships(anyObject).forEach(relationship -> {
            userDAO.findById(relationship.getLeftEnd().getKey()).ifPresent(le -> {
                le.getRelationships().remove(relationship);
                userDAO.save(le);
            });

            neo4jTemplate.deleteById(relationship.getKey(), Neo4jURelationship.class);
        });

        anyObjectCache.remove(EntityCacheKey.of(anyObject.getKey()));

        cascadeDelete(
                Neo4jAMembership.NODE,
                Neo4jAnyObject.NODE,
                anyObject.getKey());

        neo4jTemplate.deleteById(anyObject.getKey(), Neo4jAnyObject.class);
    }
}
