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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
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
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.event.AnyDeletedEvent;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGroupDAO extends AbstractAnyDAO<Group> implements GroupDAO {

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    private UserDAO userDAO;

    private AnyObjectDAO anyObjectDAO;

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
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_READ);
        boolean authorized = IterableUtils.matchesAny(authRealms, new Predicate<String>() {

            @Override
            public boolean evaluate(final String realm) {
                return group.getRealm().getFullPath().startsWith(realm)
                        || realm.equals(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
            }
        });
        if (authRealms == null || authRealms.isEmpty() || !authorized) {
            throw new DelegatedAdministrationException(AnyTypeKind.GROUP, group.getKey());
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

    @Override
    public Group authFindByName(final String name) {
        if (name == null) {
            throw new NotFoundException("Null name");
        }

        Group group = findByName(name);
        if (group == null) {
            throw new NotFoundException("Group " + name);
        }

        securityChecks(group);

        return group;
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
                "SELECT e FROM  " + JPAGroup.class.getSimpleName() + " e", Group.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    private SearchCond buildDynMembershipCond(final String baseCondFIQL, final Realm groupRealm) {
        AssignableCond cond = new AssignableCond();
        cond.setRealmFullPath(groupRealm.getFullPath());
        cond.setFromGroup(false);

        return SearchCond.getAndCond(SearchCond.getLeafCond(cond), SearchCondConverter.convert(baseCondFIQL));
    }

    @Override
    public Group save(final Group group) {
        Group merged = super.save(group);
        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged));

        // refresh dynaminc memberships
        if (merged.getUDynMembership() != null) {
            List<User> matching = searchDAO().search(
                    buildDynMembershipCond(merged.getUDynMembership().getFIQLCond(), merged.getRealm()),
                    AnyTypeKind.USER);

            merged.getUDynMembership().clear();
            for (User user : matching) {
                merged.getUDynMembership().add(user);
                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, user));
            }
        }
        for (ADynGroupMembership memb : merged.getADynMemberships()) {
            List<AnyObject> matching = searchDAO().search(
                    buildDynMembershipCond(memb.getFIQLCond(), merged.getRealm()),
                    AnyTypeKind.ANY_OBJECT);

            memb.clear();
            for (AnyObject anyObject : matching) {
                memb.add(anyObject);
                publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, anyObject));
            }
        }

        return merged;
    }

    @Override
    public void delete(final Group group) {
        for (AMembership membership : findAMemberships(group)) {
            AnyObject leftEnd = membership.getLeftEnd();
            leftEnd.getMemberships().remove(membership);
            membership.setRightEnd(null);
            for (APlainAttr attr : leftEnd.getPlainAttrs(membership)) {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);
                plainAttrDAO.delete(attr);
            }

            anyObjectDAO().save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd));
        }
        for (UMembership membership : findUMemberships(group)) {
            User leftEnd = membership.getLeftEnd();
            leftEnd.getMemberships().remove(membership);
            membership.setRightEnd(null);
            for (UPlainAttr attr : leftEnd.getPlainAttrs(membership)) {
                leftEnd.remove(attr);
                attr.setOwner(null);
                attr.setMembership(null);
                plainAttrDAO.delete(attr);
            }

            userDAO().save(leftEnd);
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, leftEnd));
        }

        entityManager().remove(group);
        publisher.publishEvent(new AnyDeletedEvent(this, AnyTypeKind.GROUP, group.getKey()));
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
    public List<String> findADynMembersKeys(final Group group) {
        List<String> result = new ArrayList<>();
        for (ADynGroupMembership memb : group.getADynMemberships()) {
            Query query = entityManager().createNativeQuery(
                    "SELECT t.anyObject_id FROM " + JPAADynGroupMembership.JOIN_TABLE + " t "
                    + "WHERE t.aDynGroupMembership_id=?");
            query.setParameter(1, memb.getKey());

            for (Object key : query.getResultList()) {
                String actualKey = key instanceof Object[]
                        ? (String) ((Object[]) key)[0]
                        : ((String) key);

                result.add(actualKey);
            }
        }
        return result;
    }

    private List<Group> findWithADynMemberships(final int page, final int itemsPerPage) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM  " + JPAGroup.class.getSimpleName() + " e WHERE e.aDynMemberships IS NOT EMPTY",
                Group.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final AnyObject anyObject) {
        Query countQuery = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAGroup.class.getSimpleName() + " e WHERE e.aDynMemberships IS NOT EMPTY");
        int count = ((Number) countQuery.getSingleResult()).intValue();

        for (int page = 1; page <= (count / DEFAULT_PAGE_SIZE) + 1; page++) {
            for (Group group : findWithADynMemberships(page, DEFAULT_PAGE_SIZE)) {
                if (!group.getADynMemberships().isEmpty()) {
                    for (ADynGroupMembership memb : group.getADynMemberships()) {
                        if (jpaAnySearchDAO().matches(
                                anyObject,
                                buildDynMembershipCond(memb.getFIQLCond(), group.getRealm()))) {

                            memb.add(anyObject);
                        } else {
                            Query query = entityManager().createNativeQuery(
                                    "DELETE FROM " + JPAADynGroupMembership.JOIN_TABLE + " t "
                                    + "WHERE t.anyObject_id=? and t.aDynGroupMembership_id=?");
                            query.setParameter(1, anyObject.getKey());
                            query.setParameter(2, memb.getKey());
                            query.executeUpdate();
                        }

                        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group));
                    }
                }
            }
        }
    }

    @Override
    public void removeDynMemberships(final AnyObject anyObject) {
        List<Group> dynGroups = anyObjectDAO().findDynGroups(anyObject);

        Query query = entityManager().createNativeQuery(
                "DELETE FROM " + JPAADynGroupMembership.JOIN_TABLE + " t WHERE t.anyObject_id=?");
        query.setParameter(1, anyObject.getKey());
        query.executeUpdate();

        for (Group group : dynGroups) {
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group));
        }
    }

    @Override
    public List<String> findUDynMembersKeys(final Group group) {
        if (group.getUDynMembership() == null) {
            return Collections.emptyList();
        }

        Query query = entityManager().createNativeQuery(
                "SELECT t.user_id FROM " + JPAUDynGroupMembership.JOIN_TABLE + " t "
                + "WHERE t.uDynGroupMembership_id=?");
        query.setParameter(1, group.getUDynMembership().getKey());

        List<String> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            result.add(actualKey);
        }
        return result;
    }

    private List<Group> findWithUDynMemberships(final int page, final int itemsPerPage) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e FROM  " + JPAGroup.class.getSimpleName() + " e WHERE e.uDynMembership IS NOT NULL",
                Group.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        Query countQuery = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAGroup.class.getSimpleName() + " e WHERE e.uDynMembership IS NOT NULL");
        int count = ((Number) countQuery.getSingleResult()).intValue();

        for (int page = 1; page <= (count / DEFAULT_PAGE_SIZE) + 1; page++) {
            for (Group group : findWithUDynMemberships(page, DEFAULT_PAGE_SIZE)) {
                if (group.getUDynMembership() != null) {
                    if (jpaAnySearchDAO().matches(
                            user,
                            buildDynMembershipCond(group.getUDynMembership().getFIQLCond(), group.getRealm()))) {

                        group.getUDynMembership().add(user);
                    } else {
                        Query query = entityManager().createNativeQuery(
                                "DELETE FROM " + JPAUDynGroupMembership.JOIN_TABLE + " t "
                                + "WHERE t.user_id=? and t.uDynGroupMembership_id=?");
                        query.setParameter(1, user.getKey());
                        query.setParameter(2, group.getUDynMembership().getKey());
                        query.executeUpdate();
                    }

                    publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group));
                }
            }
        }
    }

    @Override
    public void removeDynMemberships(final User user) {
        List<Group> dynGroups = userDAO().findDynGroups(user);

        Query query = entityManager().createNativeQuery(
                "DELETE FROM " + JPAUDynGroupMembership.JOIN_TABLE + " t WHERE t.user_id=?");
        query.setParameter(1, user.getKey());
        query.executeUpdate();

        for (Group group : dynGroups) {
            publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, group));
        }
    }

}
