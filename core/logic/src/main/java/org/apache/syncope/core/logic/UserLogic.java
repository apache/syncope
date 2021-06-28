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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
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
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class UserLogic extends AbstractAnyLogic<UserTO, UserPatch> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected AccessTokenDAO accessTokenDAO;

    @Autowired
    protected DelegationDAO delegationDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "'))")
    @Transactional(readOnly = true)
    public Triple<String, String, UserTO> selfRead() {
        UserTO authenticatedUser = binder.getAuthenticatedUserTO();

        return Triple.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                POJOHelper.serialize(delegationDAO.findValidDelegating(authenticatedUser.getKey())),
                binder.returnUserTO(authenticatedUser));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final String key) {
        return binder.returnUserTO(binder.getUserTO(key));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<UserTO>> search(
            final SearchCond searchCond,
            final int page,
            final int size,
            final List<OrderByClause> orderBy,
            final String realm,
            final boolean details) {

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_SEARCH), realm);

        SearchCond effectiveCond = searchCond == null ? userDAO.getAllMatchingCond() : searchCond;

        int count = searchDAO.count(authRealms, effectiveCond, AnyTypeKind.USER);

        List<User> matching = searchDAO.search(authRealms, effectiveCond, page, size, orderBy, AnyTypeKind.USER);
        List<UserTO> result = matching.stream().
                map(user -> binder.returnUserTO(binder.getUserTO(user, details))).
                collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + StandardEntitlement.ANONYMOUS + "')")
    public ProvisioningResult<UserTO> selfCreate(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return doCreate(userTO, storePassword, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_CREATE + "')")
    public ProvisioningResult<UserTO> create(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return doCreate(userTO, storePassword, false, nullPriorityAsync);
    }

    protected Set<String> groups(final UserTO userTO) {
        return userTO.getMemberships().stream().filter(Objects::nonNull).
                map(MembershipTO::getGroupKey).filter(Objects::nonNull).
                collect(Collectors.toSet());
    }

    protected ProvisioningResult<UserTO> doCreate(
            final UserTO userTO,
            final boolean storePassword,
            final boolean self,
            final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeCreate(userTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_CREATE),
                    before.getLeft().getRealm());
            userDAO.securityChecks(
                    authRealms,
                    null,
                    before.getLeft().getRealm(),
                    groups(before.getLeft()));
        }

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(before.getLeft(), storePassword, nullPriorityAsync);

        return afterCreate(
                binder.returnUserTO(binder.getUserTO(created.getKey())), created.getRight(), before.getRight());
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + StandardEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfUpdate(final UserPatch userPatch, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userPatch.setKey(userTO.getKey());
        ProvisioningResult<UserTO> updated = doUpdate(userPatch, true, nullPriorityAsync);

        // Ensures that, if the self update above moves the user into a status from which no authentication
        // is possible, the existing Access Token is clean up to avoid issues with future authentications
        if (!confDAO.getValuesAsStrings("authentication.statuses").contains(updated.getEntity().getStatus())) {
            String accessToken = accessTokenDAO.findByOwner(updated.getEntity().getUsername()).getKey();
            if (accessToken != null) {
                accessTokenDAO.delete(accessToken);
            }
        }

        return updated;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> update(final UserPatch userPatch, final boolean nullPriorityAsync) {
        return doUpdate(userPatch, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doUpdate(
            final UserPatch userPatch, final boolean self, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getUserTO(userPatch.getKey());
        Pair<UserPatch, List<LogicActions>> before = beforeUpdate(userPatch, userTO.getRealm());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                userTO.getRealm());
        if (!self) {
            Set<String> groups = groups(userTO);
            groups.removeAll(userPatch.getMemberships().stream().filter(Objects::nonNull).
                    filter(m -> m.getOperation() == PatchOperation.DELETE).
                    map(MembershipPatch::getGroup).filter(Objects::nonNull).
                    collect(Collectors.toSet()));

            userDAO.securityChecks(
                    authRealms,
                    before.getLeft().getKey(),
                    userTO.getRealm(),
                    groups);
        }

        Pair<UserPatch, List<PropagationStatus>> after =
                provisioningManager.update(before.getLeft(), nullPriorityAsync);

        ProvisioningResult<UserTO> result = afterUpdate(
                binder.returnUserTO(binder.getUserTO(after.getLeft().getKey())),
                after.getRight(),
                before.getRight());

        return result;
    }

    protected Pair<String, List<PropagationStatus>> setStatusOnWfAdapter(
            final StatusPatch statusPatch, final boolean nullPriorityAsync) {

        Pair<String, List<PropagationStatus>> updated;

        switch (statusPatch.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(statusPatch, nullPriorityAsync);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(statusPatch, nullPriorityAsync);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(statusPatch, nullPriorityAsync);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    public ProvisioningResult<UserTO> status(final StatusPatch statusPatch, final boolean nullPriorityAsync) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusPatch.getKey());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                toUpdate.getRealm());
        userDAO.securityChecks(
                authRealms,
                toUpdate.getKey(),
                toUpdate.getRealm(),
                groups(toUpdate));

        // ensures the actual user key is effectively on the patch - as the binder.getUserTO(statusPatch.getKey())
        // call above works with username as well
        statusPatch.setKey(toUpdate.getKey());
        Pair<String, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusPatch, nullPriorityAsync);

        return afterUpdate(
                binder.returnUserTO(binder.getUserTO(updated.getKey())),
                updated.getRight(),
                Collections.emptyList());
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfStatus(final StatusPatch statusPatch, final boolean nullPriorityAsync) {
        statusPatch.setKey(userDAO.findKey(AuthContextUtils.getUsername()));
        Pair<String, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusPatch, nullPriorityAsync);

        return afterUpdate(
                binder.returnUserTO(binder.getUserTO(updated.getKey())),
                updated.getRight(),
                Collections.emptyList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "')")
    public ProvisioningResult<UserTO> mustChangePassword(final String password, final boolean nullPriorityAsync) {
        UserPatch userPatch = new UserPatch();
        userPatch.setPassword(new PasswordPatch.Builder().value(password).build());
        userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(false).build());
        return selfUpdate(userPatch, nullPriorityAsync);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + StandardEntitlement.ANONYMOUS + "')")
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
                && (securityAnswer == null || !securityAnswer.equals(user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + StandardEntitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(user.getKey(), token, password);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole('" + StandardEntitlement.ANONYMOUS + "')) "
            + "and not(hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "'))")
    public ProvisioningResult<UserTO> selfDelete(final boolean nullPriorityAsync) {
        return doDelete(binder.getAuthenticatedUserTO(), true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_DELETE + "')")
    @Override
    public ProvisioningResult<UserTO> delete(final String key, final boolean nullPriorityAsync) {
        return doDelete(binder.getUserTO(key), false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doDelete(
            final UserTO userTO, final boolean self, final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeDelete(userTO);

        if (!self) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_DELETE),
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
                    map(group -> group.getKey() + " " + group.getName()).collect(Collectors.toList()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey(), nullPriorityAsync);

        UserTO deletedTO;
        if (userDAO.find(before.getLeft().getKey()) == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(before.getLeft().getKey());
        } else {
            deletedTO = binder.getUserTO(before.getLeft().getKey());
        }

        return afterDelete(binder.returnUserTO(deletedTO), statuses, before.getRight());
    }

    protected void updateChecks(final String key) {
        User user = userDAO.authFind(key);

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm().getFullPath());
        userDAO.securityChecks(
                authRealms,
                user.getKey(),
                user.getRealm().getFullPath(),
                user.getMemberships().stream().
                        map(m -> m.getRightEnd().getKey()).
                        collect(Collectors.toSet()));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                collect(Collectors.toList()));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.unlink(patch)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final String key, final Collection<String> resources) {
        updateChecks(key);

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                collect(Collectors.toList()));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.link(patch)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                collect(Collectors.toList()));

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                collect(Collectors.toList()));

        if (changepwd) {
            patch.setPassword(new PasswordPatch.Builder().
                    value(password).onSyncope(false).resources(resources).build());
        }

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources, nullPriorityAsync);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.returnUserTO(binder.getUserTO(key)));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> provision(
            final String key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.provision(key, changePwd, password, resources,
                nullPriorityAsync);

        ProvisioningResult<UserTO> result = new ProvisioningResult<>();
        result.setEntity(binder.returnUserTO(binder.getUserTO(key)));
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
                } else if (args[i] instanceof UserPatch) {
                    key = ((UserPatch) args[i]).getKey();
                } else if (args[i] instanceof StatusPatch) {
                    key = ((StatusPatch) args[i]).getKey();
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
