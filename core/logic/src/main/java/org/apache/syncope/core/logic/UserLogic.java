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

import org.apache.syncope.core.misc.security.UnauthorizedGroupException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.BulkActionResult.Status;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AttributableTransformer;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.VirAttrHandler;
import org.apache.syncope.core.misc.security.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class UserLogic extends AbstractSubjectLogic<UserTO, UserMod> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected SubjectSearchDAO searchDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Autowired
    protected UserProvisioningManager provisioningManager;

    @Autowired
    protected SyncopeLogic syncopeLogic;

    @PreAuthorize("hasRole('USER_READ')")
    public String getUsername(final Long key) {
        return binder.getUserTO(key).getUsername();
    }

    @PreAuthorize("hasRole('USER_READ')")
    public Long getKey(final String username) {
        return binder.getUserTO(username).getKey();
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int count() {
        return userDAO.count(GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames()));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int searchCount(final SearchCond searchCondition) {
        return searchDAO.count(GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames()),
                searchCondition, SubjectType.USER);
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public List<UserTO> list(final int page, final int size, final List<OrderByClause> orderBy) {
        Set<Long> adminGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());

        List<User> users = userDAO.findAll(adminGroupIds, page, size, orderBy);
        List<UserTO> userTOs = new ArrayList<>(users.size());
        for (User user : users) {
            userTOs.add(binder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public UserTO readSelf() {
        return binder.getAuthenticatedUserTO();
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final Long key) {
        return binder.getUserTO(key);
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy) {

        final List<User> matchingUsers = searchDAO.search(GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy, SubjectType.USER);

        final List<UserTO> result = new ArrayList<>(matchingUsers.size());
        for (User user : matchingUsers) {
            result.add(binder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    public UserTO createSelf(final UserTO userTO, final boolean storePassword) {
        return doCreate(userTO, storePassword);
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    public UserTO create(final UserTO userTO, final boolean storePassword) {
        Set<Long> requestGroupIds = new HashSet<>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestGroupIds.add(membership.getGroupId());
        }
        Set<Long> adminGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        requestGroupIds.removeAll(adminGroupIds);
        if (!requestGroupIds.isEmpty()) {
            throw new UnauthorizedGroupException(requestGroupIds);
        }

        return doCreate(userTO, storePassword);
    }

    protected UserTO doCreate(final UserTO userTO, final boolean storePassword) {
        // Attributable transformation (if configured)
        UserTO actual = attrTransformer.transform(userTO);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(actual, storePassword);

        final UserTO savedTO = binder.getUserTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO updateSelf(final UserMod userMod) {
        UserTO userTO = binder.getAuthenticatedUserTO();

        if (userTO.getKey() != userMod.getKey()) {
            throw new AccessControlException("Not allowed for user with key " + userMod.getKey());
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Override
    public UserTO update(final UserMod userMod) {
        // AttributableMod transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        // SYNCOPE-501: check if there are memberships to be removed with virtual attributes assigned
        boolean removeMemberships = false;
        for (Long membershipId : actual.getMembershipsToRemove()) {
            if (!virtAttrHandler.fillMembershipVirtual(
                    null,
                    null,
                    membershipId,
                    Collections.<String>emptySet(),
                    Collections.<AttrMod>emptySet(),
                    true).isEmpty()) {

                removeMemberships = true;
            }
        }

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(actual, removeMemberships);

        final UserTO updatedTO = binder.getUserTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    protected Map.Entry<Long, List<PropagationStatus>> setStatusOnWfAdapter(final User user,
            final StatusMod statusMod) {
        Map.Entry<Long, List<PropagationStatus>> updated;

        switch (statusMod.getType()) {
            case SUSPEND:
                updated = provisioningManager.suspend(user, statusMod);
                break;

            case REACTIVATE:
                updated = provisioningManager.reactivate(user, statusMod);
                break;

            case ACTIVATE:
            default:
                updated = provisioningManager.activate(user, statusMod);
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO status(final StatusMod statusMod) {
        User user = userDAO.authFetch(statusMod.getKey());

        Map.Entry<Long, List<PropagationStatus>> updated = setStatusOnWfAdapter(user, statusMod);
        final UserTO savedTO = binder.getUserTO(updated.getKey());
        savedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return savedTO;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
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

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        User user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }
        provisioningManager.confirmPasswordReset(user, token, password);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO deleteSelf() {
        UserTO userTO = binder.getAuthenticatedUserTO();

        return delete(userTO.getKey());
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @Override
    public UserTO delete(final Long key) {
        List<Group> ownedGroups = groupDAO.findOwnedByUser(key);
        if (!ownedGroups.isEmpty()) {
            List<String> owned = new ArrayList<>(ownedGroups.size());
            for (Group group : ownedGroups) {
                owned.add(group.getKey() + " " + group.getName());
            }

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(key);

        final UserTO deletedTO;
        User deleted = userDAO.find(key);
        if (deleted == null) {
            deletedTO = new UserTO();
            deletedTO.setKey(key);
        } else {
            deletedTO = binder.getUserTO(key);
        }
        deletedTO.getPropagationStatusTOs().addAll(statuses);

        return deletedTO;
    }

    @PreAuthorize("(hasRole('USER_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('USER_UPDATE') and "
            + "(#bulkAction.operation == #bulkAction.operation.REACTIVATE or "
            + "#bulkAction.operation == #bulkAction.operation.SUSPEND))")
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        switch (bulkAction.getOperation()) {
            case DELETE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        res.add(delete(Long.valueOf(key)).getKey(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", key, e);
                        res.add(key, Status.FAILURE);
                    }
                }
                break;

            case SUSPEND:
                for (String key : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setKey(Long.valueOf(key));
                    statusMod.setType(StatusMod.ModType.SUSPEND);
                    try {
                        res.add(status(statusMod).getKey(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing suspend for user {}", key, e);
                        res.add(key, Status.FAILURE);
                    }
                }
                break;

            case REACTIVATE:
                for (String key : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setKey(Long.valueOf(key));
                    statusMod.setType(StatusMod.ModType.REACTIVATE);
                    try {
                        res.add(status(statusMod).getKey(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing reactivate for user {}", key, e);
                        res.add(key, Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return res;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO unlink(final Long key, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToRemove().addAll(resources);
        Long updatedId = provisioningManager.unlink(userMod);

        return binder.getUserTO(updatedId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO link(final Long key, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToAdd().addAll(resources);
        return binder.getUserTO(provisioningManager.link(userMod));
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO unassign(final Long key, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToRemove().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO assign(
            final Long key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {

        final UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToAdd().addAll(resources);

        if (changepwd) {
            StatusMod statusMod = new StatusMod();
            statusMod.setOnSyncope(false);
            statusMod.getResourceNames().addAll(resources);
            userMod.setPwdPropRequest(statusMod);
            userMod.setPassword(password);
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO deprovision(final Long key, final Collection<String> resources) {
        final User user = userDAO.authFetch(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources);

        final UserTO updatedUserTO = binder.getUserTO(user);
        updatedUserTO.getPropagationStatusTOs().addAll(statuses);
        return updatedUserTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(readOnly = true)
    @Override
    public UserTO provision(
            final Long key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        final UserTO original = binder.getUserTO(key);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(key, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
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
                } else if (args[i] instanceof UserMod) {
                    key = ((UserMod) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0l)) {
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
