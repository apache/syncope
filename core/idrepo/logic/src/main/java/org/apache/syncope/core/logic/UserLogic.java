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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
public class UserLogic extends AbstractAnyLogic<UserTO, UserCR, UserUR> {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnySearchDAO searchDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final DelegationDAO delegationDAO;

    protected final ConfParamOps confParamOps;

    protected final UserDataBinder binder;

    protected final UserProvisioningManager provisioningManager;

    protected final SyncopeLogic syncopeLogic;

    public UserLogic(
            final RealmDAO realmDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO searchDAO,
            final AccessTokenDAO accessTokenDAO,
            final DelegationDAO delegationDAO,
            final ConfParamOps confParamOps,
            final UserDataBinder binder,
            final UserProvisioningManager provisioningManager,
            final SyncopeLogic syncopeLogic) {

        super(realmDAO, anyTypeDAO, templateUtils);

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.searchDAO = searchDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.delegationDAO = delegationDAO;
        this.confParamOps = confParamOps;
        this.binder = binder;
        this.provisioningManager = provisioningManager;
        this.syncopeLogic = syncopeLogic;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    @Transactional(readOnly = true)
    public Triple<String, String, UserTO> selfRead() {
        UserTO authenticatedUser = binder.getAuthenticatedUserTO();

        return Triple.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                POJOHelper.serialize(delegationDAO.findValidDelegating(authenticatedUser.getKey())), authenticatedUser);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final String key) {
        return binder.getUserTO(key);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<UserTO>> search(
            final SearchCond searchCond,
            final int page, final int size, final List<OrderByClause> orderBy,
            final String realm,
            final boolean details) {

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_SEARCH), realm);

        SearchCond effectiveCond = searchCond == null ? userDAO.getAllMatchingCond() : searchCond;

        int count = searchDAO.count(authRealms, effectiveCond, AnyTypeKind.USER);

