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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.BulkActionResult.Status;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Note that this controller does not extend AbstractTransactionalController, hence does not provide any
 * Spring's Transactional logic at class level.
 *
 * @see AbstractTransactionalController
 */
@Component
public class UserController extends AbstractSubjectController<UserTO, UserMod> {

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected SubjectSearchDAO searchDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Transactional(readOnly = true)
    public boolean isSelfRegAllowed() {
        return confDAO.find("selfRegistration.allowed", "false").getValues().get(0).getBooleanValue();
    }

    @Transactional(readOnly = true)
    public boolean isPwdResetAllowed() {
        return confDAO.find("passwordReset.allowed", "false").getValues().get(0).getBooleanValue();
    }

    @Transactional(readOnly = true)
    public boolean isPwdResetRequiringSecurityQuestions() {
        return confDAO.find("passwordReset.securityQuestion", "true").getValues().get(0).getBooleanValue();
    }

    @PreAuthorize("hasRole('USER_READ')")
    public String getUsername(final Long userId) {
        return binder.getUserTO(userId).getUsername();
    }

    @PreAuthorize("hasRole('USER_READ')")
    public Long getUserId(final String username) {
        return binder.getUserTO(username).getId();
    }

    @Transactional(readOnly = true)
    public Date findLastChange(final Long id) {
        Date etag = userDAO.findLastChange(id);
        if (etag == null) {
            throw new NotFoundException("User " + id);
        }
        return etag;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int count() {
        return userDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int searchCount(final SearchCond searchCondition) {
        return searchDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, SubjectType.USER);
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public List<UserTO> list(
            final int page, final int size, final List<OrderByClause> orderBy, final boolean details) {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size, orderBy);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(binder.getUserTO(user, details));
        }

        return userTOs;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public UserTO readSelf() {
        return binder.getAuthenticatedUserTO();
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true)
    @Override
    public UserTO read(final Long userId) {
        return binder.getUserTO(userId);
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true)
    @Override
    public List<UserTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy, final boolean details) {

        final List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy, SubjectType.USER);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(binder.getUserTO(user, details));
        }

        return result;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    public UserTO createSelf(final UserTO userTO, final boolean storePassword) {
        return doCreate(userTO, storePassword);
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    public UserTO create(final UserTO userTO, final boolean storePassword) {
        Set<Long> requestRoleIds = new HashSet<Long>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestRoleIds.add(membership.getRoleId());
        }
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        requestRoleIds.removeAll(adminRoleIds);
        if (!requestRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(requestRoleIds);
        }

        return doCreate(userTO, storePassword);
    }

    protected UserTO doCreate(final UserTO userTO, final boolean storePassword) {
        // Attributable transformation (if configured)
        UserTO actual = attrTransformer.transform(userTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation, notification
         */
        WorkflowResult<Map.Entry<Long, Boolean>> created = uwfAdapter.create(actual, storePassword);

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, actual.getPassword(), actual.getVirAttrs(), actual.getMemberships());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO savedTO = binder.getUserTO(created.getResult().getKey());
        savedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());
        return savedTO;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO updateSelf(final UserMod userMod) {
        UserTO userTO = binder.getAuthenticatedUserTO();

        if (userTO.getId() != userMod.getId()) {
            throw new AccessControlException("Not allowed for user id " + userMod.getId());
        }

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Override
    public UserTO update(final UserMod userMod) {
        // AttributableMod transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        PropagationByResource propByResVirAttr = new PropagationByResource();
        for (Long membershipId : actual.getMembershipsToRemove()) {
            propByResVirAttr.merge(binder.fillMembershipVirtual(
                    null,
                    null,
                    membershipId,
                    Collections.<String>emptySet(),
                    Collections.<AttributeMod>emptySet(),
                    true));
        }

        // Actual operations: workflow, propagation, notification
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = uwfAdapter.update(actual);

        // SYNCOPE-459: take care of user virtual attributes ...
        propByResVirAttr.merge(binder.fillVirtual(
                updated.getResult().getKey().getId(),
                actual.getVirAttrsToRemove(),
                actual.getVirAttrsToUpdate()));
        for (MembershipMod membershipMod : actual.getMembershipsToAdd()) {
            propByResVirAttr.merge(binder.fillMembershipVirtual(
                    updated.getResult().getKey().getId(),
                    membershipMod.getRole(),
                    null,
                    membershipMod.getVirAttrsToRemove(),
                    membershipMod.getVirAttrsToUpdate(),
                    false));
        }
        if (updated.getPropByRes() == null) {
            updated.setPropByRes(propByResVirAttr);
        } else {
            updated.getPropByRes().merge(propByResVirAttr);
        }

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        if (!tasks.isEmpty()) {
            try {
                taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                LOG.error("Error propagation primary resource", e);
                propagationReporter.onPrimaryResourceFailure(tasks);
            }
        }

        UserTO updatedTO = binder.getUserTO(updated.getResult().getKey().getId());
        updatedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());
        return updatedTO;
    }

    protected WorkflowResult<Long> setStatusOnWfAdapter(final SyncopeUser user, final StatusMod statusMod) {
        WorkflowResult<Long> updated;

        switch (statusMod.getType()) {
            case SUSPEND:
                updated = uwfAdapter.suspend(user.getId());
                break;

            case REACTIVATE:
                updated = uwfAdapter.reactivate(user.getId());
                break;

            case ACTIVATE:
            default:
                updated = uwfAdapter.activate(user.getId(), statusMod.getToken());
                break;

        }

        return updated;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public UserTO status(final StatusMod statusMod) {
        SyncopeUser user = binder.getUserFromId(statusMod.getId());

        if (statusMod.isOnSyncope()) {
            setStatusOnWfAdapter(user, statusMod);
        }

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.UPDATE, statusMod.getResourceNames());
        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                user, // SyncopeUser to be updated on external resources
                null, // no password
                false,
                statusMod.getType() != StatusMod.ModType.SUSPEND, // status to be propagated
                Collections.<String>emptySet(), // no virtual attributes to be managed
                Collections.<AttributeMod>emptySet(), // no virtual attributes to be managed
                propByRes,
                null,
                Collections.<MembershipMod>emptySet());
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO savedTO = binder.getUserTO(user.getId());
        savedTO.getPropagationStatusTOs().addAll(propReporter.getStatuses());
        return savedTO;
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional
    public void requestPasswordReset(final String username, final String securityAnswer) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        if (isPwdResetRequiringSecurityQuestions()
                && (securityAnswer == null || !securityAnswer.equals(user.getSecurityAnswer()))) {

            throw SyncopeClientException.build(ClientExceptionType.InvalidSecurityAnswer);
        }

        uwfAdapter.requestPasswordReset(user.getId());
    }

    @PreAuthorize("isAnonymous() or hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional
    public void confirmPasswordReset(final String token, final String password) {
        SyncopeUser user = userDAO.findByToken(token);
        if (user == null) {
            throw new NotFoundException("User with token " + token);
        }

        WorkflowResult<Map.Entry<UserMod, Boolean>> updated =
                uwfAdapter.confirmPasswordReset(user.getId(), token, password);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated);
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    public UserTO deleteSelf() {
        UserTO userTO = binder.getAuthenticatedUserTO();

        return delete(userTO.getId());
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @Override
    public UserTO delete(final Long userId) {
        List<SyncopeRole> ownedRoles = roleDAO.findOwnedByUser(userId);
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (SyncopeRole role : ownedRoles) {
                owned.add(role.getId() + " " + role.getName());
            }

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RoleOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        uwfAdapter.delete(userId);

        final UserTO deletedTO;
        SyncopeUser deleted = userDAO.find(userId);
        if (deleted == null) {
            deletedTO = new UserTO();
            deletedTO.setId(userId);
        } else {
            deletedTO = binder.getUserTO(userId);
        }
        deletedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

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
                for (String userId : bulkAction.getTargets()) {
                    try {
                        res.add(delete(Long.valueOf(userId)).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;

            case SUSPEND:
                for (String userId : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setId(Long.valueOf(userId));
                    statusMod.setType(StatusMod.ModType.SUSPEND);
                    try {
                        res.add(status(statusMod).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing suspend for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;

            case REACTIVATE:
                for (String userId : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setId(Long.valueOf(userId));
                    statusMod.setType(StatusMod.ModType.REACTIVATE);
                    try {
                        res.add(status(statusMod).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing reactivate for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
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
    public UserTO unlink(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToRemove().addAll(resources);
        return binder.getUserTO(uwfAdapter.update(userMod).getResult().getKey().getId());
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO link(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToAdd().addAll(resources);
        return binder.getUserTO(uwfAdapter.update(userMod).getResult().getKey().getId());
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO unassign(final Long userId, final Collection<String> resources) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToRemove().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public UserTO assign(
            final Long userId,
            final Collection<String> resources,
            final boolean changepwd,
            final String password) {
        final UserMod userMod = new UserMod();
        userMod.setId(userId);
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
    public UserTO deprovision(final Long userId, final Collection<String> resources) {
        final SyncopeUser user = binder.getUserFromId(userId);

        final Set<String> noPropResourceName = user.getResourceNames();
        noPropResourceName.removeAll(resources);

        final List<PropagationTask> tasks =
                propagationManager.getUserDeleteTaskIds(userId, new HashSet<String>(resources), noPropResourceName);
        final PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO updatedUserTO = binder.getUserTO(user, true);
        updatedUserTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());
        return updatedUserTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(readOnly = true)
    @Override
    public UserTO provision(
            final Long userId,
            final Collection<String> resources,
            final boolean changePwd,
            final String password) {

        final UserTO original = binder.getUserTO(userId);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(userId, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object id = null;

        if (!"confirmPasswordReset".equals(method.getName()) && ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof String) {
                    id = (String) args[i];
                } else if (args[i] instanceof UserTO) {
                    id = ((UserTO) args[i]).getId();
                } else if (args[i] instanceof UserMod) {
                    id = ((UserMod) args[i]).getId();
                }
            }
        }

        if ((id != null) && !id.equals(0l)) {
            try {
                return id instanceof Long ? binder.getUserTO((Long) id) : binder.getUserTO((String) id);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
