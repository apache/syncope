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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroupTypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
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
            final PlainSchemaDAO plainSchemaDAO,
            final RealmDAO realmDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        super(
                plainSchemaDAO,
                entityManager,
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

        // 1. check if AuthContextUtils.getUsername() is owner of the group, or
        // if group is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        boolean authorized = authRealms.stream().anyMatch(authRealm -> realm.startsWith(authRealm)
                || authRealm.equals(new RealmUtils.GroupOwnerRealm(realm, key).output()));

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
        checkBeforeSave((JPAGroup) group);
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