        List<User> matching = searchDAO.search(authRealms, effectiveCond, page, size, orderBy, AnyTypeKind.USER);
        List<UserTO> result = matching.stream().
                map(user -> binder.getUserTO(user, details)).
                collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public ProvisioningResult<UserTO> selfCreate(final UserCR createReq, final boolean nullPriorityAsync) {
        return doCreate(createReq, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_CREATE + "')")
    public ProvisioningResult<UserTO> create(final UserCR createReq, final boolean nullPriorityAsync) {
        return doCreate(createReq, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doCreate(
            final UserCR userCR,
            final boolean self,
            final boolean nullPriorityAsync) {

        Pair<UserCR, List<LogicActions>> before = beforeCreate(userCR);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_CREATE),
                    before.getLeft().getRealm());
            userDAO.securityChecks(
                    authRealms,
                    null,
                    before.getLeft().getRealm(),
                    before.getLeft().getMemberships().stream().filter(Objects::nonNull).
                            map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                            collect(Collectors.toSet()));
        }

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(
                before.getLeft(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        return afterCreate(binder.getUserTO(created.getKey()), created.getRight(), before.getRight());
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfUpdate(final UserUR userUR, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userUR.setKey(userTO.getKey());
        ProvisioningResult<UserTO> updated = doUpdate(userUR, true, nullPriorityAsync);

        // Ensures that, if the self update above moves the user into a status from which no authentication
        // is possible, the existing Access Token is clean up to avoid issues with future authentications
        List<String> authStatuses = List.of(confParamOps.get(AuthContextUtils.getDomain(),
                "authentication.statuses", new String[] {}, String[].class));
        if (!authStatuses.contains(updated.getEntity().getStatus())) {
            String accessToken = accessTokenDAO.findByOwner(updated.getEntity().getUsername()).getKey();
            if (accessToken != null) {
                accessTokenDAO.delete(accessToken);
            }
        }

        return updated;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> update(final UserUR userUR, final boolean nullPriorityAsync) {
        return doUpdate(userUR, false, nullPriorityAsync);
    }

    protected Set<String> groups(final UserTO userTO) {
        return userTO.getMemberships().stream().filter(Objects::nonNull).
                map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                collect(Collectors.toSet());
    }

    protected ProvisioningResult<UserTO> doUpdate(
            final UserUR userUR, final boolean self, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getUserTO(userUR.getKey());
        Pair<UserUR, List<LogicActions>> before = beforeUpdate(userUR, userTO.getRealm());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                userTO.getRealm());
        if (!self) {
            Set<String> groups = groups(userTO);
            groups.removeAll(userUR.getMemberships().stream().filter(Objects::nonNull).
                    filter(m -> m.getOperation() == PatchOperation.DELETE).
                    map(MembershipUR::getGroup).filter(Objects::nonNull).
                    collect(Collectors.toSet()));

            userDAO.securityChecks(
                    authRealms,
                    before.getLeft().getKey(),
                    userTO.getRealm(),
                    groups);
        }

        Pair<UserUR, List<PropagationStatus>> after = provisioningManager.update(
                before.getLeft(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<UserTO> result = afterUpdate(
                binder.getUserTO(after.getLeft().getKey()),
                after.getRight(),
                before.getRight());

        return result;
    }

    protected Pair<String, List<PropagationStatus>> setStatusOnWfAdapter(
            final StatusR statusR, final boolean nullPriorityAsync) {

        Pair<String, List<PropagationStatus>> updated;

        switch (statusR.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(
                        statusR, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    public ProvisioningResult<UserTO> status(final StatusR statusR, final boolean nullPriorityAsync) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusR.getKey());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                toUpdate.getRealm());
        userDAO.securityChecks(
                authRealms,
                toUpdate.getKey(),
                toUpdate.getRealm(),
                groups(toUpdate));

        // ensures the actual user key is effectively on the request - as the binder.getUserTO(statusR.getKey())
        // call above works with username as well
        statusR.setKey(toUpdate.getKey());
        Pair<String, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusR, nullPriorityAsync);

        return afterUpdate(
                binder.getUserTO(updated.getKey()),
                updated.getRight(),
                List.of());
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfStatus(final StatusR statusR, final boolean nullPriorityAsync) {
        statusR.setKey(userDAO.findKey(AuthContextUtils.getUsername()));
        Pair<String, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusR, nullPriorityAsync);

        return afterUpdate(
                binder.getUserTO(updated.getKey()),
                updated.getRight(),
                List.of());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "')")
    public ProvisioningResult<UserTO> mustChangePassword(final String password, final boolean nullPriorityAsync) {
        UserUR userUR = new UserUR();
        userUR.setPassword(new PasswordPatch.Builder().value(password).build());
        userUR.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(false).build());
        return selfUpdate(userUR, nullPriorityAsync);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (syncopeLogic.isPwdResetRequiringSecurityQuestions()
                && (securityAnswer == null
                || !Encryptor.getInstance().verify(securityAnswer, user.getCipherAlgorithm(),
                user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey(), AuthContextUtils.getUsername(), REST_CONTEXT);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(
                user.getKey(), token, password, AuthContextUtils.getUsername(), REST_CONTEXT);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + IdRepoEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfDelete(final boolean nullPriorityAsync) {
        return doDelete(binder.getAuthenticatedUserTO(), true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_DELETE + "')")
    @Override
    public ProvisioningResult<UserTO> delete(final String key, final boolean nullPriorityAsync) {
        return doDelete(binder.getUserTO(key), false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doDelete(
            final UserTO userTO, final boolean self, final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeDelete(userTO);

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_DELETE),
                    before.getLeft().getRealm());
            userDAO.securityChecks(
                    authRealms,
                    before.getLeft().getKey(),
                    before.getLeft().getRealm(),
                    groups(before.getLeft()));
        }

        List<Group> ownedGroups = groupDAO.findOwnedByUser(before.getLeft().getKey());
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(ownedGroups.stream().
                    map(group -> group.getKey() + ' ' + group.getName()).collect(Collectors.toList()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(
                before.getLeft().getKey(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        UserTO deletedTO;
        if (userDAO.find(before.getLeft().getKey()) == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(before.getLeft().getKey());
        } else {
            deletedTO = binder.getUserTO(before.getLeft().getKey());
        }

        return afterDelete(deletedTO, statuses, before.getRight());
    }

    protected void updateChecks(final String key) {
        User user = userDAO.authFind(key);

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.USER_UPDATE),
                user.getRealm().getFullPath());
        userDAO.securityChecks(
                authRealms,
                user.getKey(),
                user.getRealm().getFullPath(),
                user.getMemberships().stream().
                        map(m -> m.getRightEnd().getKey()).
                        collect(Collectors.toSet()));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserUR req = new UserUR();
        req.setKey(key);
        req.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                collect(Collectors.toList()));

        return binder.getUserTO(provisioningManager.unlink(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserUR req = new UserUR();
        req.setKey(key);
        req.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                collect(Collectors.toList()));

        return binder.getUserTO(provisioningManager.link(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        UserUR req = new UserUR();
        req.setKey(key);
        req.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                collect(Collectors.toList()));

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        UserUR req = new UserUR();
        req.setKey(key);
        req.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                collect(Collectors.toList()));

        if (changepwd) {
            req.setPassword(new PasswordPatch.Builder().
                    value(password).onSyncope(false).resources(resources).build());
        }

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getUserTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> provision(
            final String key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.provision(
                key, changePwd, password, resources, nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getUserTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        String key = null;

        if ("requestPasswordReset".equals(method.getName())) {
            key = userDAO.findKey((String) args[0]);
        } else if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    key = ((UserTO) args[i]).getKey();
                } else if (args[i] instanceof UserUR) {
                    key = ((UserUR) args[i]).getKey();
                } else if (args[i] instanceof StatusR) {
                    key = ((StatusR) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getUserTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
