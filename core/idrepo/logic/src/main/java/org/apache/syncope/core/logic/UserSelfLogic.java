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
package org.apache.syncope.core.logic;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.beans.ComplianceQuery;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.policy.AccountPolicyException;
import org.apache.syncope.core.spring.policy.PasswordPolicyException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class UserSelfLogic extends AbstractUserLogic {

    public record Self(UserTO user, String entitlements, String delegations) {

    }

    protected static void throwMfaWasSet(final String username) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.EntityExists);
        sce.getElements().add("MFA secret for " + username + " already set");
        throw sce;
    }

    protected final DelegationDAO delegationDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final RuleProvider ruleProvider;

    public UserSelfLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final UserDataBinder binder,
            final UserProvisioningManager provisioningManager,
            final EncryptorManager encryptorManager,
            final ConfParamOps confParamOps,
            final DelegationDAO delegationDAO,
            final AccessTokenDAO accessTokenDAO,
            final ExternalResourceDAO resourceDAO,
            final RuleProvider ruleProvider) {

        super(realmSearchDAO,
                anyTypeDAO,
                templateUtils,
                userDAO,
                binder,
                provisioningManager,
                encryptorManager,
                confParamOps);
        this.delegationDAO = delegationDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.resourceDAO = resourceDAO;
        this.ruleProvider = ruleProvider;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    @Transactional(readOnly = true)
    public Self read() {
        UserTO authenticatedUser = binder.getAuthenticatedUserTO();

        return new Self(
                authenticatedUser,
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                POJOHelper.serialize(delegationDAO.findValidDelegating(
                        authenticatedUser.getKey(), OffsetDateTime.now())));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public ProvisioningResult<UserTO> create(final UserCR createReq, final boolean nullPriorityAsync) {
        if (!confParamOps.get(
                AuthContextUtils.getDomain(), StandardConfParams.SELF_REGISTRATION_ALLOWED, false, boolean.class)) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Self registration forbidden by configuration");
            throw sce;
        }

        return doCreate(createReq, true, nullPriorityAsync);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> update(final UserUR userUR, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userUR.setKey(userTO.getKey());
        ProvisioningResult<UserTO> updated = doUpdate(userUR, true, nullPriorityAsync);

        // Ensures that, if the self update above moves the user into a status from which no authentication
        // is possible, the existing Access Token is clean up to avoid issues with future authentications
        List<String> authStatuses = List.of(confParamOps.get(
                AuthContextUtils.getDomain(), StandardConfParams.AUTHENTICATION_STATUSES, new String[] {},
                String[].class));
        if (!authStatuses.contains(updated.getEntity().getStatus())) {
            accessTokenDAO.findByOwner(updated.getEntity().getUsername()).ifPresent(accessTokenDAO::delete);
        }

        return updated;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> status(final StatusR statusR, final boolean nullPriorityAsync) {
        // security checks
        UserTO toUpdate = binder.getAuthenticatedUserTO();
        statusR.setKey(toUpdate.getKey());

        return doStatus(statusR, nullPriorityAsync);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MFA_ENROLL + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> delete(final boolean nullPriorityAsync) {
        return doDelete(binder.getAuthenticatedUserTO(), true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "')")
    public ProvisioningResult<UserTO> mustChangePassword(
            final PasswordPatch password, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getAuthenticatedUserTO();

        password.setOnSyncope(true);
        password.getResources().clear();
        password.getResources().addAll(userDAO.findAllResourceKeys(userTO.getKey()));

        UserUR userUR = new UserUR.Builder(userTO.getKey()).
                password(password).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(false).build()).
                build();
        ProvisioningResult<UserTO> result = update(userUR, nullPriorityAsync);

        accessTokenDAO.findByOwner(result.getEntity().getUsername()).ifPresent(accessTokenDAO::delete);

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public void compliance(final ComplianceQuery query) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RESTValidation);

        if (query.isEmpty()) {
            sce.getElements().add("Nothing to check");
            throw sce;
        }

        Realm realm = null;
        if (StringUtils.isNotBlank(query.getRealm())) {
            realm = realmSearchDAO.findByFullPath(query.getRealm()).
                    orElseThrow(() -> new NotFoundException("Realm " + query.getRealm()));
        }
        Set<ExternalResource> resources = query.getResources().stream().
                map(resourceDAO::findById).flatMap(Optional::stream).collect(Collectors.toSet());
        if (realm == null && resources.isEmpty()) {
            sce.getElements().add("Nothing to check");
            throw sce;
        }

        if (StringUtils.isNotBlank(query.getUsername())) {
            List<AccountPolicy> accountPolicies = ruleProvider.getAccountPolicies(realm, resources);
            try {
                if (accountPolicies.isEmpty()) {
                    if (!Entity.ID_PATTERN.matcher(query.getUsername()).matches()) {
                        throw new AccountPolicyException("Character(s) not allowed: " + query.getUsername());
                    }
                } else {
                    for (AccountPolicy policy : accountPolicies) {
                        ruleProvider.getAccountRules(policy).forEach(rule -> rule.enforce(query.getUsername()));
                    }
                }
            } catch (AccountPolicyException e) {
                throw new InvalidEntityException(User.class, EntityViolationType.InvalidUsername, e.getMessage());
            }
        }

        if (StringUtils.isNotBlank(query.getPassword())) {
            try {
                for (PasswordPolicy policy : ruleProvider.getPasswordPolicies(realm, resources)) {
                    ruleProvider.getPasswordRules(policy).
                            forEach(rule -> rule.enforce(query.getUsername(), query.getPassword()));
                }
            } catch (PasswordPolicyException e) {
                throw new InvalidEntityException(User.class, EntityViolationType.InvalidPassword, e.getMessage());
            }
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (!confParamOps.get(
                AuthContextUtils.getDomain(), StandardConfParams.PASSWORD_RESET_ALLOWED, false, boolean.class)) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        User user = userDAO.findByUsername(username).
                orElseThrow(() -> new NotFoundException("User " + username));

        if (confParamOps.get(
                AuthContextUtils.getDomain(), StandardConfParams.PASSWORD_RESET_SECURITY_QUESTION, false, boolean.class)
                && (securityAnswer == null || !encryptorManager.getInstance().
                        verify(securityAnswer, user.getCipherAlgorithm(), user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey(), AuthContextUtils.getUsername(), REST_CONTEXT);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        if (!confParamOps.get(
                AuthContextUtils.getDomain(), StandardConfParams.PASSWORD_RESET_ALLOWED, false, boolean.class)) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Password reset forbidden by configuration");
            throw sce;
        }

        User user = userDAO.findByToken(token).
                orElseThrow(() -> new NotFoundException("User with token " + token));

        provisioningManager.confirmPasswordReset(
                user.getKey(), token, password, AuthContextUtils.getUsername(), REST_CONTEXT);
    }
}
