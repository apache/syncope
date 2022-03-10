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
package org.apache.syncope.core.persistence.jpa.dao;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Entity;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class JPAGroupDAO extends AbstractAnyDAO<Group> implements GroupDAO {

    public static final String UDYNMEMB_TABLE = "UDynGroupMembers";

    public static final String ADYNMEMB_TABLE = "ADynGroupMembers";

    protected final AnyMatchDAO anyMatchDAO;

    protected final PlainAttrDAO plainAttrDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    public JPAGroupDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final PlainAttrDAO plainAttrDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final SearchCondVisitor searchCondVisitor) {

        super(anyUtilsFactory, publisher, plainSchemaDAO, derSchemaDAO, dynRealmDAO);
        this.anyMatchDAO = anyMatchDAO;
        this.plainAttrDAO = plainAttrDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.anySearchDAO = searchDAO;
        this.searchCondVisitor = searchCondVisitor;
    }

    @Override
    protected AnyUtils init() {
        return anyUtilsFactory.getInstance(AnyTypeKind.GROUP);
    }

    @Transactional(readOnly = true)
    @Override
    public String findKey(final String name) {
        return findKey(name, JPAGroup.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public OffsetDateTime findLastChange(final String key) {
        return findLastChange(key, JPAGroup.TABLE);
    }

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + anyUtils().anyClass().getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public Map<String, Integer> countByRealm() {
        Query query = entityManager().createQuery(
                "SELECT e.realm, COUNT(e) FROM  " + anyUtils().anyClass().getSimpleName() + " e GROUP BY e.realm");

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream().collect(Collectors.toMap(
                result -> ((Realm) result[0]).getFullPath(),
                result -> ((Number) result[1]).intValue()));
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
        if (!authorized) {
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
    public Group findByName(final String name) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName() + " e WHERE e.name = :name", Group.class);
        query.setParameter("name", name);

        Group result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No group found with name {}", name, e);
        }

        return result;
    }

    @Override
    public List<String> findKeysByNamePattern(final String pattern) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + JPAGroup.TABLE + " WHERE LOWER(name) LIKE LOWER(?1)");
        query.setParameter(1, pattern);

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();
        return raw.stream().map(Object::toString).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final String userKey) {
        User owner = userDAO.find(userKey);
        if (owner == null) {
            return List.of();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(anyUtils().anyClass().getSimpleName())
                .append(" e WHERE e.userOwner=:owner ");
        userDAO.findAllGroupKeys(owner).forEach(groupKey -> queryString
                .append("OR e.groupOwner.id='").append(groupKey).append("' "));

        TypedQuery<Group> query = entityManager().createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByGroup(final String groupKey) {
        Group owner = find(groupKey);
        if (owner == null) {
            return List.of();
        }

        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName() + " e WHERE e.groupOwner=:owner", Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Override
    public List<AMembership> findAMemberships(final Group group) {
        TypedQuery<AMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAMembership.class.getSimpleName() + " e WHERE e.rightEnd=:group",
                AMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public List<UMembership> findUMemberships(final Group group) {
        TypedQuery<UMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUMembership.class.getSimpleName() + " e WHERE e.rightEnd=:group",
                UMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public List<Group> findAll(final int page, final int itemsPerPage) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM  " + anyUtils().anyClass().getSimpleName() + " e ORDER BY e.id", Group.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    @Override
    public List<String> findAllKeys(final int page, final int itemsPerPage) {
        return findAllKeys(JPAGroup.TABLE, page, itemsPerPage);
    }

    protected SearchCond buildDynMembershipCond(final String baseCondFIQL, final Realm groupRealm) {
        AssignableCond cond = new AssignableCond();
        cond.setRealmFullPath(groupRealm.getFullPath());
        cond.setFromGroup(true);

        return SearchCond.getAnd(
                SearchCond.getLeaf(cond),
                SearchCondConverter.convert(searchCondVisitor, baseCondFIQL));
    }

    @Override
    public Group saveAndRefreshDynMemberships(final Group group) {
        Group merged = save(group);
        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));

        // refresh dynamic memberships
        clearUDynMembers(merged);
        if (merged.getUDynMembership() != null) {
            SearchCond cond = buildDynMembershipCond(merged.getUDynMembership().getFIQLCond(), merged.getRealm());
            int count = anySearchDAO.count(Set.of(merged.getRealm().getFullPath()), cond, AnyTypeKind.USER);
            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                List<User> matching = anySearchDAO.search(
                        Set.of(merged.getRealm().getFullPath()),
                        cond,
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE,
                        List.of(),
                        AnyTypeKind.USER);

                matching.forEach(user -> {
                    Query insert = entityManager().createNativeQuery("INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                    insert.setParameter(1, user.getKey());
                    insert.setParameter(2, merged.getKey());
                    insert.executeUpdate();

                    publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
                });
            }
        }
        clearADynMembers(merged);
        merged.getADynMemberships().forEach(memb -> {
            SearchCond cond = buildDynMembershipCond(memb.getFIQLCond(), merged.getRealm());
            int count = anySearchDAO.count(Set.of(merged.getRealm().getFullPath()), cond, AnyTypeKind.ANY_OBJECT);
            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                List<AnyObject> matching = anySearchDAO.search(
                        Set.of(merged.getRealm().getFullPath()),
                        cond,
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE,
                        List.of(),
                        AnyTypeKind.ANY_OBJECT);

                matching.forEach(anyObject -> {
                    Query insert = entityManager().createNativeQuery(
                            "INSERT INTO " + ADYNMEMB_TABLE + " VALUES(?, ?, ?)");
                    insert.setParameter(1, anyObject.getType().getKey());
                    insert.setParameter(2, anyObject.getKey());
                    insert.setParameter(3, merged.getKey());
                    insert.executeUpdate();

                    publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, anyObject, AuthContextUtils.getDomain()));
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
            leftEnd.getPlainAttrs(membership).forEach(attr -> {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);
                plainAttrDAO.delete(attr);
            });

            anyObjectDAO.save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd, AuthContextUtils.getDomain()));
        });

        findUMemberships(group).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).forEach(attr -> {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);

                plainAttrDAO.delete(attr);
            });

            userDAO.save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd, AuthContextUtils.getDomain()));
        });

        clearUDynMembers(group);
        clearADynMembers(group);

        entityManager().remove(group);
        publisher.publishEvent(new AnyDeletedEvent(
                this, AnyTypeKind.GROUP, group.getKey(), group.getName(), AuthContextUtils.getDomain()));
    }

    @Override
    public List<TypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        TypedQuery<TypeExtension> query = entityManager().createQuery(
                "SELECT e FROM " + JPATypeExtension.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", TypeExtension.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findADynMembers(final Group group) {
        List<String> result = new ArrayList<>();

        group.getADynMemberships().forEach(memb -> {
            Query query = entityManager().createNativeQuery(
                    "SELECT any_id FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND anyType_id=?");
            query.setParameter(1, group.getKey());
            query.setParameter(2, memb.getAnyType().getKey());

            query.getResultList().stream().map(key -> key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key)).
                    filter(anyObject -> !result.contains((String) anyObject)).
                    forEach(anyObject -> result.add((String) anyObject));
        });

        return result;
    }

    @Override
    public int countAMembers(final Group group) {
        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(anyObject_id) FROM " + JPAAMembership.TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public int countUMembers(final Group group) {
        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(user_id) FROM " + JPAUMembership.TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public int countADynMembers(final Group group) {
        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(any_id) FROM " + ADYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public int countUDynMembers(final Group group) {
        if (group.getUDynMembership() == null) {
            return 0;
        }

        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(any_id) FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public void clearADynMembers(final Group group) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    protected List<ADynGroupMembership> findWithADynMemberships(final AnyType anyType) {
        TypedQuery<ADynGroupMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAADynGroupMembership.class.getSimpleName() + " e  WHERE e.anyType=:anyType",
                ADynGroupMembership.class);
        query.setParameter("anyType", anyType);
        return query.getResultList();
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final AnyObject anyObject) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.ADYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, anyObject.getKey());

        Set<String> before = new HashSet<>();
        Set<String> after = new HashSet<>();
        findWithADynMemberships(anyObject.getType()).forEach(memb -> {
            boolean matches = anyMatchDAO.matches(
                    anyObject,
                    buildDynMembershipCond(memb.getFIQLCond(), memb.getGroup().getRealm()));
            if (matches) {
                after.add(memb.getGroup().getKey());
            }

            Query find = entityManager().createNativeQuery(
                    "SELECT any_id FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            find.setParameter(1, memb.getGroup().getKey());
            find.setParameter(2, anyObject.getKey());
            boolean existing = !find.getResultList().isEmpty();
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                Query insert = entityManager().createNativeQuery(
                        "INSERT INTO " + ADYNMEMB_TABLE + " VALUES(?, ?, ?)");
                insert.setParameter(1, anyObject.getType().getKey());
                insert.setParameter(2, anyObject.getKey());
                insert.setParameter(3, memb.getGroup().getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
                delete.setParameter(1, memb.getGroup().getKey());
                delete.setParameter(2, anyObject.getKey());
                delete.executeUpdate();
            }

            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final AnyObject anyObject) {
        List<Group> dynGroups = anyObjectDAO.findDynGroups(anyObject.getKey());

        Query delete = entityManager().createNativeQuery("DELETE FROM " + ADYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, anyObject.getKey());
        delete.executeUpdate();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group, AuthContextUtils.getDomain()));
        });

        return before;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findUDynMembers(final Group group) {
        if (group.getUDynMembership() == null) {
            return List.of();
        }

        Query query = entityManager().createNativeQuery(
                "SELECT any_id FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        List<String> result = new ArrayList<>();
        query.getResultList().stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).
                forEach(user -> result.add((String) user));
        return result;
    }

    @Override
    public void clearUDynMembers(final Group group) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    protected List<UDynGroupMembership> findWithUDynMemberships() {
        TypedQuery<UDynGroupMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUDynGroupMembership.class.getSimpleName() + " e",
                UDynGroupMembership.class);

        return query.getResultList();
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final User user) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.UDYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, user.getKey());

        Set<String> before = new HashSet<>();
        Set<String> after = new HashSet<>();
        findWithUDynMemberships().forEach(memb -> {
            boolean matches = anyMatchDAO.matches(
                    user,
                    buildDynMembershipCond(memb.getFIQLCond(), memb.getGroup().getRealm()));
            if (matches) {
                after.add(memb.getGroup().getKey());
            }

            Query find = entityManager().createNativeQuery(
                    "SELECT any_id FROM " + UDYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            find.setParameter(1, memb.getGroup().getKey());
            find.setParameter(2, user.getKey());
            boolean existing = !find.getResultList().isEmpty();
            if (existing) {
                before.add(memb.getGroup().getKey());
            }

            if (matches && !existing) {
                Query insert = entityManager().createNativeQuery(
                        "INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, memb.getGroup().getKey());
                insert.executeUpdate();
            } else if (!matches && existing) {
                Query delete = entityManager().createNativeQuery(
                        "DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
                delete.setParameter(1, memb.getGroup().getKey());
                delete.setParameter(2, user.getKey());
                delete.executeUpdate();
            }

            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, memb.getGroup(), AuthContextUtils.getDomain()));
        });

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final User user) {
        List<Group> dynGroups = userDAO.findDynGroups(user.getKey());

        Query delete = entityManager().createNativeQuery("DELETE FROM " + UDYNMEMB_TABLE + " WHERE any_id=?");
        delete.setParameter(1, user.getKey());
        delete.executeUpdate();

        Set<String> before = new HashSet<>();
        dynGroups.forEach(group -> {
            before.add(group.getKey());

            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group, AuthContextUtils.getDomain()));
        });

        return before;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return find(key).getResources().stream().map(Entity::getKey).collect(Collectors.toList());
    }
}
