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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.provisioning.api.utils.policy.AccountPolicyException;
import org.apache.syncope.core.provisioning.api.utils.policy.PasswordPolicyException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAUserDAO extends AbstractAnyDAO<User> implements UserDAO {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^" + SyncopeConstants.NAME_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private ImplementationLookup implementationLookup;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    private RealmDAO realmDAO;

    private GroupDAO groupDAO;

    private RealmDAO realmDAO() {
        synchronized (this) {
            if (realmDAO == null) {
                realmDAO = ApplicationContextProvider.getApplicationContext().getBean(RealmDAO.class);
            }
        }
        return realmDAO;
    }

    private GroupDAO groupDAO() {
        synchronized (this) {
            if (groupDAO == null) {
                groupDAO = ApplicationContextProvider.getApplicationContext().getBean(GroupDAO.class);
            }
        }
        return groupDAO;
    }

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.USER);
    }

    @Transactional(readOnly = true)
    @Override
    public String findKey(final String username) {
        return findKey(username, JPAUser.TABLE);
    }

    @Transactional(readOnly = true)
    @Override
    public Date findLastChange(final String key) {
        return findLastChange(key, JPAUser.TABLE);
    }

    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAUser.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public Map<String, Integer> countByRealm() {
        Query query = entityManager().createQuery(
                "SELECT e.realm, COUNT(e) FROM  " + JPAUser.class.getSimpleName() + " e GROUP BY e.realm");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Integer> countByRealm = new HashMap<>(results.size());
        for (Object[] result : results) {
            countByRealm.put(((Realm) result[0]).getFullPath(), ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByRealm);
    }

    @Override
    public Map<String, Integer> countByStatus() {
        Query query = entityManager().createQuery(
                "SELECT e.status, COUNT(e) FROM  " + JPAUser.class.getSimpleName() + " e GROUP BY e.status");
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<String, Integer> countByStatus = new HashMap<>(results.size());
        for (Object[] result : results) {
            countByStatus.put(((String) result[0]), ((Number) result[1]).intValue());
        }

        return Collections.unmodifiableMap(countByStatus);
    }

    @Override
    protected void securityChecks(final User user) {
        // Allows anonymous (during self-registration) and self (during self-update) to read own user,
        // otherwise goes through security checks to see if required entitlements are owned
        if (!AuthContextUtils.getUsername().equals(anonymousUser)
                && !AuthContextUtils.getUsername().equals(user.getUsername())) {

            Map<String, Set<String>> authorizations = AuthContextUtils.getAuthorizations();
            Set<String> authRealms = authorizations.containsKey(StandardEntitlement.USER_READ)
                    ? authorizations.get(StandardEntitlement.USER_READ)
                    : Collections.emptySet();
            boolean authorized = authRealms.stream().
                    anyMatch(realm -> user.getRealm().getFullPath().startsWith(realm));
            if (!authorized) {
                authorized = findDynRealms(user.getKey()).stream().
                        filter(dynRealm -> authRealms.contains(dynRealm)).
                        count() > 0;
            }
            if (authRealms.isEmpty() || !authorized) {
                throw new DelegatedAdministrationException(
                        user.getRealm().getFullPath(), AnyTypeKind.USER.name(), user.getKey());
            }
        }
    }

    @Override
    public User findByUsername(final String username) {
        TypedQuery<User> query = entityManager().createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
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
        TypedQuery<User> query = entityManager().createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
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
        TypedQuery<User> query = entityManager().createQuery("SELECT e FROM " + JPAUser.class.getSimpleName()
                + " e WHERE e.securityQuestion = :securityQuestion", User.class);
        query.setParameter("securityQuestion", securityQuestion);

        return query.getResultList();
    }

    private List<PasswordPolicy> getPasswordPolicies(final User user) {
        List<PasswordPolicy> policies = new ArrayList<>();

        PasswordPolicy policy;

        // add resource policies
        for (ExternalResource resource : findAllResources(user)) {
            policy = resource.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add realm policies
        for (Realm realm : realmDAO().findAncestors(user.getRealm())) {
            policy = realm.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }

    @Override
    public List<User> findAll(final int page, final int itemsPerPage) {
        TypedQuery<User> query = entityManager().createQuery(
                "SELECT e FROM  " + JPAUser.class.getSimpleName() + " e ORDER BY e.id", User.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        return query.getResultList();
    }

    private List<AccountPolicy> getAccountPolicies(final User user) {
        List<AccountPolicy> policies = new ArrayList<>();

        // add resource policies
        findAllResources(user).stream().
                map(resource -> resource.getAccountPolicy()).
                filter(policy -> policy != null).
                forEachOrdered(policy -> policies.add(policy));

        // add realm policies
        realmDAO().findAncestors(user.getRealm()).stream().
                map(realm -> realm.getAccountPolicy()).
                filter(policy -> policy != null).
                forEachOrdered(policy -> policies.add(policy));

        return policies;
    }

    @Transactional(readOnly = true)
    @Override
    public Pair<Boolean, Boolean> enforcePolicies(final User user) {
        // ------------------------------
        // Verify password policies
        // ------------------------------
        LOG.debug("Password Policy enforcement");

        try {
            int maxPPSpecHistory = 0;
            for (PasswordPolicy policy : getPasswordPolicies(user)) {
                if (user.getPassword() == null && !policy.isAllowNullPassword()) {
                    throw new PasswordPolicyException("Password mandatory");
                }

                policy.getRuleConfs().forEach(ruleConf -> {
                    Class<? extends PasswordRule> ruleClass =
                            implementationLookup.getPasswordRuleClass(ruleConf.getClass());
                    if (ruleClass == null) {
                        LOG.warn("Could not find matching password rule for {}", ruleConf.getClass());
                    } else {
                        // fetch (or create) rule
                        PasswordRule rule;
                        if (ApplicationContextProvider.getBeanFactory().containsSingleton(ruleClass.getName())) {
                            rule = (PasswordRule) ApplicationContextProvider.getBeanFactory().
                                    getSingleton(ruleClass.getName());
                        } else {
                            rule = (PasswordRule) ApplicationContextProvider.getBeanFactory().
                                    createBean(ruleClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                            ApplicationContextProvider.getBeanFactory().
                                    registerSingleton(ruleClass.getName(), rule);
                        }

                        // enforce rule
                        rule.enforce(ruleConf, user);
                    }
                });

                if (user.verifyPasswordHistory(user.getClearPassword(), policy.getHistoryLength())) {
                    throw new PasswordPolicyException("Password value was used in the past: not allowed");
                }

                if (policy.getHistoryLength() > maxPPSpecHistory) {
                    maxPPSpecHistory = policy.getHistoryLength();
                }
            }

            // update user's password history with encrypted password
            if (maxPPSpecHistory > 0 && user.getPassword() != null
                    && !user.getPasswordHistory().contains(user.getPassword())) {
                user.getPasswordHistory().add(user.getPassword());
            }
            // keep only the last maxPPSpecHistory items in user's password history
            if (maxPPSpecHistory < user.getPasswordHistory().size()) {
                for (int i = 0; i < user.getPasswordHistory().size() - maxPPSpecHistory; i++) {
                    user.getPasswordHistory().remove(i);
                }
            }
        } catch (Exception e) {
            LOG.error("Invalid password for {}", user, e);
            throw new InvalidEntityException(User.class, EntityViolationType.InvalidPassword, e.getMessage());
        } finally {
            // password has been validated, let's remove its clear version
            user.removeClearPassword();
        }

        // ------------------------------
        // Verify account policies
        // ------------------------------
        LOG.debug("Account Policy enforcement");

        boolean suspend = false;
        boolean propagateSuspension = false;
        try {
            if (user.getUsername() == null) {
                throw new AccountPolicyException("Null username");
            }

            if (adminUser.equals(user.getUsername()) || anonymousUser.equals(user.getUsername())) {
                throw new AccountPolicyException("Not allowed: " + user.getUsername());
            }

            if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
                throw new AccountPolicyException("Character(s) not allowed");
            }

            for (AccountPolicy policy : getAccountPolicies(user)) {
                policy.getRuleConfs().forEach(ruleConf -> {
                    Class<? extends AccountRule> ruleClass =
                            implementationLookup.getAccountRuleClass(ruleConf.getClass());
                    if (ruleClass == null) {
                        LOG.warn("Could not find matching account rule for {}", ruleConf.getClass());
                    } else {
                        // fetch (or create) rule
                        AccountRule rule;
                        if (ApplicationContextProvider.getBeanFactory().containsSingleton(ruleClass.getName())) {
                            rule = (AccountRule) ApplicationContextProvider.getBeanFactory().
                                    getSingleton(ruleClass.getName());
                        } else {
                            rule = (AccountRule) ApplicationContextProvider.getBeanFactory().
                                    createBean(ruleClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                            ApplicationContextProvider.getBeanFactory().
                                    registerSingleton(ruleClass.getName(), rule);
                        }

                        // enforce rule
                        rule.enforce(ruleConf, user);
                    }
                });

                suspend |= user.getFailedLogins() != null && policy.getMaxAuthenticationAttempts() > 0
                        && user.getFailedLogins() > policy.getMaxAuthenticationAttempts() && !user.isSuspended();
                propagateSuspension |= policy.isPropagateSuspension();
            }
        } catch (Exception e) {
            LOG.error("Invalid username for {}", user, e);
            throw new InvalidEntityException(User.class, EntityViolationType.InvalidUsername, e.getMessage());
        }

        return ImmutablePair.of(suspend, propagateSuspension);
    }

    private Pair<User, Pair<Set<String>, Set<String>>> doSave(final User user) {
        // 1. save clear password value before save
        String clearPwd = user.getClearPassword();

        // 2. save and flush to trigger entity validation        
        User merged = super.save(user);
        entityManager().flush();

        // 3. set back the sole clear password value
        JPAUser.class.cast(merged).setClearPassword(clearPwd);

        // 4. enforce password and account policies
        try {
            enforcePolicies(merged);
        } catch (InvalidEntityException e) {
            entityManager().remove(merged);
            throw e;
        }

        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));

        roleDAO.refreshDynMemberships(merged);
        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO().refreshDynMemberships(merged);
        dynRealmDAO().refreshDynMemberships(merged);

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
        groupDAO().removeDynMemberships(user);
        dynRealmDAO().removeDynMemberships(user.getKey());

        AccessToken accessToken = accessTokenDAO.findByOwner(user.getUsername());
        if (accessToken != null) {
            accessTokenDAO.delete(accessToken);
        }

        entityManager().remove(user);
        publisher.publishEvent(
                new AnyDeletedEvent(this, AnyTypeKind.USER, user.getKey(), AuthContextUtils.getDomain()));
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
                forEachOrdered(actualKey -> {
                    Role role = roleDAO.find(actualKey.toString());
                    if (role == null) {
                        LOG.error("Could not find role with id {}, even though returned by the native query",
                                actualKey);
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
                forEachOrdered(actualKey -> {
                    Group group = groupDAO().find(actualKey.toString());
                    if (group == null) {
                        LOG.error("Could not find group with id {}, even though returned by the native query",
                                actualKey);
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
                map(membership -> membership.getRightEnd()).collect(Collectors.toSet()));
        result.addAll(findDynGroups(user.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final User user) {
        return findAllGroups(user).stream().map(Entity::getKey).collect(Collectors.toList());
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
        return findAllResources(authFind(key)).stream().map(resource -> resource.getKey()).collect(Collectors.toList());
    }

}
