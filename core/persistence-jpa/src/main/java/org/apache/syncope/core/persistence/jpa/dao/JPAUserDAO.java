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

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALinkedAccount;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class JPAUserDAO extends AbstractAnyDAO<User> implements UserDAO {

    protected final RoleDAO roleDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final GroupDAO groupDAO;

    protected final DelegationDAO delegationDAO;

    protected final FIQLQueryDAO fiqlQueryDAO;

    protected final SecurityProperties securityProperties;

    public JPAUserDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final SecurityProperties securityProperties) {

        super(anyUtilsFactory, plainSchemaDAO, derSchemaDAO, dynRealmDAO);
        this.roleDAO = roleDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.groupDAO = groupDAO;
        this.delegationDAO = delegationDAO;
        this.fiqlQueryDAO = fiqlQueryDAO;
        this.securityProperties = securityProperties;
    }

    @Override
    protected AnyUtils init() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Transactional(readOnly = true)
    @Override
    public String findKey(final String username) {
        return findKey(username, JPAUser.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public OffsetDateTime findLastChange(final String key) {
        return findLastChange(key, JPAUser.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> findUsername(final String key) {
        Query query = entityManager().createNativeQuery("SELECT username FROM " + JPAUser.TABLE + " WHERE id=?");
        query.setParameter(1, key);

        String username = null;
        for (Object resultKey : query.getResultList()) {
            username = resultKey instanceof Object[]
                    ? (String) ((Object[]) resultKey)[0]
                    : ((String) resultKey);
        }

        return Optional.ofNullable(username);
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

    @Override
    public Map<String, Integer> countByStatus() {
        Query query = entityManager().createQuery(
                "SELECT e.status, COUNT(e) FROM  " + anyUtils().anyClass().getSimpleName() + " e GROUP BY e.status");

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results.stream().collect(Collectors.toMap(
                result -> (String) result[0],
                result -> ((Number) result[1]).intValue()));
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm,
            final Collection<String> groups) {

        // 1. check if AuthContextUtils.getUsername() is owner of at least one group of which user is member
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.parseGroupOwnerRealm(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(pair -> groups.contains(pair.getRight()));

        // 2. check if user is in at least one DynRealm for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = findDynRealms(key).stream().anyMatch(authRealms::contains);
        }

        // 3. check if user is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
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
    public User findByUsername(final String username) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName()
                + " e WHERE e.username = :username", User.class);
        query.setParameter("username", username);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with username {}", username, e);
        }

        return result;
    }

    @Override
    public User findByToken(final String token) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName()
                + " e WHERE e.token LIKE :token", User.class);
        query.setParameter("token", token);

        User result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with token {}", token, e);
        }

        return result;
    }

    @Override
    public List<User> findBySecurityQuestion(final SecurityQuestion securityQuestion) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM " + anyUtils().anyClass().getSimpleName()
                + " e WHERE e.securityQuestion = :securityQuestion", User.class);
        query.setParameter("securityQuestion", securityQuestion);

        return query.getResultList();
    }

    @Override
    public UMembership findMembership(final String key) {
        return entityManager().find(JPAUMembership.class, key);
    }

    @Override
    public List<User> findAll(final int page, final int itemsPerPage) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM  " + anyUtils().anyClass().getSimpleName() + " e ORDER BY e.id", User.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    @Override
    public List<String> findAllKeys(final int page, final int itemsPerPage) {
        return findAllKeys(JPAUser.TABLE, page, itemsPerPage);
    }

    protected Pair<User, Pair<Set<String>, Set<String>>> doSave(final User user) {
        User merged = super.save(user);
        roleDAO.refreshDynMemberships(merged);
        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    public User save(final User user) {
        return doSave(user).getLeft();
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final User user) {
        return doSave(user).getRight();
    }

    @Override
    public void delete(final User user) {
        roleDAO.removeDynMemberships(user.getKey());
        groupDAO.removeDynMemberships(user);
        dynRealmDAO.removeDynMemberships(user.getKey());

        delegationDAO.findByDelegating(user).forEach(delegationDAO::delete);
        delegationDAO.findByDelegated(user).forEach(delegationDAO::delete);

        fiqlQueryDAO.findByOwner(user, null).forEach(fiqlQueryDAO::delete);

        Optional.ofNullable(accessTokenDAO.findByOwner(user.getUsername())).ifPresent(accessTokenDAO::delete);

        entityManager().remove(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Role> findAllRoles(final User user) {
        Set<Role> result = new HashSet<>();
        result.addAll(user.getRoles());
        result.addAll(findDynRoles(user.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public List<Role> findDynRoles(final String key) {
        Query query = entityManager().createNativeQuery(
                "SELECT role_id FROM " + JPARoleDAO.DYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        List<Role> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(roleKey -> {
                    Role role = roleDAO.find(roleKey.toString());
                    if (role == null) {
                        LOG.error("Could not find role {}, even though returned by the native query", roleKey);
                    } else if (!result.contains(role)) {
                        result.add(role);
                    }
                });
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public List<Group> findDynGroups(final String key) {
        Query query = entityManager().createNativeQuery(
                "SELECT group_id FROM " + JPAGroupDAO.UDYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        List<Group> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(groupKey -> {
                    Group group = groupDAO.find(groupKey.toString());
                    if (group == null) {
                        LOG.error("Could not find group {}, even though returned by the native query", groupKey);
                    } else if (!result.contains(group)) {
                        result.add(group);
                    }
                });
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final User user) {
        Set<Group> result = new HashSet<>();
        result.addAll(user.getMemberships().stream().
                map(Membership::getRightEnd).collect(Collectors.toSet()));
        result.addAll(findDynGroups(user.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final User user) {
        return findAllGroups(user).stream().map(Group::getKey).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupNames(final User user) {
        return findAllGroups(user).stream().map(Group::getName).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
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
        return findAllResources(authFind(key)).stream().map(ExternalResource::getKey).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public boolean linkedAccountExists(final String userKey, final String connObjectKeyValue) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + JPALinkedAccount.TABLE + " WHERE owner_id=? AND connObjectKeyValue=?");
        query.setParameter(1, userKey);
        query.setParameter(2, connObjectKeyValue);

        return !query.getResultList().isEmpty();
    }

    @Override
    public Optional<? extends LinkedAccount> findLinkedAccount(
            final ExternalResource resource, final String connObjectKeyValue) {

        TypedQuery<LinkedAccount> query = entityManager().createQuery(
                "SELECT e FROM " + JPALinkedAccount.class.getSimpleName() + " e "
                + "WHERE e.resource=:resource AND e.connObjectKeyValue=:connObjectKeyValue", LinkedAccount.class);
        query.setParameter("resource", resource);
        query.setParameter("connObjectKeyValue", connObjectKeyValue);

        List<LinkedAccount> result = query.getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Transactional(readOnly = true)
    @Override
    public List<LinkedAccount> findLinkedAccounts(final String userKey) {
        TypedQuery<LinkedAccount> query = entityManager().createQuery(
                "SELECT e FROM " + JPALinkedAccount.class.getSimpleName() + " e "
                + "WHERE e.owner.id=:userKey", LinkedAccount.class);
        query.setParameter("userKey", userKey);
        return query.getResultList();
    }

    @Override
    public List<LinkedAccount> findLinkedAccountsByPrivilege(final Privilege privilege) {
        TypedQuery<LinkedAccount> query = entityManager().createQuery(
                "SELECT e FROM " + JPALinkedAccount.class.getSimpleName() + " e "
                + "WHERE :privilege MEMBER OF e.privileges", LinkedAccount.class);
        query.setParameter("privilege", privilege);
        return query.getResultList();
    }

    @Override
    public List<LinkedAccount> findLinkedAccountsByResource(final ExternalResource resource) {
        TypedQuery<LinkedAccount> query = entityManager().createQuery(
                "SELECT e FROM " + JPALinkedAccount.class.getSimpleName() + " e "
                + "WHERE e.resource=:resource", LinkedAccount.class);
        query.setParameter("resource", resource);

        return query.getResultList();
    }
}
