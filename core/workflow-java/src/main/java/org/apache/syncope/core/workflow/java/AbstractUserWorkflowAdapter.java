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
package org.apache.syncope.core.workflow.java;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.policy.AccountPolicyException;
import org.apache.syncope.core.spring.policy.PasswordPolicyException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractUserWorkflowAdapter implements UserWorkflowAdapter {

    protected static final Logger LOG = LoggerFactory.getLogger(UserWorkflowAdapter.class);

    @Autowired
    protected UserDataBinder dataBinder;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Resource(name = "adminUser")
    protected String adminUser;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    protected final Map<String, AccountRule> perContextAccountRules = new ConcurrentHashMap<>();

    protected final Map<String, PasswordRule> perContextPasswordRules = new ConcurrentHashMap<>();

    @Override
    public String getPrefix() {
        return null;
    }

    protected List<AccountPolicy> getAccountPolicies(final User user) {
        List<AccountPolicy> policies = new ArrayList<>();

        // add resource policies
        userDAO.findAllResources(user).stream().
                map(ExternalResource::getAccountPolicy).
                filter(Objects::nonNull).
                forEach(policies::add);

        // add realm policies
        realmDAO.findAncestors(user.getRealm()).stream().
                map(Realm::getAccountPolicy).
                filter(Objects::nonNull).
                forEach(policies::add);

        return policies;
    }

    protected List<AccountRule> getAccountRules(final AccountPolicy policy) {
        List<AccountRule> result = new ArrayList<>();

        for (Implementation impl : policy.getRules()) {
            try {
                ImplementationManager.buildAccountRule(
                        impl,
                        () -> perContextAccountRules.get(impl.getKey()),
                        instance -> perContextAccountRules.put(impl.getKey(), instance)).
                        ifPresent(result::add);
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        }

        return result;
    }

    protected List<PasswordPolicy> getPasswordPolicies(final User user) {
        List<PasswordPolicy> policies = new ArrayList<>();

        // add resource policies
        userDAO.findAllResources(user).
                forEach(resource -> Optional.ofNullable(resource.getPasswordPolicy()).
                filter(p -> !policies.contains(p)).
                ifPresent(policies::add));

        // add realm policies
        realmDAO.findAncestors(user.getRealm()).
                forEach(realm -> Optional.ofNullable(realm.getPasswordPolicy()).
                filter(p -> !policies.contains(p)).
                ifPresent(policies::add));

        return policies;
    }

    protected List<PasswordRule> getPasswordRules(final PasswordPolicy policy) {
        List<PasswordRule> result = new ArrayList<>();

        for (Implementation impl : policy.getRules()) {
            try {
                ImplementationManager.buildPasswordRule(
                        impl,
                        () -> perContextPasswordRules.get(impl.getKey()),
                        instance -> perContextPasswordRules.put(impl.getKey(), instance)).
                        ifPresent(result::add);
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        }

        return result;
    }

    protected Pair<Boolean, Boolean> enforcePolicies(
            final User user,
            final boolean disablePwdPolicyCheck,
            final String clearPassword) {

        if (!disablePwdPolicyCheck) {
            // ------------------------------
            // Verify password policies
            // ------------------------------
            LOG.debug("Password Policy enforcement");

            try {
                int maxPPSpecHistory = 0;
                for (PasswordPolicy policy : getPasswordPolicies(user)) {
                    if (clearPassword == null && !policy.isAllowNullPassword()) {
                        throw new PasswordPolicyException("Password mandatory");
                    }

                    getPasswordRules(policy).forEach(rule -> {
                        rule.enforce(user, clearPassword);

                        user.getLinkedAccounts().stream().
                                filter(account -> account.getPassword() != null).
                                forEach(rule::enforce);
                    });

                    boolean matching = false;
                    if (policy.getHistoryLength() > 0) {
                        List<String> pwdHistory = user.getPasswordHistory();
                        matching = pwdHistory.subList(policy.getHistoryLength() >= pwdHistory.size()
                                ? 0
                                : pwdHistory.size() - policy.getHistoryLength(), pwdHistory.size()).stream().
                                map(old -> Encryptor.getInstance().verify(
                                clearPassword, user.getCipherAlgorithm(), old)).
                                reduce(matching, (accumulator, item) -> accumulator | item);
                    }
                    if (matching) {
                        throw new PasswordPolicyException("Password value was used in the past: not allowed");
                    }

                    if (policy.getHistoryLength() > maxPPSpecHistory) {
                        maxPPSpecHistory = policy.getHistoryLength();
                    }
                }

                // update user's password history with encrypted password
                if (maxPPSpecHistory > 0
                        && user.getPassword() != null
                        && !user.getPasswordHistory().contains(user.getPassword())) {

                    user.getPasswordHistory().add(user.getPassword());
                }
                // keep only the last maxPPSpecHistory items in user's password history
                if (maxPPSpecHistory < user.getPasswordHistory().size()) {
                    for (int i = 0; i < user.getPasswordHistory().size() - maxPPSpecHistory; i++) {
                        user.getPasswordHistory().remove(i);
                    }
                }
            } catch (InvalidEntityException e) {
                throw e;
            } catch (Exception e) {
                LOG.error("Invalid password for {}", user, e);
                throw new InvalidEntityException(User.class, EntityViolationType.InvalidPassword, e.getMessage());
            }
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

            List<AccountPolicy> accountPolicies = getAccountPolicies(user);
            if (accountPolicies.isEmpty()) {
                if (!Entity.ID_PATTERN.matcher(user.getUsername()).matches()) {
                    throw new AccountPolicyException("Character(s) not allowed: " + user.getUsername());
                }
                user.getLinkedAccounts().stream().
                        filter(account -> account.getUsername() != null).
                        forEach(account -> {
                            if (!Entity.ID_PATTERN.matcher(account.getUsername()).matches()) {
                                throw new AccountPolicyException("Character(s) not allowed: " + account.getUsername());
                            }
                        });
            } else {
                for (AccountPolicy policy : accountPolicies) {
                    getAccountRules(policy).forEach(rule -> {
                        rule.enforce(user);

                        user.getLinkedAccounts().stream().
                                filter(account -> account.getUsername() != null).
                                forEach(rule::enforce);
                    });

                    suspend |= user.getFailedLogins() != null && policy.getMaxAuthenticationAttempts() > 0
                            && user.getFailedLogins() > policy.getMaxAuthenticationAttempts() && !user.isSuspended();
                    propagateSuspension |= policy.isPropagateSuspension();
                }
            }
        } catch (InvalidEntityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Invalid username for {}", user, e);
            throw new InvalidEntityException(User.class, EntityViolationType.InvalidUsername, e.getMessage());
        }

        return Pair.of(suspend, propagateSuspension);
    }

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, false, null, storePassword);
    }

    protected abstract UserWorkflowResult<Pair<String, Boolean>> doCreate(
            UserTO userTO, boolean disablePwdPolicyCheck, Boolean enabled, boolean storePassword);

    @Override
    public UserWorkflowResult<Pair<String, Boolean>> create(
            final UserTO userTO,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final boolean storePassword) {

        UserWorkflowResult<Pair<String, Boolean>> result =
                doCreate(userTO, disablePwdPolicyCheck, enabled, storePassword);

        // enforce password and account policies
        User user = userDAO.find(result.getResult().getKey());
        enforcePolicies(user, disablePwdPolicyCheck, disablePwdPolicyCheck ? null : userTO.getPassword());
        userDAO.save(user);

        return result;
    }

    protected abstract UserWorkflowResult<String> doActivate(User user, String token);

    @Override
    public UserWorkflowResult<String> activate(final String key, final String token) {
        return doActivate(userDAO.authFind(key), token);
    }

    protected abstract UserWorkflowResult<Pair<UserPatch, Boolean>> doUpdate(User user, UserPatch userPatch);

    @Override
    public UserWorkflowResult<Pair<UserPatch, Boolean>> update(final UserPatch userPatch) {
        User user = userDAO.find(userPatch.getKey());

        UserWorkflowResult<Pair<UserPatch, Boolean>> result;
        // skip actual workflow operations in case only password change on resources was requested
        if (userPatch.isEmptyButPassword() && !userPatch.getPassword().isOnSyncope()) {
            PropagationByResource<String> propByRes = new PropagationByResource<>();
            userDAO.findAllResources(user).stream().
                    filter(resource -> userPatch.getPassword().getResources().contains(resource.getKey())).
                    forEach(resource -> propByRes.add(ResourceOperation.UPDATE, resource.getKey()));

            PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
            user.getLinkedAccounts().stream().
                    filter(account -> userPatch.getPassword().getResources().contains(account.getResource().getKey())).
                    forEach(account -> propByLinkedAccount.add(
                    ResourceOperation.UPDATE,
                    Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

            result = new UserWorkflowResult<>(
                    Pair.of(userPatch, !user.isSuspended()), propByRes, propByLinkedAccount, "update");
        } else {
            result = doUpdate(userDAO.authFind(userPatch.getKey()), userPatch);
        }

        // enforce password and account policies
        enforcePolicies(
                user,
                false,
                Optional.ofNullable(userPatch.getPassword()).map(PasswordPatch::getValue).orElse(null));
        user = userDAO.save(user);

        if (!AuthContextUtils.getUsername().equals(user.getUsername())) {
            // ensure that requester's administration rights are still valid
            Set<String> authRealms = new HashSet<>();
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(StandardEntitlement.USER_READ, Collections.emptySet()));
            authRealms.addAll(AuthContextUtils.getAuthorizations().
                    getOrDefault(StandardEntitlement.USER_UPDATE, Collections.emptySet()));
            userDAO.securityChecks(
                    authRealms,
                    user.getKey(),
                    user.getRealm().getFullPath(),
                    userDAO.findAllGroupKeys(user));
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doSuspend(User user);

    @Override
    public UserWorkflowResult<String> suspend(final String key) {
        User user = userDAO.authFind(key);

        // set suspended flag
        user.setSuspended(Boolean.TRUE);

        return doSuspend(user);
    }

    @Override
    public Pair<UserWorkflowResult<String>, Boolean> internalSuspend(final String key) {
        User user = userDAO.authFind(key);

        Pair<UserWorkflowResult<String>, Boolean> result = null;

        Pair<Boolean, Boolean> enforce = enforcePolicies(user, true, null);
        if (enforce.getKey()) {
            LOG.debug("User {} {} is over the max failed logins", user.getKey(), user.getUsername());

            // reduce failed logins number to avoid multiple request       
            user.setFailedLogins(user.getFailedLogins() - 1);

            // set suspended flag
            user.setSuspended(Boolean.TRUE);

            result = Pair.of(doSuspend(user), enforce.getValue());
        }

        return result;
    }

    protected abstract UserWorkflowResult<String> doReactivate(User user);

    @Override
    public UserWorkflowResult<String> reactivate(final String key) {
        User user = userDAO.authFind(key);

        // reset failed logins
        user.setFailedLogins(0);

        // reset suspended flag
        user.setSuspended(Boolean.FALSE);

        return doReactivate(user);
    }

    protected abstract void doRequestPasswordReset(User user);

    @Override
    public void requestPasswordReset(final String key) {
        doRequestPasswordReset(userDAO.authFind(key));
    }

    protected abstract UserWorkflowResult<Pair<UserPatch, Boolean>> doConfirmPasswordReset(
            User user, String token, String password);

    @Override
    public UserWorkflowResult<Pair<UserPatch, Boolean>> confirmPasswordReset(
            final String key, final String token, final String password) {

        User user = userDAO.authFind(key);

        // enforce password and account policies
        enforcePolicies(user, false, password);
        user = userDAO.save(user);

        return doConfirmPasswordReset(user, token, password);
    }

    protected abstract void doDelete(User user);

    @Override
    public void delete(final String userKey) {
        doDelete(userDAO.authFind(userKey));
    }
}
