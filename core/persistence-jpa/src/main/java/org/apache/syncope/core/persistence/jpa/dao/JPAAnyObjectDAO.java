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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.misc.EntitlementsHolder;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAAnyObjectDAO extends AbstractAnyDAO<AnyObject> implements AnyObjectDAO {

    @Autowired
    private GroupDAO groupDAO;

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.ANY_OBJECT);
    }

    @Override
    protected void securityChecks(final AnyObject anyObject) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(EntitlementsHolder.getInstance().
                getFor(anyObject.getType().getKey(), AnyEntitlement.READ));
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
            group.getADynMembership(any.getType()).remove(any);
        }

        entityManager().remove(any);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroupMemberships(final AnyObject anyObject) {
        TypedQuery<Group> query = entityManager().createQuery(
                "SELECT e.group FROM " + JPAADynGroupMembership.class.getSimpleName()
                + " e WHERE :anyObject MEMBER OF e.anyObjects", Group.class);
        query.setParameter("anyObject", anyObject);

        return query.getResultList();
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
    public Collection<Long> findAllGroupKeys(final AnyObject anyObject) {
        return CollectionUtils.collect(findAllGroups(anyObject), new Transformer<Group, Long>() {

            @Override
            public Long transform(final Group input) {
                return input.getKey();
            }
        });
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
    public Collection<String> findAllResourceNames(final AnyObject anyObject) {
        return CollectionUtils.collect(findAllResources(anyObject), new Transformer<ExternalResource, String>() {

            @Override
            public String transform(final ExternalResource input) {
                return input.getKey();
            }
        });
    }

}
