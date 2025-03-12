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
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

public class GroupRepoExtImpl extends AbstractAnyRepoExt<Group> implements GroupRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final AnyMatchDAO anyMatchDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    public GroupRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final DynRealmDAO dynRealmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final AnyMatchDAO anyMatchDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        super(
                dynRealmDAO,
                plainSchemaDAO,
                entityManager,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        this.publisher = publisher;
        this.anyMatchDAO = anyMatchDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.anySearchDAO = searchDAO;
        this.searchCondVisitor = searchCondVisitor;
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
        Query query = entityManager.createQuery(
                "SELECT e.realm, COUNT(e) FROM " + anyUtils.anyClass().getSimpleName() + " e GROUP BY e.realm");

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream().collect(Collectors.toMap(
                result -> ((Realm) result[0]).getFullPath(),
                result -> ((Number) result[1]).longValue()));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final String userKey) {
        User owner = userDAO.findById(userKey).orElse(null);
        if (owner == null) {
            return List.of();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(anyUtils.anyClass().getSimpleName())
                .append(" e WHERE e.userOwner=:owner ");
        userDAO.findAllGroupKeys(owner).forEach(groupKey -> queryString.
                append("OR e.groupOwner.id='").append(groupKey).append("' "));

        TypedQuery<Group> query = entityManager.createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
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
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + JPAAMembership.TABLE + " WHERE group_id=? AND anyobject_it=?");
        query.setParameter(1, groupKey);
        query.setParameter(2, anyObjectKey);

        return ((Number) query.getSingleResult()).longValue() > 0;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsUMembership(final String userKey, final String groupKey) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + JPAUMembership.TABLE + " WHERE group_id=? AND user_id=?");
        query.setParameter(1, groupKey);
        query.setParameter(2, userKey);

        return ((Number) query.getSingleResult()).longValue() > 0;
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
    public List<UMembership> findUMemberships(final Group group) {
        TypedQuery<UMembership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUMembership.class.getSimpleName() + " e WHERE e.rightEnd=:group",
                UMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public <S extends Group> S save(final S group) {
        checkBeforeSave((JPAGroup) group);
        return entityManager.merge(group);
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
                    Query insert = entityManager.createNativeQuery(
                            "INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                    insert.setParameter(1, user.getKey());
                    insert.setParameter(2, merged.getKey());
                    insert.executeUpdate();

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
                    Query insert = entityManager.createNativeQuery(
                            "INSERT INTO " + ADYNMEMB_TABLE + " VALUES(?, ?, ?)");
                    insert.setParameter(1, any.getType().getKey());
                    insert.setParameter(2, any.getKey());
                    insert.setParameter(3, merged.getKey());
                    insert.executeUpdate();

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

            anyObjectDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        findUMemberships(group).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(leftEnd::remove);

            userDAO.save(leftEnd);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, leftEnd, AuthContextUtils.getDomain()));
        });

        clearUDynMembers(group);
        clearADynMembers(group);

        entityManager.remove(group);
    }

    @Override
    public List<TypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        TypedQuery<TypeExtension> query = entityManager.createQuery(
                "SELECT e FROM " + JPATypeExtension.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", TypeExtension.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    public long countADynMembers(final Group group) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(DISTINCT any_id) FROM " + ADYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public long countUDynMembers(final Group group) {
        if (group.getUDynMembership() == null) {
            return 0;
        }

        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(DISTINCT any_id) FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> findADynMembers(final Group group) {
        List<String> result = new ArrayList<>();

        group.getADynMemberships().forEach(memb -> {
            Query query = entityManager.createNativeQuery(
                    "SELECT DISTINCT any_id FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND anyType_id=?");
            query.setParameter(1, group.getKey());
            query.setParameter(2, memb.getAnyType().getKey());

            @SuppressWarnings("unchecked")
            List<Object> queryResult = query.getResultList();
            result.addAll(queryResult.stream().
                    map(Object::toString).
                    filter(anyObject -> !result.contains(anyObject)).
                    toList());
        });

        return result;
    }

    @Override
    public List<String> findUDynMembers(final Group group) {
        if (group.getUDynMembership() == null) {
            return List.of();
        }

        Query query = entityManager.createNativeQuery(
                "SELECT DISTINCT any_id FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.stream().
                map(Object::toString).
                toList();
    }

    @Override
    public void clearADynMembers(final Group group) {
        Query delete = entityManager.createNativeQuery("DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    @Override
    public void clearUDynMembers(final Group group) {
        Query delete = entityManager.createNativeQuery("DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    protected List<ADynGroupMembership> findWithADynMemberships(final AnyType anyType) {
        TypedQuery<ADynGroupMembership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAADynGroupMembership.class.getSimpleName() + " e  WHERE e.anyType=:anyType",
                ADynGroupMembership.class);
        query.setParameter("anyType", anyType);
        return query.getResultList();
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

            Query query = entityManager.createNativeQuery(
                    "SELECT COUNT(group_id) FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            query.setParameter(1, memb.getGroup().getKey());
            query.setParameter(2, anyObject.getKey());
            boolean existing = ((Number) query.getSingleResult()).longValue() > 0;
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                Query insert = entityManager.createNativeQuery(
                        "INSERT INTO " + ADYNMEMB_TABLE + " VALUES(?, ?, ?)");
                insert.setParameter(1, anyObject.getType().getKey());
                insert.setParameter(2, anyObject.getKey());
                insert.setParameter(3, memb.getGroup().getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager.createNativeQuery(
                        "DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
                delete.setParameter(1, memb.getGroup().getKey());
                delete.setParameter(2, anyObject.getKey());
                delete.executeUpdate();
            }

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final AnyObject anyObject) {
        List<Group> dynGroups = anyObjectDAO.findDynGroups(anyObject.getKey());

        Query delete = entityManager.createNativeQuery("DELETE FROM " + ADYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, anyObject.getKey());
        delete.executeUpdate();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain()));
        });

        return before;
    }

    protected List<UDynGroupMembership> findWithUDynMemberships() {
        TypedQuery<UDynGroupMembership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAUDynGroupMembership.class.getSimpleName() + " e",
                UDynGroupMembership.class);

        return query.getResultList();
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

            Query query = entityManager.createNativeQuery(
                    "SELECT COUNT(group_id) FROM " + UDYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            query.setParameter(1, memb.getGroup().getKey());
            query.setParameter(2, user.getKey());
            boolean existing = ((Number) query.getSingleResult()).longValue() > 0;
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                Query insert = entityManager.createNativeQuery(
                        "INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, memb.getGroup().getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager.createNativeQuery(
                        "DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
                delete.setParameter(1, memb.getGroup().getKey());
                delete.setParameter(2, user.getKey());
                delete.executeUpdate();
            }

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final User user) {
        List<Group> dynGroups = userDAO.findDynGroups(user.getKey());

        Query delete = entityManager.createNativeQuery("DELETE FROM " + UDYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, user.getKey());
        delete.executeUpdate();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new EntityLifecycleEvent<>(
                    this, SyncDeltaType.UPDATE, group, AuthContextUtils.getDomain()));
        });

        return before;
    }
}
