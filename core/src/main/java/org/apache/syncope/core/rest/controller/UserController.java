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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.BulkActionRes.Status;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.UserSubCategory;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend AbstractController, hence does not provide any Spring's Transactional logic
 * at class level.
 *
 * @see AbstractController
 */
@Component
public class UserController {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected AttributableSearchDAO searchDAO;

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

    @Autowired
    protected NotificationManager notificationManager;

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public int count() {
        return userDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {
        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        return searchDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list() {
        List<SyncopeUser> users =
                userDAO.findAll(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));

        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(binder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.list, Result.success,
                "Successfully listed all users: " + userTOs.size());

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list(final int page, final int size) {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(binder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.list, Result.success,
                "Successfully listed all users (page=" + page + ", size=" + size + "): " + userTOs.size());

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(final Long userId) {
        UserTO result = binder.getUserTO(userId);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully read user: " + userId);

        return result;
    }

    @PreAuthorize("#username == authentication.name or hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(final String username) {
        UserTO result = binder.getUserTO(username);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully read user: " + username);

        return result;
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public UserTO read() {
        UserTO userTO = binder.getAuthenticatedUserTO();

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully read own data: " + userTO.getUsername());

        return userTO;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        return search(searchCondition, -1, -1);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeUser> matchingUsers = searchDAO.search(EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition, page, size,
                AttributableUtil.getInstance(AttributableType.USER));

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(binder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully searched for users (page=" + page + ", size=" + size + "): " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    public UserTO create(final UserTO userTO) {
        LOG.debug("User create called with {}", userTO);

        Set<Long> requestRoleIds = new HashSet<Long>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestRoleIds.add(membership.getRoleId());
        }
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        requestRoleIds.removeAll(adminRoleIds);
        if (!requestRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(requestRoleIds);
        }

        // Attributable transformation (if configured)
        UserTO actual = attrTransformer.transform(userTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation, notification
         */

        WorkflowResult<Map.Entry<Long, Boolean>> created = uwfAdapter.create(actual);

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, actual.getPassword(), actual.getVirAttrs());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

        final UserTO savedTO = binder.getUserTO(created.getResult().getKey());
        savedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        LOG.debug("About to return created user\n{}", savedTO);

        auditManager.audit(Category.user, UserSubCategory.create, Result.success,
                "Successfully created user: " + savedTO.getUsername());

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    public UserTO update(final UserMod userMod) {
        LOG.debug("User update called with {}", userMod);

        final String changedPwd = userMod.getPassword();

        // AttributableMod transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        // 1. update password internally only if required
        if (actual.getPwdPropRequest() != null && !actual.getPwdPropRequest().isOnSyncope()) {
            actual.setPassword(null);
        }
        WorkflowResult<Map.Entry<Long, Boolean>> updated = uwfAdapter.update(actual);

        // 2. propagate password update only to requested resources
        List<PropagationTask> tasks = new ArrayList<PropagationTask>();
        if (actual.getPwdPropRequest() == null) {
            // 2a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = propagationManager.getUserUpdateTaskIds(updated, changedPwd,
                    actual.getVirAttrsToRemove(), actual.getVirAttrsToUpdate());
        } else {
            // 2b. generate the propagation task list in two phases: first the ones containing password,
            // the the rest (with no password)
            final PropagationByResource origPropByRes = new PropagationByResource();
            origPropByRes.merge(updated.getPropByRes());

            Set<String> pwdResourceNames = actual.getPwdPropRequest().getResources();
            SyncopeUser user = binder.getUserFromId(updated.getResult().getKey());
            pwdResourceNames.retainAll(user.getResourceNames());
            final PropagationByResource pwdPropByRes = new PropagationByResource();
            pwdPropByRes.addAll(ResourceOperation.UPDATE, pwdResourceNames);
            updated.setPropByRes(pwdPropByRes);

            if (!pwdPropByRes.isEmpty()) {
                Set<String> toBeExcluded = new HashSet<String>(user.getResourceNames());
                toBeExcluded.addAll(actual.getResourcesToAdd());
                toBeExcluded.removeAll(pwdResourceNames);
                tasks.addAll(propagationManager.getUserUpdateTaskIds(
                        updated,
                        changedPwd,
                        actual.getVirAttrsToRemove(),
                        actual.getVirAttrsToUpdate(),
                        toBeExcluded));
            }

            final PropagationByResource nonPwdPropByRes = new PropagationByResource();
            nonPwdPropByRes.merge(origPropByRes);
            nonPwdPropByRes.removeAll(pwdResourceNames);
            nonPwdPropByRes.purge();
            updated.setPropByRes(nonPwdPropByRes);

            if (!nonPwdPropByRes.isEmpty()) {
                tasks.addAll(propagationManager.getUserUpdateTaskIds(
                        updated,
                        null,
                        actual.getVirAttrsToRemove(),
                        actual.getVirAttrsToUpdate(),
                        pwdResourceNames));
            }

            updated.setPropByRes(origPropByRes);
        }

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        // 3. create notification tasks
        notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

        // 4. prepare result, including propagation status on external resources
        final UserTO updatedTO = binder.getUserTO(updated.getResult().getKey());
        updatedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        auditManager.audit(Category.user, UserSubCategory.update, Result.success,
                "Successfully updated user: " + updatedTO.getUsername());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(final Long userId, final String token) {
        return activate(userId, token, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(final Long userId, final String token, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to activate " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, token, propagationRequestTO, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(final String username, final String token) {
        return activate(username, token, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(final String username, final String token, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to activate " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, token, propagationRequestTO, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(final Long userId) {
        return suspend(userId, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(final Long userId, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to suspend " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, null, propagationRequestTO, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(final String username) {
        return suspend(username, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(final String username, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to suspend " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, null, propagationRequestTO, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final Long userId) {
        return reactivate(userId, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final Long userId, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, null, propagationRequestTO, true, "reactivate");
    }

    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final String username) {
        return reactivate(username, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final String username, final PropagationRequestTO propagationRequestTO) {
        LOG.debug("About to reactivate " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, null, propagationRequestTO, true, "reactivate");
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    public UserTO delete(final Long userId) {
        LOG.debug("User delete called with {}", userId);

        return doDelete(userId);
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    public UserTO delete(final String username) {
        LOG.debug("User delete called with {}", username);

        UserTO result = binder.getUserTO(username);
        long userId = result.getId();

        return doDelete(userId);
    }

    protected UserTO setStatus(final SyncopeUser user, final String token,
            final PropagationRequestTO propagationRequestTO, final boolean status, final String task) {

        LOG.debug("About to set 'enabled:{}' status to {}", user, status);

        WorkflowResult<Long> updated;
        if (propagationRequestTO == null || propagationRequestTO.isOnSyncope()) {
            updated = setStatusOnWfAdapter(user, token, task);
        } else {
            updated = new WorkflowResult<Long>(user.getId(), null, task);
        }

        // Resources to exclude from propagation
        Set<String> resourcesToBeExcluded = new HashSet<String>(user.getResourceNames());
        if (propagationRequestTO != null) {
            resourcesToBeExcluded.removeAll(propagationRequestTO.getResources());
        }

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(user, status, resourcesToBeExcluded);
        taskExecutor.execute(tasks);

        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = binder.getUserTO(updated.getResult());

        auditManager.audit(Category.user, UserSubCategory.setStatus, Result.success,
                "Successfully changed status to " + savedTO.getStatus() + " for user: " + savedTO.getUsername());

        LOG.debug("About to return updated user\n{}", savedTO);

        return savedTO;
    }

    protected WorkflowResult<Long> setStatusOnWfAdapter(final SyncopeUser user, final String token, final String task) {
        WorkflowResult<Long> updated;
        if ("suspend".equals(task)) {
            updated = uwfAdapter.suspend(user.getId());
        } else if ("reactivate".equals(task)) {
            updated = uwfAdapter.reactivate(user.getId());
        } else {
            updated = uwfAdapter.activate(user.getId(), token);
        }
        return updated;
    }

    protected UserTO doDelete(final Long userId) {
        List<SyncopeRole> ownedRoles = roleDAO.findOwned(binder.getUserFromId(userId));
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (SyncopeRole role : ownedRoles) {
                owned.add(role.getId() + " " + role.getName());
            }

            auditManager.audit(Category.user, UserSubCategory.delete, Result.failure,
                    "Could not delete user: " + userId + " because of role(s) ownership " + owned);

            SyncopeClientCompositeException sccee =
                    new SyncopeClientCompositeException(Response.Status.BAD_REQUEST.getStatusCode());

            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.RoleOwnership);
            sce.setElements(owned);
            sccee.addException(sce);

            throw sccee;
        }

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        notificationManager.createTasks(userId, Collections.singleton("delete"));

        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId);

        final UserTO userTO = new UserTO();
        userTO.setId(userId);

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        userTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        uwfAdapter.delete(userId);

        auditManager.audit(Category.user, UserSubCategory.delete, Result.success,
                "Successfully deleted user: " + userId);

        LOG.debug("User successfully deleted: {}", userId);

        return userTO;
    }

    @PreAuthorize("(hasRole('USER_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('USER_UPDATE') and "
            + "(#bulkAction.operation == #bulkAction.operation.REACTIVATE or "
            + "#bulkAction.operation == #bulkAction.operation.SUSPEND))")
    public BulkActionRes bulkAction(final BulkAction bulkAction) {
        LOG.debug("Bulk action '{}' called on '{}'", bulkAction.getOperation(), bulkAction.getTargets());

        BulkActionRes res = new BulkActionRes();

        switch (bulkAction.getOperation()) {
            case DELETE:
                for (String userId : bulkAction.getTargets()) {
                    try {
                        res.add(doDelete(Long.valueOf(userId)).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;
            case SUSPEND:
                for (String userId : bulkAction.getTargets()) {
                    try {
                        res.add(suspend(Long.valueOf(userId)).getId(), Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing suspend for user {}", userId, e);
                        res.add(userId, Status.FAILURE);
                    }
                }
                break;
            case REACTIVATE:
                for (String userId : bulkAction.getTargets()) {
                    try {
                        res.add(reactivate(Long.valueOf(userId)).getId(), Status.SUCCESS);
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
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO unlink(final Long userId, final Collection<String> resources) {
        LOG.debug("About to unlink user({}) and resources {}", userId, resources);

        final UserMod userMod = new UserMod();
        userMod.setId(userId);

        userMod.getResourcesToRemove().addAll(resources);

        final WorkflowResult<Map.Entry<Long, Boolean>> updated = uwfAdapter.update(userMod);

        final UserTO updatedTO = binder.getUserTO(updated.getResult().getKey());

        auditManager.audit(Category.user, UserSubCategory.update, Result.success,
                "Successfully updated user: " + updatedTO.getUsername());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO unassign(final Long userId, final Collection<String> resources) {
        LOG.debug("About to unassign user({}) and resources {}", userId, resources);

        final UserMod userMod = new UserMod();
        userMod.setId(userId);
        userMod.getResourcesToRemove().addAll(resources);

        return update(userMod);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO deprovision(final Long userId, final Collection<String> resources) {
        LOG.debug("About to deprovision user({}) from resources {}", userId, resources);

        final SyncopeUser user = binder.getUserFromId(userId);

        final Set<String> noPropResourceName = user.getResourceNames();
        noPropResourceName.removeAll(resources);

        final List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId, noPropResourceName);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO updatedUserTO = binder.getUserTO(user);
        updatedUserTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        auditManager.audit(Category.user, UserSubCategory.update, Result.success,
                "Successfully deprovisioned user: " + updatedUserTO.getUsername());

        LOG.debug("About to return updated user\n{}", updatedUserTO);

        return updatedUserTO;
    }
}
