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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
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
    protected AnySearchDAO searchDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<String, UserTO> selfRead() {
        return ImmutablePair.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                binder.returnUserTO(binder.getAuthenticatedUserTO()));
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
            final int page, final int size, final List<OrderByClause> orderBy,
            final String realm,
            final boolean details) {

        int count = searchDAO.count(RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_SEARCH), realm),
                searchCond == null ? userDAO.getAllMatchingCond() : searchCond, AnyTypeKind.USER);

        List<User> matching = searchDAO.search(RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_SEARCH), realm),
                searchCond == null ? userDAO.getAllMatchingCond() : searchCond,
                page, size, orderBy, AnyTypeKind.USER);
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
    @Override
    public ProvisioningResult<UserTO> create(final UserTO userTO, final boolean nullPriorityAsync) {
        return doCreate(userTO, true, false, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_CREATE + "')")
    public ProvisioningResult<UserTO> create(
            final UserTO userTO, final boolean storePassword, final boolean nullPriorityAsync) {

        return doCreate(userTO, storePassword, false, nullPriorityAsync);
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
            Set<String> effectiveRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_CREATE),
                    before.getLeft().getRealm());
            securityChecks(effectiveRealms, before.getLeft().getRealm(), null);
        }

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(before.getLeft(), storePassword, nullPriorityAsync);

        return afterCreate(
                binder.returnUserTO(binder.getUserTO(created.getKey())), created.getRight(), before.getRight());
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.ANONYMOUS + "'))")
    public ProvisioningResult<UserTO> selfUpdate(final UserPatch userPatch, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userPatch.setKey(userTO.getKey());
        return doUpdate(userPatch, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> update(final UserPatch userPatch, final boolean nullPriorityAsync) {
        return doUpdate(userPatch, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doUpdate(
            final UserPatch userPatch, final boolean self, final boolean nullPriorityAsync) {

        UserTO userTO = binder.getUserTO(userPatch.getKey());
        Set<String> dynRealmsBefore = new HashSet<>(userTO.getDynRealms());
        Pair<UserPatch, List<LogicActions>> before = beforeUpdate(userPatch, userTO.getRealm());

        boolean authDynRealms = false;
        if (!self
                && before.getLeft().getRealm() != null
                && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())) {

            Set<String> effectiveRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                    before.getLeft().getRealm().getValue());
            authDynRealms =
                    securityChecks(effectiveRealms, before.getLeft().getRealm().getValue(), before.getLeft().getKey());
        }

        Pair<String, List<PropagationStatus>> updated = provisioningManager.update(before.getLeft(), nullPriorityAsync);

        return afterUpdate(
                binder.returnUserTO(binder.getUserTO(updated.getKey())),
                updated.getRight(),
                before.getRight(),
                authDynRealms,
                dynRealmsBefore);
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
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                toUpdate.getRealm());
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());

        // ensures the actual user key is effectively on the patch - as the binder.getUserTO(statusPatch.getKey())
        // call above works with username as well
        statusPatch.setKey(toUpdate.getKey());
        Pair<String, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusPatch, nullPriorityAsync);

        return afterUpdate(
                binder.returnUserTO(binder.getUserTO(updated.getKey())),
                updated.getRight(),
                Collections.<LogicActions>emptyList(),
                false,
                Collections.<String>emptySet());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.MUST_CHANGE_PASSWORD + "')")
    public ProvisioningResult<UserTO> changePassword(final String password, final boolean nullPriorityAsync) {
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

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.ANONYMOUS + "'))")
    public ProvisioningResult<UserTO> selfDelete(final boolean nullPriorityAsync) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        return doDelete(userTO, true, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_DELETE + "')")
    @Override
    public ProvisioningResult<UserTO> delete(final String key, final boolean nullPriorityAsync) {
        UserTO userTO = binder.getUserTO(key);
        return doDelete(userTO, false, nullPriorityAsync);
    }

    protected ProvisioningResult<UserTO> doDelete(
            final UserTO userTO, final boolean self, final boolean nullPriorityAsync) {

        Pair<UserTO, List<LogicActions>> before = beforeDelete(userTO);

        if (!self) {
            Set<String> effectiveRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_DELETE),
                    before.getLeft().getRealm());
            securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());
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

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final String key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.unlink(patch)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final String key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.returnUserTO(binder.getUserTO(provisioningManager.link(patch)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.USER_UPDATE + "')")
    @Override
    public ProvisioningResult<UserTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
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

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
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

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

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

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.USER_UPDATE),
                user.getRealm());
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

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

        if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    key = ((UserTO) args[i]).getKey();
                } else if (args[i] instanceof UserPatch) {
                    key = ((UserPatch) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getUserTO((String) key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
