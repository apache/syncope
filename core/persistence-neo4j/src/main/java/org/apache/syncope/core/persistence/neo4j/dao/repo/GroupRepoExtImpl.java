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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractAny;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroupTypeExtension;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class GroupRepoExtImpl extends AbstractAnyRepoExt<Group, Neo4jGroup> implements GroupRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final RealmDAO realmDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jUser> userCache;

    protected final Cache<EntityCacheKey, Neo4jGroup> groupCache;

    protected final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache;

    public GroupRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final RealmDAO realmDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnyFinder anyFinder,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jUser> userCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache) {

        super(
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.GROUP),
                neo4jTemplate,
                neo4jClient);
        this.publisher = publisher;
        this.realmDAO = realmDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.nodeValidator = nodeValidator;
        this.userCache = userCache;
        this.groupCache = groupCache;
        this.anyObjectCache = anyObjectCache;
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

        // 0. check if AuthContextUtils.getUsername() is manager of the given group
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.ManagerRealm.of(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(managerRealm -> key.equals(managerRealm.anyKey()));

        // 1. check if group is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = authRealms.stream().anyMatch(realm::startsWith);
        }

        if (!authorized) {
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
    public boolean isManager(final String key) {
        return !findManagedUsers(key).isEmpty()
                || !findManagedGroups(key).isEmpty()
                || !findManagedAnyObjects(key).isEmpty();
    }

    @Override
    public List<User> findManagedUsers(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jGroup.NODE + " {id: $id})-"
                + "[:" + AbstractAny.GROUP_MANAGER_REL + "]-"
                + "(p:" + Neo4jUser.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jUser.class,
                userCache);
    }

    @Override
    public List<Group> findManagedGroups(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jGroup.NODE + " {id: $id})-"
                + "[:" + AbstractAny.GROUP_MANAGER_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jGroup.class,
                groupCache);
    }

    @Override
    public List<AnyObject> findManagedAnyObjects(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jGroup.NODE + " {id: $id})-"
                + "[:" + AbstractAny.GROUP_MANAGER_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jAnyObject.class,
                anyObjectCache);
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
    public List<UMembership> findUMemberships(final Group group, final Pageable pageable) {
        String paged = "";
        if (pageable.isPaged()) {
            paged = " SKIP " + pageable.getPageSize() * pageable.getPageNumber()
                    + " LIMIT " + pageable.getPageSize();
        }
        return toList(
                neo4jClient.query(
                        "MATCH (n:" + Neo4jUMembership.NODE + ")-[]-(g:" + Neo4jGroup.NODE + " {id: $id}) "
                        + "RETURN n.id" + paged)
                        .bindAll(Map.of("id", group.getKey())).fetch().all(),
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
            if (before.getUManager() != null && group.getUManager() == null) {
                deleteRelationship(
                        Neo4jGroup.NODE,
                        Neo4jUser.NODE,
                        group.getKey(),
                        before.getUManager().getKey(),
                        AbstractAny.USER_MANAGER_REL);
            }
            if (before.getGManager() != null && group.getGManager() == null) {
                deleteRelationship(
                        Neo4jGroup.NODE,
                        Neo4jGroup.NODE,
                        group.getKey(),
                        before.getGManager().getKey(),
                        AbstractAny.GROUP_MANAGER_REL);
            }

            Set<String> beforeTypeExts = before.getTypeExtensions().stream().map(GroupTypeExtension::getKey).
                    collect(Collectors.toSet());
            beforeTypeExts.removeAll(group.getTypeExtensions().stream().map(GroupTypeExtension::getKey).toList());
            beforeTypeExts.forEach(r -> neo4jTemplate.deleteById(r, Neo4jGroupTypeExtension.class));
        });

        S merged = neo4jTemplate.save(nodeValidator.validate(group));

        groupCache.put(EntityCacheKey.of(merged.getKey()), (Neo4jGroup) merged);

        return merged;
    }

    @Override
    public void delete(final Group group) {
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

        findUMemberships(group, Pageable.unpaged()).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);
            neo4jTemplate.deleteById(membership.getKey(), Neo4jUMembership.class);

            userDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        groupCache.remove(EntityCacheKey.of(group.getKey()));

        cascadeDelete(
                Neo4jGroupTypeExtension.NODE,
                Neo4jGroup.NODE,
                group.getKey());

        neo4jTemplate.deleteById(group.getKey(), Neo4jGroup.class);
    }

    @Override
    public List<GroupTypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        return findByRelationship(
                Neo4jGroupTypeExtension.NODE,
                Neo4jAnyTypeClass.NODE,
                anyTypeClass.getKey(),
                Neo4jGroupTypeExtension.class,
                null);
    }
}
