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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroupTypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class GroupRepoExtImpl extends AbstractAnyRepoExt<Group> implements GroupRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final RealmDAO realmDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    public GroupRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final RealmDAO realmDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final EntityManager entityManager,
            final AnyChecker anyChecker,
            final AnyFinder anyFinder) {

        super(
                entityManager,
                anyChecker,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        this.publisher = publisher;
        this.realmDAO = realmDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
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
            throw new DelegatedAdministrationException(realm, AnyTypeKind.GROUP.name(), key);
        }
    }

    @Override
    protected void securityChecks(final Group group) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().
                getOrDefault(IdRepoEntitlement.GROUP_READ, Set.of());

        securityChecks(authRealms, group.getKey(), group.getRealm().getFullPath());
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isManager(final String key) {
        long users = query(
                "SELECT COUNT(*) FROM " + JPAUser.TABLE + " WHERE gManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        long groups = query(
                "SELECT COUNT(*) FROM " + JPAGroup.TABLE + " WHERE gManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        long anyObjects = query(
                "SELECT COUNT(*) FROM " + JPAAnyObject.TABLE + " WHERE gManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        return users + groups + anyObjects > 0;
    }

    @Override
    public List<User> findManagedUsers(final String key) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE e.gManager.id=:key", User.class);
        query.setParameter("key", key);
        return query.getResultList();
    }

    @Override
    public List<Group> findManagedGroups(final String key) {
        TypedQuery<Group> query = entityManager.createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.gManager.id=:key", Group.class);
        query.setParameter("key", key);
        return query.getResultList();
    }

    @Override
    public List<AnyObject> findManagedAnyObjects(final String key) {
        TypedQuery<AnyObject> query = entityManager.createQuery(
                "SELECT e FROM " + JPAAnyObject.class.getSimpleName() + " e WHERE e.gManager.id=:key", AnyObject.class);
        query.setParameter("key", key);
        return query.getResultList();
    }

    @Override
    public Map<String, Long> countByRealm() {
        return query(
                "SELECT r.fullPath, COUNT(e.id) "
                + "FROM " + JPAGroup.TABLE + " e JOIN Realm r ON e.realm_id=r.id "
                + "GROUP BY r.fullPath",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getLong(2));
                    }
                    return result;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findById(key).map(Any::getResources).
                orElseGet(List::of).
                stream().map(ExternalResource::getKey).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsAMembership(final String anyObjectKey, final String groupKey) {
        return query(
                "SELECT COUNT(*) FROM " + JPAAMembership.TABLE + " WHERE group_id=? AND anyobject_it=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                groupKey, anyObjectKey) > 0;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsUMembership(final String userKey, final String groupKey) {
        return query(
                "SELECT COUNT(*) FROM " + JPAUMembership.TABLE + " WHERE group_id=? AND user_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                groupKey, userKey) > 0;
    }

    @Override
    public List<AMembership> findAMemberships(final Group group) {
        TypedQuery<AMembership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAAMembership.class.getSimpleName() + " e WHERE e.rightEnd=:group",
                AMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public List<UMembership> findUMemberships(final Group group, final Pageable pageable) {
        TypedQuery<UMembership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUMembership.class.getSimpleName()
                + " e WHERE e.rightEnd=:group ORDER BY e.leftEnd",
                UMembership.class);
        query.setParameter("group", group);
        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    @Override
    public <S extends Group> S save(final S group) {
        anyChecker.checkBeforeSave(group, anyUtils);
        return entityManager.merge(group);
    }

    @Override
    public void delete(final Group group) {
        findAMemberships(group).forEach(membership -> {
            AnyObject leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);

            anyObjectDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        findUMemberships(group, Pageable.unpaged()).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);

            userDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        entityManager.remove(group);
    }

    @Override
    public List<GroupTypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        TypedQuery<GroupTypeExtension> query = entityManager.createQuery(
                "SELECT e FROM " + JPAGroupTypeExtension.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", GroupTypeExtension.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }
}
