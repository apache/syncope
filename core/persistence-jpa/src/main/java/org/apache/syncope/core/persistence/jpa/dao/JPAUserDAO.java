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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
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
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPADynRoleMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
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
    private RealmDAO realmDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ImplementationLookup implementationLookup;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Override
    protected AnyUtils init() {
        return new JPAAnyUtilsFactory().getInstance(AnyTypeKind.USER);
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

            Set<String> authRealms = AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_READ);
            boolean authorized = IterableUtils.matchesAny(authRealms, new Predicate<String>() {

                @Override
                public boolean evaluate(final String realm) {
                    return user.getRealm().getFullPath().startsWith(realm);
                }
            });
            if (authRealms == null || authRealms.isEmpty() || !authorized) {
                throw new DelegatedAdministrationException(AnyTypeKind.USER, user.getKey());
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public User authFindByUsername(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = findByUsername(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        securityChecks(user);

        return user;
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
        for (Realm realm : realmDAO.findAncestors(user.getRealm())) {
            policy = realm.getPasswordPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        return policies;
    }

    private List<AccountPolicy> getAccountPolicies(final User user) {
        List<AccountPolicy> policies = new ArrayList<>();

        // add resource policies        
        for (ExternalResource resource : findAllResources(user)) {
            AccountPolicy policy = resource.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

        // add realm policies
        for (Realm realm : realmDAO.findAncestors(user.getRealm())) {
            AccountPolicy policy = realm.getAccountPolicy();
            if (policy != null) {
                policies.add(policy);
            }
        }

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

                for (PasswordRuleConf ruleConf : policy.getRuleConfs()) {
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
                }

                if (user.verifyPasswordHistory(user.getClearPassword(), policy.getHistoryLength())) {
                    throw new PasswordPolicyException("Password value was used in the past: not allowed");
                }

                if (policy.getHistoryLength() > maxPPSpecHistory) {
                    maxPPSpecHistory = policy.getHistoryLength();
                }
            }

            // update user's password history with encrypted password
            if (maxPPSpecHistory > 0 && user.getPassword() != null) {
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
            if (adminUser.equals(user.getUsername()) || anonymousUser.equals(user.getUsername())) {
                throw new AccountPolicyException("Not allowed: " + user.getUsername());
            }

            if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
                throw new AccountPolicyException("Character(s) not allowed");
            }

            for (AccountPolicy policy : getAccountPolicies(user)) {
                for (AccountRuleConf ruleConf : policy.getRuleConfs()) {
                    Class<? extends AccountRule> ruleClass =
                            implementationLookup.getAccountRuleClass(ruleConf.getClass());
                    if (ruleClass == null) {
                        LOG.warn("Could not find matching password rule for {}", ruleConf.getClass());
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
                }

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

    @Override
    public User save(final User user) {
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

        roleDAO.refreshDynMemberships(merged);
        groupDAO.refreshDynMemberships(merged);

        return merged;
    }

    @Override
    public void delete(final User user) {
        for (Role role : findDynRoleMemberships(user)) {
            role.getDynMembership().getMembers().remove(user);
        }
        for (Group group : findDynGroupMemberships(user)) {
            group.getUDynMembership().getMembers().remove(user);
        }

        entityManager().remove(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Role> findDynRoleMemberships(final User user) {
        Query query = entityManager().createNativeQuery(
                "SELECT t2.id FROM " + JPADynRoleMembership.TABLE + " t0 "
                + "INNER JOIN " + JPADynRoleMembership.TABLE + "_User t1 "
                + "ON t0.id = t1.dynRoleMembership_id "
                + "LEFT OUTER JOIN " + JPARole.TABLE + " t2 "
                + "ON t0.ROLE_ID = t2.id "
                + "WHERE (t1.user_id = ?1)");
        query.setParameter(1, user.getKey());

        List<Role> result = new ArrayList<>();
        for (Object key : query.getResultList()) {
            String actualKey = key instanceof Object[]
                    ? (String) ((Object[]) key)[0]
                    : ((String) key);

            Role role = roleDAO.find(actualKey);
            if (role == null) {
                LOG.error("Could not find role with id {}, even though returned by the native query", actualKey);
            } else if (!result.contains(role)) {
                result.add(role);
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroupMemberships(final User user) {
        Query query = entityManager().createNativeQuery(
                "SELECT t2.id FROM " + JPAUDynGroupMembership.TABLE + " t0 "
                + "INNER JOIN " + JPAUDynGroupMembership.TABLE + "_User t1 "
                + "ON t0.id = t1.uDynGroupMembership_id "
                + "LEFT OUTER JOIN " + JPAGroup.TABLE + " t2 "
                + "ON t0.GROUP_ID = t2.id "
                + "WHERE (t1.user_id = ?1)");
        query.setParameter(1, user.getKey());

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
    public Collection<Role> findAllRoles(final User user) {
        return CollectionUtils.union(user.getRoles(), findDynRoleMemberships(user));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final User user) {
        return CollectionUtils.union(
                CollectionUtils.collect(user.getMemberships(), new Transformer<UMembership, Group>() {

                    @Override
                    public Group transform(final UMembership input) {
                        return input.getRightEnd();
                    }
                }, new ArrayList<Group>()),
                findDynGroupMemberships(user));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final User user) {
        return CollectionUtils.collect(findAllGroups(user), EntityUtils.<Group>keyTransformer());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupNames(final User user) {
        return CollectionUtils.collect(findAllGroups(user), new Transformer<Group, String>() {

            @Override
            public String transform(final Group input) {
                return input.getName();
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final User user) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(user.getResources());
        for (Group group : findAllGroups(user)) {
            result.addAll(group.getResources());
        }

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllResourceNames(final String key) {
        return CollectionUtils.collect(findAllResources(authFind(key)), EntityUtils.keyTransformer());
    }

}
