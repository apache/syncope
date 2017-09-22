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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGroupDAO extends AbstractAnyDAO<Group> implements GroupDAO {

    public static final String UDYNMEMB_TABLE = "UDynGroupMembers";

    public static final String ADYNMEMB_TABLE = "ADynGroupMembers";

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    private UserDAO userDAO;

    private AnyObjectDAO anyObjectDAO;

    private AnySearchDAO searchDAO;

    private AnySearchDAO jpaAnySearchDAO;

    private UserDAO userDAO() {
        synchronized (this) {
            if (userDAO == null) {
                userDAO = ApplicationContextProvider.getApplicationContext().getBean(UserDAO.class);
            }
        }
        return userDAO;
    }

    private AnyObjectDAO anyObjectDAO() {
        synchronized (this) {
            if (anyObjectDAO == null) {
                anyObjectDAO = ApplicationContextProvider.getApplicationContext().getBean(AnyObjectDAO.class);
            }
        }
        return anyObjectDAO;
    }

    private AnySearchDAO searchDAO() {
        synchronized (this) {
            if (searchDAO == null) {
                searchDAO = ApplicationContextProvider.getApplicationContext().getBean(AnySearchDAO.class);
            }
        }
        return searchDAO;
    }

    private AnySearchDAO jpaAnySearchDAO() {
        synchronized (this) {
            if (jpaAnySearchDAO == null) {
                if (AopUtils.getTargetClass(searchDAO()).equals(JPAAnySearchDAO.class)) {
                    jpaAnySearchDAO = searchDAO();
                } else {
                    jpaAnySearchDAO = (AnySearchDAO) ApplicationContextProvider.getBeanFactory().
                            createBean(JPAAnySearchDAO.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                }
            }
        }
        return jpaAnySearchDAO;
    }

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.GROUP);
    }

    @Transactional(readOnly = true)
    @Override
    public String findKey(final String username) {
        return findKey(username, JPAGroup.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public Date findLastChange(final String key) {
        return findLastChange(key, JPAGroup.TABLE);
    }

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAGroup.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public Map<String, Integer> countByRealm() {
        Query query = entityManager().createQuery(
                "SELECT e.realm, COUNT(e) FROM  " + JPAGroup.class.getSimpleName() + " e GROUP BY e.realm");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Integer> countByRealm = new HashMap<>(results.size());
        for (Object[] result : results) {
            countByRealm.put(((Realm) result[0]).getFullPath(), ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    protected void securityChecks(final Group group) {
        Map<String, Set<String>> authorizations = AuthContextUtils.getAuthorizations();
        Set<String> authRealms = authorizations.containsKey(StandardEntitlement.GROUP_READ)
                ? authorizations.get(StandardEntitlement.GROUP_READ)
                : Collections.emptySet();

        boolean authorized = authRealms.stream().anyMatch(realm -> group.getRealm().getFullPath().startsWith(realm)
                || realm.equals(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey())));
        if (!authorized) {
            authorized = findDynRealms(group.getKey()).stream().
                    filter(dynRealm -> authRealms.contains(dynRealm)).
                    count() > 0;
        }
        if (authRealms.isEmpty() || !authorized) {
            throw new DelegatedAdministrationException(
                    group.getRealm().getFullPath(), AnyTypeKind.GROUP.name(), group.getKey());
        }
    }

    @Override
    public Group findByName(final String name) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.name = :name", Group.class);
        query.setParameter("name", name);

        Group result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No group found with name {}", name, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final String userKey) {
        User owner = userDAO().find(userKey);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAGroup.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (String groupKey : userDAO().findAllGroupKeys(owner)) {
            queryString.append("OR e.groupOwner.id='").append(groupKey).append("' ");
        }

        TypedQuery<Group> query = entityManager().createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByGroup(final String groupKey) {
        Group owner = find(groupKey);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.groupOwner=:owner", Group.class);
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
                "SELECT e FROM  " + JPAGroup.class.getSimpleName() + " e ORDER BY e.id", Group.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    private SearchCond buildDynMembershipCond(final String baseCondFIQL, final Realm groupRealm) {
        AssignableCond cond = new AssignableCond();
        cond.setRealmFullPath(groupRealm.getFullPath());
        cond.setFromGroup(true);

        return SearchCond.getAndCond(SearchCond.getLeafCond(cond), SearchCondConverter.convert(baseCondFIQL));
    }

    @Override
    public Group save(final Group group) {
        Group merged = super.save(group);
        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));

        // refresh dynamic memberships
        if (merged.getUDynMembership() != null) {
            List<User> matching = searchDAO().search(
                    buildDynMembershipCond(merged.getUDynMembership().getFIQLCond(), merged.getRealm()),
                    AnyTypeKind.USER);

            clearUDynMembers(merged);

            matching.forEach(user -> {
                Query insert = entityManager().createNativeQuery("INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, merged.getKey());
                insert.executeUpdate();

                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user, AuthContextUtils.getDomain()));
            });
        }
        merged.getADynMemberships().stream().map(memb -> searchDAO().search(
                buildDynMembershipCond(memb.getFIQLCond(), merged.getRealm()),
                AnyTypeKind.ANY_OBJECT)).forEachOrdered(matching -> {
            clearADynMembers(merged);

            matching.forEach(anyObject -> {
                Query insert = entityManager().createNativeQuery("INSERT INTO " + ADYNMEMB_TABLE
                        + " VALUES(?, ?, ?)");
                insert.setParameter(1, anyObject.getType().getKey());
                insert.setParameter(2, anyObject.getKey());
                insert.setParameter(3, merged.getKey());
                insert.executeUpdate();

                publisher.publishEvent(
                        new AnyCreatedUpdatedEvent<>(this, anyObject, AuthContextUtils.getDomain()));
            });
        });

        dynRealmDAO().refreshDynMemberships(merged);

        return merged;
    }

    @Override
    public void delete(final Group group) {
        dynRealmDAO().removeDynMemberships(group.getKey());

        findAMemberships(group).forEach(membership -> {
            AnyObject leftEnd = membership.getLeftEnd();
            leftEnd.getMemberships().remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).stream().map(attr -> {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);
                return attr;
            }).forEachOrdered(attr -> plainAttrDAO.delete(attr));

            anyObjectDAO().save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd, AuthContextUtils.getDomain()));
        });

        findUMemberships(group).forEach(membership -> {
            User leftEnd = membership.getLeftEnd();
            leftEnd.getMemberships().remove(membership);
            membership.setRightEnd(null);
            leftEnd.getPlainAttrs(membership).stream().map(attr -> {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);
                return attr;
            }).forEachOrdered(attr -> plainAttrDAO.delete(attr));

            userDAO().save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd, AuthContextUtils.getDomain()));
        });

        clearUDynMembers(group);
        clearADynMembers(group);

        entityManager().remove(group);
        publisher.publishEvent(
                new AnyDeletedEvent(this, AnyTypeKind.GROUP, group.getKey(), AuthContextUtils.getDomain()));
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
        group.getADynMemberships().stream().map(memb -> {
            Query query = entityManager().createNativeQuery(
                    "SELECT any_id FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND anyType_id=?");
            query.setParameter(1, group.getKey());
            query.setParameter(2, memb.getAnyType().getKey());
            return query;
        }).forEachOrdered((query) -> {
            query.getResultList().stream().map(key -> key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key)).
                    forEachOrdered(actualKey -> result.add(actualKey.toString()));
        });

        return result;
    }

    @Override
    public void clearADynMembers(final Group group) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    private List<ADynGroupMembership> findWithADynMemberships(final AnyType anyType) {
        TypedQuery<ADynGroupMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAADynGroupMembership.class.getSimpleName() + " e  WHERE e.anyType=:anyType",
                ADynGroupMembership.class);
        query.setParameter("anyType", anyType);
        return query.getResultList();
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final AnyObject anyObject) {
        Query dynGroupsQuery = entityManager().createNativeQuery(
                "SELECT group_id FROM " + ADYNMEMB_TABLE + " WHERE any_id=?");
        dynGroupsQuery.setParameter(1, anyObject.getKey());
        @SuppressWarnings("unchecked")
        List<String> dynGroups = dynGroupsQuery.getResultList();

        Set<String> before = dynGroups.stream().collect(Collectors.toSet());

        Set<String> after = new HashSet<>();
        findWithADynMemberships(anyObject.getType()).stream().map(memb -> {
            Query delete = entityManager().createNativeQuery(
                    "DELETE FROM " + ADYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            delete.setParameter(1, memb.getGroup().getKey());
            delete.setParameter(2, anyObject.getKey());
            delete.executeUpdate();
            if (jpaAnySearchDAO().matches(
                    anyObject,
                    buildDynMembershipCond(memb.getFIQLCond(), memb.getGroup().getRealm()))) {

                Query insert = entityManager().createNativeQuery(
                        "INSERT INTO " + ADYNMEMB_TABLE + " VALUES(?, ?, ?)");
                insert.setParameter(1, anyObject.getType().getKey());
                insert.setParameter(2, anyObject.getKey());
                insert.setParameter(3, memb.getGroup().getKey());
                insert.executeUpdate();

                after.add(memb.getGroup().getKey());
            }
            return memb;
        }).forEachOrdered(memb -> publisher.publishEvent(
                new AnyCreatedUpdatedEvent<>(this, memb.getGroup(), AuthContextUtils.getDomain())));

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final AnyObject anyObject) {
        List<Group> dynGroups = anyObjectDAO().findDynGroups(anyObject.getKey());

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
            return Collections.emptyList();
        }

        Query query = entityManager().createNativeQuery(
                "SELECT any_id FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        query.setParameter(1, group.getKey());

        List<String> result = new ArrayList<>();
        query.getResultList().stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).forEachOrdered(actualKey -> result.add(actualKey.toString()));
        return result;
    }

    @Override
    public void clearUDynMembers(final Group group) {
        Query delete = entityManager().createNativeQuery("DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=?");
        delete.setParameter(1, group.getKey());
        delete.executeUpdate();
    }

    private List<UDynGroupMembership> findWithUDynMemberships() {
        TypedQuery<UDynGroupMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUDynGroupMembership.class.getSimpleName() + " e",
                UDynGroupMembership.class);

        return query.getResultList();
    }

    @Transactional
    @Override
    public Pair<Set<String>, Set<String>> refreshDynMemberships(final User user) {
        Query dynGroupsQuery = entityManager().createNativeQuery(
                "SELECT group_id FROM " + UDYNMEMB_TABLE + " WHERE any_id=?");
        dynGroupsQuery.setParameter(1, user.getKey());
        @SuppressWarnings("unchecked")
        List<String> dynGroups = dynGroupsQuery.getResultList();

        Set<String> before = dynGroups.stream().collect(Collectors.toSet());

        Set<String> after = new HashSet<>();
        findWithUDynMemberships().stream().map(memb -> {
            Query delete = entityManager().createNativeQuery(
                    "DELETE FROM " + UDYNMEMB_TABLE + " WHERE group_id=? AND any_id=?");
            delete.setParameter(1, memb.getGroup().getKey());
            delete.setParameter(2, user.getKey());
            delete.executeUpdate();
            if (jpaAnySearchDAO().matches(
                    user,
                    buildDynMembershipCond(memb.getFIQLCond(), memb.getGroup().getRealm()))) {

                Query insert = entityManager().createNativeQuery(
                        "INSERT INTO " + UDYNMEMB_TABLE + " VALUES(?, ?)");
                insert.setParameter(1, user.getKey());
                insert.setParameter(2, memb.getGroup().getKey());
                insert.executeUpdate();

                after.add(memb.getGroup().getKey());
            }
            return memb;
        }).forEachOrdered(memb -> publisher.publishEvent(
                new AnyCreatedUpdatedEvent<>(this, memb.getGroup(), AuthContextUtils.getDomain())));

        return Pair.of(before, after);
    }

    @Override
    public Set<String> removeDynMemberships(final User user) {
        List<Group> dynGroups = userDAO().findDynGroups(user.getKey());

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
