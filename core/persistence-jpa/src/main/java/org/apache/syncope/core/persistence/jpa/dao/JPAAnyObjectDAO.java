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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAAnyObjectDAO extends AbstractAnyDAO<AnyObject> implements AnyObjectDAO {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Override
    public Map<AnyType, Integer> countByType() {
        Query query = entityManager().createQuery(
                "SELECT e.type, COUNT(e) AS countByType FROM  " + JPAAnyObject.class.getSimpleName() + " e "
                + "GROUP BY e.type ORDER BY countByType DESC");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<AnyType, Integer> countByRealm = new LinkedHashMap<>(results.size());
        for (Object[] result : results) {
            countByRealm.put((AnyType) result[0], ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    public Map<String, Integer> countByRealm(final AnyType anyType) {
        Query query = entityManager().createQuery(
                "SELECT e.realm, COUNT(e) FROM  " + JPAAnyObject.class.getSimpleName() + " e "
                + "WHERE e.type=:type GROUP BY e.realm");
        query.setParameter("type", anyType);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Integer> countByRealm = new HashMap<>(results.size());
        for (Object[] result : results) {
            countByRealm.put(((Realm) result[0]).getFullPath(), ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.ANY_OBJECT);
    }

    @Override
    protected void securityChecks(final AnyObject anyObject) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(
                AnyEntitlement.READ.getFor(anyObject.getType().getKey()));
        boolean authorized = IterableUtils.matchesAny(authRealms, new Predicate<String>() {

            @Override
            public boolean evaluate(final String realm) {
                return anyObject.getRealm().getFullPath().startsWith(realm);
            }
        });
        if (authRealms == null || authRealms.isEmpty() || !authorized) {
            throw new DelegatedAdministrationException(AnyTypeKind.ANY_OBJECT, anyObject.getKey());
        }
    }

    @Override
    public AnyObject findByName(final String name) {
        TypedQuery<AnyObject> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAnyObject.class.getSimpleName() + " e WHERE e.name = :name", AnyObject.class);
        query.setParameter("name", name);

        AnyObject result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No any object found with name {}", name, e);
        }

        return result;
    }

    @Override
    public AnyObject authFindByName(final String name) {
        if (name == null) {
            throw new NotFoundException("Null name");
        }

        AnyObject anyObject = findByName(name);
        if (anyObject == null) {
            throw new NotFoundException("Any Object " + name);
        }

        securityChecks(anyObject);

        return anyObject;
    }

    @Override
    public List<ARelationship> findARelationships(final AnyObject anyObject) {
        TypedQuery<ARelationship> query = entityManager().createQuery(
                "SELECT e FROM " + JPAARelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", ARelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    @Override
    public List<URelationship> findURelationships(final AnyObject anyObject) {
        TypedQuery<URelationship> query = entityManager().createQuery(
                "SELECT e FROM " + JPAURelationship.class.getSimpleName()
                + " e WHERE e.rightEnd=:anyObject", URelationship.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
    }

    @Override
    public AnyObject save(final AnyObject anyObject) {
        AnyObject merged = super.save(anyObject);

        groupDAO.refreshDynMemberships(merged);

        return merged;
    }

    @Override
    public void delete(final AnyObject any) {
        for (Group group : findDynGroupMemberships(any)) {
            group.getADynMembership(any.getType()).getMembers().remove(any);
        }

        for (ARelationship relationship : findARelationships(any)) {
            relationship.getLeftEnd().getRelationships().remove(relationship);
            save(relationship.getLeftEnd());

            entityManager().remove(relationship);
        }
        for (URelationship relationship : findURelationships(any)) {
            relationship.getLeftEnd().getRelationships().remove(relationship);
            userDAO.save(relationship.getLeftEnd());

            entityManager().remove(relationship);
        }

        entityManager().remove(any);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroupMemberships(final AnyObject anyObject) {
        Query query = entityManager().createNativeQuery(
                "SELECT t2.id FROM " + JPAADynGroupMembership.TABLE + " t0 "
                + "INNER JOIN ADynGroupMembership_AnyObject t1 "
                + "ON t0.id = t1.aDynGroupMembership_id "
                + "LEFT OUTER JOIN " + JPAGroup.TABLE + " t2 "
                + "ON t0.GROUP_ID = t2.id "
                + "WHERE t1.anyObject_id = ?1");
        query.setParameter(1, anyObject.getKey());

        List<Group> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            Group group = groupDAO.find(actualKey);
            if (group == null) {
                LOG.error("Could not find group with id {}, even though returned by the native query", actualKey);
            } else if (!result.contains(group)) {
                result.add(group);
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final AnyObject anyObject) {
        return CollectionUtils.union(
                CollectionUtils.collect(anyObject.getMemberships(), new Transformer<AMembership, Group>() {

                    @Override
                    public Group transform(final AMembership input) {
                        return input.getRightEnd();
                    }
                }, new ArrayList<Group>()),
                findDynGroupMemberships(anyObject));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final AnyObject anyObject) {
        return CollectionUtils.collect(findAllGroups(anyObject), EntityUtils.<Group>keyTransformer());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final AnyObject anyObject) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(anyObject.getResources());
        for (Group group : findAllGroups(anyObject)) {
            result.addAll(group.getResources());
        }

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllResourceNames(final String key) {
        return CollectionUtils.collect(findAllResources(authFind(key)), EntityUtils.<ExternalResource>keyTransformer());
    }

}
