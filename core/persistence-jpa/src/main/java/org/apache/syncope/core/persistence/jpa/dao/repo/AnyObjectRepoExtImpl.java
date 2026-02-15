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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.transaction.annotation.Transactional;

public class AnyObjectRepoExtImpl extends AbstractAnyRepoExt<AnyObject> implements AnyObjectRepoExt {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    public AnyObjectRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final EntityManager entityManager,
            final AnyChecker anyChecker,
            final AnyFinder anyFinder) {

        super(
                entityManager,
                anyChecker,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT));
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
    }

    @Override
    public Map<String, Long> countByType() {
        return query(
                "SELECT e.type_id, COUNT(e.id) "
                + "FROM " + JPAAnyObject.TABLE + " e "
                + "GROUP BY e.type_id",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getLong(2));
                    }
                    return result;
                });

    }

    @Override
    public Map<String, Long> countByRealm(final String anyType) {
        return query(
                "SELECT r.fullPath, COUNT(e.id) "
                + "FROM " + JPAAnyObject.TABLE + " e JOIN Realm r ON e.realm_id=r.id "
                + "WHERE e.type_id=? "
                + "GROUP BY r.fullPath",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getLong(2));
                    }
                    return result;
                },
                anyType);
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm,
            final Collection<String> groups) {

        // 0. check if AuthContextUtils.getUsername() is manager of the given anyObject
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.ManagerRealm.of(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(managerRealm -> key.equals(managerRealm.anyKey()));

        // 1. check if AuthContextUtils.getUsername() is manager of at least one group of which anyObject is member
        if (!authorized) {
            authorized = authRealms.stream().
                    map(authRealm -> RealmUtils.ManagerRealm.of(authRealm).orElse(null)).
                    filter(Objects::nonNull).
                    anyMatch(managerRealm -> groups.contains(managerRealm.anyKey()));
        }

        // 2. check if anyObject is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = authRealms.stream().anyMatch(realm::startsWith);
        }

        if (!authorized) {
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
    public void deleteMembership(final AMembership membership) {
        entityManager.remove(membership);
    }

    @Override
    public List<Relationship<Any, AnyObject>> findAllRelationships(final AnyObject anyObject) {
        List<Relationship<Any, AnyObject>> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        TypedQuery<Relationship<Any, AnyObject>> aquery =
                (TypedQuery<Relationship<Any, AnyObject>>) entityManager.createQuery(
                        "SELECT e FROM " + JPAARelationship.class.getSimpleName()
                        + " e WHERE e.rightEnd=:anyObject OR e.leftEnd=:anyObject");
        aquery.setParameter("anyObject", anyObject);
        result.addAll(aquery.getResultList());

        @SuppressWarnings("unchecked")
        TypedQuery<Relationship<Any, AnyObject>> uquery =
                (TypedQuery<Relationship<Any, AnyObject>>) entityManager.createQuery(
                        "SELECT e FROM " + JPAURelationship.class.getSimpleName()
                        + " e WHERE e.rightEnd=:anyObject");
        uquery.setParameter("anyObject", anyObject);
        result.addAll(uquery.getResultList());

        return result;
    }

    @Override
    public <S extends AnyObject> S save(final S anyObject) {
        anyChecker.checkBeforeSave(anyObject, anyUtils);
        return entityManager.merge(anyObject);
    }

    protected List<ARelationship> findARelationships(final AnyObject anyObject) {
        TypedQuery<ARelationship> query = entityManager.createQuery(
                "SELECT e FROM " + JPAARelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", ARelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    protected List<URelationship> findURelationships(final AnyObject anyObject) {
        TypedQuery<URelationship> query = entityManager.createQuery(
                "SELECT e FROM " + JPAURelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", URelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final AnyObject anyObject) {
        return anyObject.getMemberships().stream().map(AMembership::getRightEnd).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final AnyObject anyObject) {
        Set<String> result = new HashSet<>();
        result.addAll(anyObject.getMemberships().stream().map(m -> m.getRightEnd().getKey()).toList());

        return result;
    }

    @Transactional(readOnly = true)
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
        findARelationships(anyObject).forEach(relationship -> {
            relationship.getLeftEnd().remove(relationship);
            save(relationship.getLeftEnd());

            entityManager.remove(relationship);
        });
        findURelationships(anyObject).forEach(relationship -> {
            relationship.getLeftEnd().remove(relationship);
            userDAO.save(relationship.getLeftEnd());

            entityManager.remove(relationship);
        });

        entityManager.remove(anyObject);
    }
}
