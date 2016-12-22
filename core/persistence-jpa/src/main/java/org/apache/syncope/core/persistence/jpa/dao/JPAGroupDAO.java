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

import java.util.Collections;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGroupDAO extends AbstractAnyDAO<Group> implements GroupDAO {

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.GROUP);
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
        User owner = userDAO.find(userKey);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAGroup.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (String groupKey : userDAO.findAllGroupKeys(owner)) {
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
                "SELECT e FROM " + JPAAMembership.class.getSimpleName()
                + " e WHERE e.rightEnd=:group", AMembership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @Override
    public List<UMembership> findUMemberships(final Group group) {
        TypedQuery<UMembership> query = entityManager().createQuery(
                "SELECT e FROM " + JPAUMembership.class.getSimpleName()
                + " e WHERE e.rightEnd=:group", UMembership.class);
        query.setParameter("group", group);

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

        // refresh dynaminc memberships
        if (merged.getUDynMembership() != null) {
            List<User> matching = searchDAO.search(
                    buildDynMembershipCond(merged.getUDynMembership().getFIQLCond(), merged.getRealm()),
                    AnyTypeKind.USER);

            merged.getUDynMembership().getMembers().clear();
            for (User user : matching) {
                merged.getUDynMembership().add(user);
            }
        }
        for (ADynGroupMembership memb : merged.getADynMemberships()) {
            List<AnyObject> matching = searchDAO.search(
                    buildDynMembershipCond(memb.getFIQLCond(), merged.getRealm()),
                    AnyTypeKind.ANY_OBJECT);

            memb.getMembers().clear();
            for (AnyObject anyObject : matching) {
                memb.add(anyObject);
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

            anyObjectDAO.save(leftEnd);
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

            userDAO.save(leftEnd);
        }

        entityManager().remove(group);
    }

    @Override
    public List<TypeExtension> findTypeExtensions(final AnyTypeClass anyTypeClass) {
        TypedQuery<TypeExtension> query = entityManager().createQuery(
                "SELECT e FROM " + JPATypeExtension.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", TypeExtension.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final AnyObject anyObject) {
        for (Group group : findAll()) {
            for (ADynGroupMembership memb : group.getADynMemberships()) {
                if (searchDAO.matches(
                        anyObject,
                        buildDynMembershipCond(memb.getFIQLCond(), group.getRealm()))) {

                    memb.add(anyObject);
                } else {
                    memb.getMembers().remove(anyObject);
                }
            }
        }
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final User user) {
        for (Group group : findAll()) {
            if (group.getUDynMembership() != null) {
                if (searchDAO.matches(
                        user,
                        buildDynMembershipCond(group.getUDynMembership().getFIQLCond(), group.getRealm()))) {

                    group.getUDynMembership().add(user);
                } else {
                    group.getUDynMembership().getMembers().remove(user);
                }
            }
        }
    }
}
