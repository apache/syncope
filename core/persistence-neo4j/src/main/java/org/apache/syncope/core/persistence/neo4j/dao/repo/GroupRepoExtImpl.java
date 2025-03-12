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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jADynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jTypeExtension;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUDynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class GroupRepoExtImpl extends AbstractAnyRepoExt<Group, Neo4jGroup> implements GroupRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final AnyMatchDAO anyMatchDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jGroup> groupCache;

    public GroupRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final AnyFinder anyFinder,
            final SearchCondVisitor searchCondVisitor,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache) {

        super(
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.GROUP),
                neo4jTemplate,
                neo4jClient);
        this.publisher = publisher;
        this.anyMatchDAO = anyMatchDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.anySearchDAO = anySearchDAO;
        this.searchCondVisitor = searchCondVisitor;
        this.nodeValidator = nodeValidator;
        this.groupCache = groupCache;
    }

    @Override
    protected Cache<EntityCacheKey, Neo4jGroup> cache() {
        return groupCache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OffsetDateTime> findLastChange(final String key) {
        return findLastChange(key, Neo4jGroup.NODE);
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm) {

        // 1. check if AuthContextUtils.getUsername() is owner of the group, or
        // if group is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        boolean authorized = authRealms.stream().anyMatch(authRealm -> realm.startsWith(authRealm)
                || authRealm.equals(RealmUtils.getGroupOwnerRealm(realm, key)));

        // 2. check if groups is in at least one DynRealm for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized && key != null) {
            authorized = findDynRealms(key).stream().anyMatch(authRealms::contains);
        }

        if (authRealms.isEmpty() || !authorized) {
            Optional.ofNullable(key).map(EntityCacheKey::of).ifPresent(groupCache::remove);
            throw new DelegatedAdministrationException(realm, AnyTypeKind.GROUP.name(), key);
        }
    }

    @Override
    protected void securityChecks(final Group group) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().
                getOrDefault(IdRepoEntitlement.GROUP_READ, Set.of());

        securityChecks(authRealms, group.getKey(), group.getRealm().getFullPath());
    }

    @Override
    public Map<String, Long> countByRealm() {
        Collection<Map<String, Object>> result = neo4jClient.query(
                "MATCH (n:" + Neo4jGroup.NODE + ")-[]-(r:" + Neo4jRealm.NODE + ") "
                + "RETURN r.fullPath AS realm, COUNT(n) AS counted").fetch().all();

        return result.stream().collect(Collectors.toMap(r -> r.get("realm").toString(), r -> (Long) r.get("counted")));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final String userKey) {
        User owner = userDAO.findById(userKey).orElse(null);
        if (owner == null) {
            return List.of();
        }

        Set<String> owned = neo4jClient.query(
                "MATCH (n:" + Neo4jGroup.NODE + ")-[]-(u:" + Neo4jUser.NODE + " {id: $userKey}) "
                + "RETURN n.id").bindAll(Map.of("userKey", userKey)).fetch().all().
                stream().map(r -> r.get("n.id").toString()).collect(Collectors.toSet());

        Map<String, Object> parameters = new HashMap<>();
        StringBuilder query = new StringBuilder("MATCH (n:" + Neo4jGroup.NODE + ")-[]-(o:" + Neo4jGroup.NODE + ") ");

        Collection<String> matching = userDAO.findAllGroupKeys(owner);
        if (!matching.isEmpty()) {
            AtomicInteger index = new AtomicInteger(0);
            query.append("WHERE (").
                    append(matching.stream().map(group -> {
                        int idx = index.incrementAndGet();
                        parameters.put("group" + idx, group);
                        return "o.id = $group" + idx;
                    }).collect(Collectors.joining(" OR "))).
                    append(") ");
        }

        query.append("RETURN n.id");

        owned.addAll(neo4jClient.query(query.toString()).bindAll(parameters).fetch().all().
                stream().map(r -> r.get("n.id").toString()).collect(Collectors.toSet()));

        return owned.stream().
                map(id -> neo4jTemplate.findById(id, Neo4jGroup.class)).
                flatMap(Optional::stream).map(Group.class::cast).toList();
    }

    @Override
    public List<Group> findOwnedByGroup(final String groupKey) {
        return findByRelationship(
                Neo4jGroup.NODE,
                Neo4jGroup.NODE,
                groupKey,
                Neo4jGroup.class,
                groupCache);
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findById(key).map(Any::getResources).
            orElseGet(List::of).
                stream().map(ExternalResource::getKey).toList();
    }

    @Override
    public List<AMembership> findAMemberships(final Group group) {
        return toList(
                neo4jClient.query(
                        "MATCH (n:" + Neo4jAMembership.NODE + ")-[]-(g:" + Neo4jGroup.NODE + " {id: $id}) "
                        + "RETURN n.id").bindAll(Map.of("id", group.getKey())).fetch().all(),
                "n.id",
                Neo4jAMembership.class,
                null);
    }

    @Override
    public List<UMembership> findUMemberships(final Group group) {
        return toList(
                neo4jClient.query(
                        "MATCH (n:" + Neo4jUMembership.NODE + ")-[]-(g:" + Neo4jGroup.NODE + " {id: $id}) "
                        + "RETURN n.id").bindAll(Map.of("id", group.getKey())).fetch().all(),
                "n.id",
                Neo4jUMembership.class,
                null);
    }

    @Override
    public <S extends Group> S save(final S group) {
        checkBeforeSave(group);

        // unlink any resource, aux class, user or group owner that was unlinked from group
        // delete any dynamic membership or type extension that was removed from group
        neo4jTemplate.findById(group.getKey(), Neo4jGroup.class).ifPresent(before -> {
            before.getResources().stream().filter(resource -> !group.getResources().contains(resource)).
                    forEach(resource -> deleteRelationship(
                    Neo4jGroup.NODE,
                    Neo4jExternalResource.NODE,
                    group.getKey(),
                    resource.getKey(),
                    Neo4jGroup.GROUP_RESOURCE_REL));
            before.getAuxClasses().stream().filter(auxClass -> !group.getAuxClasses().contains(auxClass)).
                    forEach(auxClass -> deleteRelationship(
                    Neo4jGroup.NODE,
                    Neo4jAnyTypeClass.NODE,
                    group.getKey(),
                    auxClass.getKey(),
                    Neo4jGroup.GROUP_AUX_CLASSES_REL));
            if (before.getUserOwner() != null && group.getUserOwner() == null) {
                deleteRelationship(
                        Neo4jGroup.NODE,
                        Neo4jUser.NODE,
                        group.getKey(),
                        before.getUserOwner().getKey(),
                        Neo4jGroup.USER_OWNER_REL);
            }
            if (before.getGroupOwner() != null && group.getGroupOwner() == null) {
                deleteRelationship(
                        Neo4jGroup.NODE,
                        Neo4jGroup.NODE,
                        group.getKey(),
                        before.getGroupOwner().getKey(),
                        Neo4jGroup.GROUP_OWNER_REL);
            }

            if (before.getUDynMembership() != null && group.getUDynMembership() == null) {
                neo4jTemplate.deleteById(before.getUDynMembership().getKey(), Neo4jUDynGroupMembership.class);
            }
            Set<String> beforeADynMembs = before.getADynMemberships().stream().map(ADynGroupMembership::getKey).
                    collect(Collectors.toSet());
            beforeADynMembs.removeAll(group.getADynMemberships().stream().map(ADynGroupMembership::getKey).toList());
            beforeADynMembs.forEach(m -> neo4jTemplate.deleteById(m, Neo4jADynGroupMembership.class));

            Set<String> beforeTypeExts = before.getTypeExtensions().stream().map(TypeExtension::getKey).
                    collect(Collectors.toSet());
            beforeTypeExts.removeAll(group.getTypeExtensions().stream().map(TypeExtension::getKey).toList());
            beforeTypeExts.forEach(r -> neo4jTemplate.deleteById(r, Neo4jTypeExtension.class));
        });

        S merged = neo4jTemplate.save(nodeValidator.validate(group));

        groupCache.put(EntityCacheKey.of(merged.getKey()), (Neo4jGroup) merged);

        return merged;
    }

    @Override
    public Group saveAndRefreshDynMemberships(final Group group) {
        Group merged = save(group);

        // refresh dynamic memberships
        clearUDynMembers(merged);
        if (merged.getUDynMembership() != null) {
            SearchCond cond = SearchCondConverter.convert(searchCondVisitor, merged.getUDynMembership().getFIQLCond());
            long count = anySearchDAO.count(
                    merged.getRealm(), true, Set.of(merged.getRealm().getFullPath()), cond, AnyTypeKind.USER);
            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                List<User> matching = anySearchDAO.search(
                        merged.getRealm(),
                        true,
                        Set.of(merged.getRealm().getFullPath()),
                        cond,
                        PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE),
                        AnyTypeKind.USER);

                matching.forEach(user -> {
                    neo4jClient.query(
                            "MATCH (a:" + Neo4jUser.NODE + " {id: $aid}), (b:" + Neo4jGroup.NODE + "{id: $gid}) "
                            + "CREATE (a)-[:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]->(b)").
                            bindAll(Map.of("aid", user.getKey(), "gid", merged.getKey())).run();

                    publisher.publishEvent(
                            new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));
                });
            }
        }
        clearADynMembers(merged);
        merged.getADynMemberships().forEach(memb -> {
            SearchCond cond = SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond());
            long count = anySearchDAO.count(
                    merged.getRealm(), true, Set.of(merged.getRealm().getFullPath()), cond, AnyTypeKind.ANY_OBJECT);
            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                List<AnyObject> matching = anySearchDAO.search(
                        merged.getRealm(),
                        true,
                        Set.of(merged.getRealm().getFullPath()),
                        cond,
                        PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE),
                        AnyTypeKind.ANY_OBJECT);

                matching.forEach(any -> {
                    neo4jClient.query(
                            "MATCH (a:" + Neo4jAnyObject.NODE + " {id: $aid}), (b:" + Neo4jGroup.NODE + "{id: $gid}) "
                            + "CREATE (a)-[:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]->(b)").
                            bindAll(Map.of("aid", any.getKey(), "gid", merged.getKey())).run();

                    publisher.publishEvent(
                            new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, any, AuthContextUtils.getDomain()));
                });
            }
        });

        dynRealmDAO.refreshDynMemberships(merged);

        return merged;
    }

    @Override
    public void delete(final Group group) {
        dynRealmDAO.removeDynMemberships(group.getKey());

        findAMemberships(group).forEach(membership -> {
            AnyObject leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);
            neo4jTemplate.deleteById(membership.getKey(), Neo4jAMembership.class);

            anyObjectDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        findUMemberships(group).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);
            neo4jTemplate.deleteById(membership.getKey(), Neo4jUMembership.class);

            userDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        clearUDynMembers(group);
        clearADynMembers(group);

        Optional.ofNullable(group.getUDynMembership()).
                ifPresent(r -> neo4jTemplate.deleteById(r.getKey(), Neo4jUDynGroupMembership.class));

        groupCache.remove(EntityCacheKey.of(group.getKey()));

        cascadeDelete(
                Neo4jADynGroupMembership.NODE,
                Neo4jGroup.NODE,
                group.getKey());

        cascadeDelete(
                Neo4jTypeExtension.NODE,
                Neo4jGroup.NODE,
                group.getKey());

        neo4jTemplate.deleteById(group.getKey(), Neo4jGroup.class);
    }

    @Override
    public List<TypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        return findByRelationship(
                Neo4jTypeExtension.NODE,
                Neo4jAnyTypeClass.NODE,
                anyTypeClass.getKey(),
                Neo4jTypeExtension.class,
                null);
    }

    @Override
    public long countADynMembers(final Group group) {
        return neo4jTemplate.count(
                "MATCH (n)-[:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + " {id: $id}) "
                + "RETURN COUNT(DISTINCT n.id)", Map.of("id", group.getKey()));
    }

    @Override
    public long countUDynMembers(final Group group) {
        return neo4jTemplate.count(
                "MATCH (n)-[:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + " {id: $id}) "
                + "RETURN COUNT(DISTINCT n.id)", Map.of("id", group.getKey()));
    }

    @Override
    public List<String> findADynMembers(final Group group) {
        return neo4jClient.query(
                "MATCH (n)-[:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + " {id: $id}) "
                + "RETURN DISTINCT n.id").bindAll(Map.of("id", group.getKey())).fetch().all().stream().
                map(found -> found.get("n.id").toString()).toList();
    }

    @Override
    public List<String> findUDynMembers(final Group group) {
        return neo4jClient.query(
                "MATCH (n)-[:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + " {id: $id}) "
                + "RETURN DISTINCT n.id").bindAll(Map.of("id", group.getKey())).fetch().all().stream().
                map(found -> found.get("n.id").toString()).toList();
    }

    @Override
    public void clearADynMembers(final Group group) {
        neo4jClient.query(
                "MATCH (n)-[r:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-(p:" + Neo4jGroup.NODE + " {id: $id})"
                + "DETACH DELETE r").
                bindAll(Map.of("id", group.getKey())).run();
    }

    @Override
    public void clearUDynMembers(final Group group) {
        neo4jClient.query(
                "MATCH (n)-[r:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-(p:" + Neo4jGroup.NODE + " {id: $id}) "
                + "DETACH DELETE r").
                bindAll(Map.of("id", group.getKey())).run();
    }

    protected List<ADynGroupMembership> findWithADynMemberships(final AnyType anyType) {
        return findByRelationship(
                Neo4jADynGroupMembership.NODE,
                Neo4jAnyType.NODE,
                anyType.getKey(),
                Neo4jADynGroupMembership.class,
                null);
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final AnyObject anyObject) {
        Set<String> before = new HashSet<>();
        Set<String> after = new HashSet<>();
        findWithADynMemberships(anyObject.getType()).forEach(memb -> {
            boolean matches = anyMatchDAO.matches(
                    anyObject, SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()));
            if (matches) {
                after.add(memb.getGroup().getKey());
            }

            boolean existing = neo4jTemplate.count(
                    "MATCH (n:" + Neo4jAnyObject.NODE + " {id: $aid})-"
                    + "[r:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                    + "(p:" + Neo4jGroup.NODE + "{id: $gid}) "
                    + "RETURN COUNT(n)",
                    Map.of("aid", anyObject.getKey(), "gid", memb.getGroup().getKey())) > 0;
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                neo4jClient.query(
                        "MATCH (a:" + Neo4jAnyObject.NODE + " {id: $aid}), (b:" + Neo4jGroup.NODE + "{id: $gid}) "
                        + "CREATE (a)-[:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]->(b)").
                        bindAll(Map.of("aid", anyObject.getKey(), "gid", memb.getGroup().getKey())).run();
            } else if (!matches && existing) {
                neo4jClient.query(
                        "MATCH (n {id: $aid})-"
                        + "[r:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                        + "(p:" + Neo4jGroup.NODE + " {id: $gid}) "
                        + "DETACH DELETE r").
                        bindAll(Map.of("aid", anyObject.getKey(), "gid", memb.getGroup().getKey())).run();
            }

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final AnyObject anyObject) {
        List<Group> dynGroups = anyObjectDAO.findDynGroups(anyObject.getKey());

        neo4jClient.query(
                "MATCH (n {id: $id})-[r:" + DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-(p:" + Neo4jGroup.NODE + ") "
                + "DETACH DELETE r").bindAll(Map.of("id", anyObject.getKey())).run();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain()));
        });

        return before;
    }

    protected List<UDynGroupMembership> findWithUDynMemberships() {
        return neo4jTemplate.findAll(Neo4jUDynGroupMembership.class).stream().
                map(UDynGroupMembership.class::cast).toList();
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final User user) {
        Set<String> before = new HashSet<>();
        Set<String> after = new HashSet<>();
        findWithUDynMemberships().forEach(memb -> {
            boolean matches = anyMatchDAO.matches(
                    user, SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()));
            if (matches) {
                after.add(memb.getGroup().getKey());
            }

            boolean existing = neo4jTemplate.count(
                    "MATCH (n:" + Neo4jUser.NODE + " {id: $aid})-"
                    + "[r:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                    + "(p:" + Neo4jGroup.NODE + "{id: $gid}) "
                    + "RETURN COUNT(n)",
                    Map.of("aid", user.getKey(), "gid", memb.getGroup().getKey())) > 0;
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                neo4jClient.query(
                        "MATCH (a:" + Neo4jUser.NODE + " {id: $aid}), (b:" + Neo4jGroup.NODE + "{id: $gid}) "
                        + "CREATE (a)-[:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]->(b)").
                        bindAll(Map.of("aid", user.getKey(), "gid", memb.getGroup().getKey())).run();
            } else if (!matches && existing) {
                neo4jClient.query(
                        "MATCH (n {id: $aid})-"
                        + "[r:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                        + "(p:" + Neo4jGroup.NODE + " {id: $gid}) "
                        + "DETACH DELETE r").
                        bindAll(Map.of("aid", user.getKey(), "gid", memb.getGroup().getKey())).run();
            }

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final User user) {
        List<Group> dynGroups = userDAO.findDynGroups(user.getKey());

        neo4jClient.query(
                "MATCH (n {id: $id})-[r:" + DYN_GROUP_USER_MEMBERSHIP_REL + "]-(p:" + Neo4jGroup.NODE + ") "
                + "DETACH DELETE r").bindAll(Map.of("id", user.getKey())).run();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain()));
        });

        return before;
    }
}
