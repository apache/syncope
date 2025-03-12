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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
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
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AnyObjectRepoExtImpl extends AbstractAnyRepoExt<AnyObject> implements AnyObjectRepoExt {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    public AnyObjectRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final DynRealmDAO dynRealmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        super(
                dynRealmDAO,
                plainSchemaDAO,
                entityManager,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT));
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
    }

    @Override
    public Map<AnyType, Long> countByType() {
        Query query = entityManager.createQuery(
                "SELECT e.type, COUNT(e) AS countByType FROM " + anyUtils.anyClass().getSimpleName() + " e "
                + "GROUP BY e.type ORDER BY countByType DESC");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<AnyType, Long> countByRealm = new LinkedHashMap<>(results.size());
        results.forEach(result -> countByRealm.put((AnyType) result[0], ((Number) result[1]).longValue()));

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    public Map<String, Long> countByRealm(final AnyType anyType) {
        Query query = entityManager.createQuery(
                "SELECT e.realm.fullPath, COUNT(e) FROM " + anyUtils.anyClass().getSimpleName() + " e "
                + "WHERE e.type=:type GROUP BY e.realm.fullPath");
        query.setParameter("type", anyType);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream().collect(Collectors.toMap(
                result -> result[0].toString(),
                result -> ((Number) result[1]).longValue()));
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
    public AMembership findMembership(final String key) {
        return entityManager.find(JPAAMembership.class, key);
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

    protected Pair<AnyObject, Pair<Set<String>, Set<String>>> doSave(final AnyObject anyObject) {
        AnyObject merged = entityManager.merge(anyObject);

        // ensure that entity listeners are invoked at this point
        entityManager.flush();

        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends AnyObject> S save(final S anyObject) {
        checkBeforeSave((JPAAnyObject) anyObject);
        return (S) doSave(anyObject).getLeft();
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final AnyObject anyObject) {
        checkBeforeSave((JPAAnyObject) anyObject);
        return doSave(anyObject).getRight();
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroups(final String key) {
        Query query = entityManager.createNativeQuery(
                "SELECT group_id FROM " + GroupRepoExt.ADYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.stream().
                map(groupKey -> groupDAO.findById(groupKey.toString())).
                flatMap(Optional::stream).
                distinct().
                collect(Collectors.toList());
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
            relationship.getLeftEnd().getRelationships().remove(relationship);
            save(relationship.getLeftEnd());

            entityManager.remove(relationship);
        });
        findURelationships(anyObject).forEach(relationship -> {
            relationship.getLeftEnd().getRelationships().remove(relationship);
            userDAO.save(relationship.getLeftEnd());

            entityManager.remove(relationship);
        });

        entityManager.remove(anyObject);
    }
}
