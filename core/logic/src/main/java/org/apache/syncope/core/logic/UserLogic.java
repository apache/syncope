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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
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
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
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
    protected UserDataBinder binder;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public String getUsername(final Long key) {
        return binder.getUserTO(key).getUsername();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    public Long getKey(final String username) {
        return binder.getUserTO(username).getKey();
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public int count(final List<String> realms) {
        return userDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_LIST + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> list(
            final int page, final int size, final List<OrderByClause> orderBy,
            final List<String> realms, final boolean details) {

        return CollectionUtils.collect(userDAO.findAll(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_LIST), realms),
                page, size, orderBy),
                new Transformer<User, UserTO>() {

                    @Override
                    public UserTO transform(final User input) {
                        return binder.getUserTO(input, details);
                    }
                }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<String, UserTO> readSelf() {
        return ImmutablePair.of(
                POJOHelper.serialize(AuthContextUtils.getAuthorizations()),
                binder.getAuthenticatedUserTO());
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final Long key) {
        return binder.getUserTO(key);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public int searchCount(final SearchCond searchCondition, final List<String> realms) {
        return searchDAO.count(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, AnyTypeKind.USER);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final List<String> realms, final boolean details) {

        List<User> matchingUsers = searchDAO.search(
                getEffectiveRealms(AuthContextUtils.getAuthorizations().get(Entitlement.USER_SEARCH), realms),
                searchCondition, page, size, orderBy, AnyTypeKind.USER);
        return CollectionUtils.collect(matchingUsers, new Transformer<User, UserTO>() {

            @Override
            public UserTO transform(final User input) {
                return binder.getUserTO(input, details);
            }
        }, new ArrayList<UserTO>());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    public UserTO selfCreate(final UserTO userTO, final boolean storePassword) {
        return doCreate(userTO, storePassword, true);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    @Override
    public UserTO create(final UserTO userTO) {
        return create(userTO, true);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_CREATE + "')")
    public UserTO create(final UserTO userTO, final boolean storePassword) {
        return doCreate(userTO, storePassword, false);
    }

    protected UserTO doCreate(final UserTO userTO, final boolean storePassword, final boolean self) {
        Pair<UserTO, List<LogicActions>> before = beforeCreate(userTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        if (!self) {
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_CREATE),
                    Collections.singleton(before.getLeft().getRealm()));
            securityChecks(effectiveRealms, before.getLeft().getRealm(), null);
        }

        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(before.getLeft(), storePassword);

        UserTO savedTO = binder.getUserTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());

        return afterCreate(savedTO, before.getValue());
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public UserTO selfUpdate(final UserPatch userPatch) {
        UserTO userTO = binder.getAuthenticatedUserTO();
        userPatch.setKey(userTO.getKey());
        return doUpdate(userPatch, true);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO update(final UserPatch userPatch) {
        return doUpdate(userPatch, false);
    }

    protected UserTO doUpdate(final UserPatch userPatch, final boolean self) {
        UserTO userTO = binder.getUserTO(userPatch.getKey());
        Pair<UserPatch, List<LogicActions>> before = beforeUpdate(userPatch, userTO.getRealm());

        if (!self
                && before.getLeft().getRealm() != null
                && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())) {

            Set<String> requestedRealms = new HashSet<>();
            requestedRealms.add(before.getLeft().getRealm().getValue());
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                    requestedRealms);
            securityChecks(effectiveRealms, before.getLeft().getRealm().getValue(), before.getLeft().getKey());
        }

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(before.getLeft());

        UserTO updatedTO = binder.getUserTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());

        return afterUpdate(updatedTO, before.getRight());
    }

    protected Map.Entry<Long, List<PropagationStatus>> setStatusOnWfAdapter(final StatusPatch statusPatch) {
        Map.Entry<Long, List<PropagationStatus>> updated;

        switch (statusPatch.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(statusPatch);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(statusPatch);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(statusPatch);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    public UserTO status(final StatusPatch statusPatch) {
        // security checks
        UserTO toUpdate = binder.getUserTO(statusPatch.getKey());
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(toUpdate.getRealm()));
        securityChecks(effectiveRealms, toUpdate.getRealm(), toUpdate.getKey());

        Map.Entry<Long, List<PropagationStatus>> updated = setStatusOnWfAdapter(statusPatch);
        UserTO savedTO = binder.getUserTO(updated.getKey());
        savedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.MUST_CHANGE_PASSWORD + "')")
    public UserTO changePassword(final String password) { 
        UserPatch userPatch = new UserPatch();
        userPatch.setPassword(new PasswordPatch.Builder().value(password).build());
        userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(false).build());
        return selfUpdate(userPatch);
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        User user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (syncopeLogic.isPwdResetRequiringSecurityQuestions()
                && (securityAnswer == null || !securityAnswer.equals(user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        provisioningManager.requestPasswordReset(user.getKey());
    }

    @PreAuthorize("isAnonymous() or hasRole('" + Entitlement.ANONYMOUS + "')")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(user.getKey(), token, password);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + Entitlement.ANONYMOUS + "'))")
    public UserTO selfDelete() {
        UserTO userTO = binder.getAuthenticatedUserTO();
        return doDelete(userTO, true);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_DELETE + "')")
    @Override
    public UserTO delete(final Long key) {
        UserTO userTO = binder.getUserTO(key);
        return doDelete(userTO, false);
    }

    protected UserTO doDelete(final UserTO userTO, final boolean self) {
        Pair<UserTO, List<LogicActions>> before = beforeDelete(userTO);

        if (!self) {
            Set<String> effectiveRealms = getEffectiveRealms(
                    AuthContextUtils.getAuthorizations().get(Entitlement.USER_DELETE),
                    Collections.singleton(before.getLeft().getRealm()));
            securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());
        }

        List<Group> ownedGroups = groupDAO.findOwnedByUser(before.getLeft().getKey());
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(CollectionUtils.collect(ownedGroups, new Transformer<Group, String>() {

                @Override
                public String transform(final Group group) {
                    return group.getKey() + " " + group.getName();
                }
            }, new ArrayList<String>()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey());

        UserTO deletedTO;
        if (userDAO.find(before.getLeft().getKey()) == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(before.getLeft().getKey());
        } else {
            deletedTO = binder.getUserTO(before.getLeft().getKey());
        }
        deletedTO.getPropagationStatusTOs().addAll(statuses);

        return afterDelete(deletedTO, before.getRight());
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO unlink(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return binder.getUserTO(provisioningManager.unlink(patch));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO link(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        return binder.getUserTO(provisioningManager.link(patch));
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO unassign(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build();
            }
        }));

        return update(patch);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        UserPatch patch = new UserPatch();
        patch.setKey(key);
        patch.getResources().addAll(CollectionUtils.collect(resources, new Transformer<String, StringPatchItem>() {

            @Override
            public StringPatchItem transform(final String resource) {
                return new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build();
            }
        }));

        if (changepwd) {
            patch.setPassword(new PasswordPatch.Builder().
                    value(password).onSyncope(false).resources(resources).build());
        }

        return update(patch);
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO deprovision(final Long key, final Collection<String> resources) {
        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources);

        UserTO updatedTO = binder.getUserTO(key);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('" + Entitlement.USER_UPDATE + "')")
    @Override
    public UserTO provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        // security checks
        UserTO user = binder.getUserTO(key);
        Set<String> effectiveRealms = getEffectiveRealms(
                AuthContextUtils.getAuthorizations().get(Entitlement.USER_UPDATE),
                Collections.singleton(user.getRealm()));
        securityChecks(effectiveRealms, user.getRealm(), user.getKey());

        user.getPropagationStatusTOs().addAll(provisioningManager.provision(key, changePwd, password, resources));
        return user;
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object key = null;

        if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    key = ((UserTO) args[i]).getKey();
                } else if (args[i] instanceof UserPatch) {
                    key = ((UserPatch) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return key instanceof Long ? binder.getUserTO((Long) key) : binder.getUserTO((String) key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
