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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALinkedAccount;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.transaction.annotation.Transactional;

public class UserRepoExtImpl extends AbstractAnyRepoExt<User> implements UserRepoExt {

    protected final RoleDAO roleDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final GroupDAO groupDAO;

    protected final DelegationDAO delegationDAO;

    protected final FIQLQueryDAO fiqlQueryDAO;

    protected final SecurityProperties securityProperties;

    public UserRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final SecurityProperties securityProperties,
            final EntityManager entityManager,
            final AnyChecker anyChecker,
            final AnyFinder anyFinder) {

        super(
                entityManager,
                anyChecker,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.USER));
        this.roleDAO = roleDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.groupDAO = groupDAO;
        this.delegationDAO = delegationDAO;
        this.fiqlQueryDAO = fiqlQueryDAO;
        this.securityProperties = securityProperties;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> findByToken(final String token) {
        return query(
                "SELECT id FROM " + JPAUser.TABLE + " WHERE token LIKE ?",
                rs -> {
                    if (rs.next()) {
                        return Optional.of(rs.getString(1));
                    }
                    return Optional.empty();
                },
                token);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isManager(final String key) {
        long users = query(
                "SELECT COUNT(*) FROM " + JPAUser.TABLE + " WHERE uManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        long groups = query(
                "SELECT COUNT(*) FROM " + JPAGroup.TABLE + " WHERE uManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        long anyObjects = query(
                "SELECT COUNT(*) FROM " + JPAAnyObject.TABLE + " WHERE uManager_id=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                key);

        return users + groups + anyObjects > 0;
    }

    @Override
    public List<User> findManagedUsers(final String key) {
        List<User> result = new ArrayList<>();

        // 1. users having uManager set to the requested user
        TypedQuery<User> users = entityManager.createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE e.uManager.id=:key", User.class);
        users.setParameter("key", key);
        result.addAll(users.getResultList());

        // 2. user members of groups having uManager set to the requested user
        findManagedGroupKeys(key).forEach(group -> groupDAO.findUMembers(group).
                forEach(member -> Optional.ofNullable(entityManager.find(JPAUser.class, member)).
                ifPresent(result::add)));

        // 3. users managed by groups the requested user is member of
        Query query = entityManager.createNativeQuery(
                "SELECT DISTINCT group_id FROM " + JPAUMembership.TABLE + " WHERE user_id=?");
        query.setParameter(1, key);
        @SuppressWarnings("unchecked")
        List<Object> groups = query.getResultList();
        groups.stream().map(String.class::cast).forEach(group -> result.addAll(groupDAO.findManagedUsers(group)));

        return result.stream().distinct().toList();
    }

    protected List<String> findManagedGroupKeys(final String key) {
        Query query = entityManager.createNativeQuery(
                "SELECT DISTINCT id FROM " + JPAGroup.TABLE + " WHERE uManager_id=?");
        query.setParameter(1, key);

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.stream().map(String.class::cast).toList();
    }

    @Override
    public List<Group> findManagedGroups(final String key) {
        return findManagedGroupKeys(key).stream().
                map(group -> Optional.ofNullable(entityManager.find(JPAGroup.class, group))).
                flatMap(Optional::stream).
                map(Group.class::cast).
                toList();
    }

    @Override
    public List<AnyObject> findManagedAnyObjects(final String key) {
        List<AnyObject> result = new ArrayList<>();

        // 1. anyObjects having uManager set to the requested user
        TypedQuery<AnyObject> anyObjects = entityManager.createQuery(
                "SELECT e FROM " + JPAAnyObject.class.getSimpleName() + " e WHERE e.uManager.id=:key", AnyObject.class);
        anyObjects.setParameter("key", key);
        result.addAll(anyObjects.getResultList());

        // 2. anyObject members of groups having uManager set to the requested user
        findManagedGroupKeys(key).forEach(group -> groupDAO.findUMembers(group).
                forEach(member -> Optional.ofNullable(entityManager.find(JPAAnyObject.class, member)).
                ifPresent(result::add)));

        // 3. anyObject managed by groups the requested user is member of
        Query query = entityManager.createNativeQuery(
                "SELECT DISTINCT group_id FROM " + JPAUMembership.TABLE + " WHERE user_id=?");
        query.setParameter(1, key);
        @SuppressWarnings("unchecked")
        List<Object> groups = query.getResultList();
        groups.stream().map(String.class::cast).forEach(group -> result.addAll(groupDAO.findManagedAnyObjects(group)));

        return result.stream().distinct().toList();
    }

    @Override
    public Map<String, Long> countByRealm() {
        return query(
                "SELECT r.fullPath, COUNT(e.id) "
                + "FROM " + JPAUser.TABLE + " e JOIN Realm r ON e.realm_id=r.id "
                + "GROUP BY r.fullPath",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getLong(2));
                    }
                    return result;
                });
    }

    @Override
    public Map<String, Long> countByStatus() {
        return query(
                "SELECT e.status, COUNT(e.id) "
                + "FROM " + JPAUser.TABLE + " e "
                + "GROUP BY e.status",
                rs -> {
                    Map<String, Long> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getLong(2));
                    }
                    return result;
                });
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm,
            final Collection<String> groups) {

        // 0. check if AuthContextUtils.getUsername() is manager of the given user
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.ManagerRealm.of(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(managerRealm -> key.equals(managerRealm.anyKey()));

        // 1. check if AuthContextUtils.getUsername() is manager of at least one group of which user is member
        if (!authorized) {
            authorized = authRealms.stream().
                    map(authRealm -> RealmUtils.ManagerRealm.of(authRealm).orElse(null)).
                    filter(Objects::nonNull).
                    anyMatch(managerRealm -> groups.contains(managerRealm.anyKey()));
        }

        // 2. check if user is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = authRealms.stream().anyMatch(realm::startsWith);
        }

        if (!authorized) {
            throw new DelegatedAdministrationException(realm, AnyTypeKind.USER.name(), key);
        }
    }

    @Override
    protected void securityChecks(final User user) {
        // Allows anonymous (during self-registration) and self (during self-update) to read own user,
        // otherwise goes through security checks to see if required entitlements are owned
        if (!AuthContextUtils.getUsername().equals(securityProperties.getAnonymousUser())
                && !AuthContextUtils.getUsername().equals(user.getUsername())) {

            Set<String> authRealms = AuthContextUtils.getAuthorizations().
                    getOrDefault(IdRepoEntitlement.USER_READ, Set.of());

            securityChecks(authRealms, user.getKey(), user.getRealm().getFullPath(), findAllGroupKeys(user));
        }
    }

    @Override
    public void deleteMembership(final UMembership membership) {
        entityManager.remove(membership);
    }

    @Override
    public <S extends User> S save(final S user) {
        S saved = entityManager.merge(user);

        anyChecker.checkBeforeSave(saved, anyUtils);
        saved.getLinkedAccounts().forEach(account -> anyChecker.checkBeforeSave(account, anyUtils));

        return saved;
    }

    @Override
    public void delete(final User user) {
        delegationDAO.findByDelegating(user).forEach(delegationDAO::delete);
        delegationDAO.findByDelegated(user).forEach(delegationDAO::delete);

        fiqlQueryDAO.findByOwner(user, null).forEach(fiqlQueryDAO::delete);

        accessTokenDAO.findByOwner(user.getUsername()).ifPresent(accessTokenDAO::delete);

        entityManager.remove(user);
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<Role> findAllRoles(final User user) {
        Set<Role> result = new HashSet<>();
        result.addAll(user.getRoles());

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final User user) {
        return user.getMemberships().stream().map(UMembership::getRightEnd).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final User user) {
        Set<String> result = new HashSet<>();
        result.addAll(user.getMemberships().stream().map(m -> m.getRightEnd().getKey()).toList());

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllGroupNames(final User user) {
        return findAllGroups(user).stream().map(Group::getName).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final User user) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(user.getResources());
        findAllGroups(user).forEach(group -> result.addAll(group.getResources()));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findAllResources(authFind(key)).stream().map(ExternalResource::getKey).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean linkedAccountExists(final String userKey, final String connObjectKeyValue) {
        return query(
                "SELECT COUNT(id) FROM " + JPALinkedAccount.TABLE + " WHERE owner_id=? AND connObjectKeyValue=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                userKey, connObjectKeyValue) > 0;
    }
}
