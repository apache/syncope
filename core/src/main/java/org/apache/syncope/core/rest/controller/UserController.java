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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.UserSearchDAO;
import org.apache.syncope.core.propagation.PropagationHandler;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.propagation.PropagationException;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.services.ContextAware;
import org.apache.syncope.services.InvalidSearchConditionException;
import org.apache.syncope.services.UnauthorizedRoleException;
import org.apache.syncope.services.UserService;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.PropagationTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.to.WorkflowFormTO;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.UserSubCategory;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.workflow.WorkflowException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Note that this controller does not extend AbstractController, hence does not
 * provide any Spring's Transactional logic at class level.
 *
 * @see AbstractController
 */
@Service
public class UserController implements UserService, ContextAware {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO searchDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private UserWorkflowAdapter wfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationManager notificationManager;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    private UriInfo uriInfo;

    @Override
    public Boolean verifyPassword(final String username, final String password) {

        auditManager.audit(Category.user, UserSubCategory.create, Result.success, "Verified password for: "
                + username);

        boolean authenticated;
        try {
            authenticated = userDataBinder.verifyPassword(username, password);
        } catch (Exception e) {
            return Boolean.FALSE;
        }

        return Boolean.valueOf(authenticated);
    }

    @Override
    public int count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        return userDAO.count(adminRoleIds);
    }

    @Override
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        return searchDAO.count(adminRoleIds, searchCondition);
    }

    @Override
    public List<UserTO> list() {
        List<SyncopeUser> users = userDAO.findAll(EntitlementUtil.getRoleIds(EntitlementUtil
                .getOwnedEntitlementNames()));

        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.list, Result.success,
                "Successfully listed all users: " + userTOs.size());

        return userTOs;
    }

    @Override
    public List<UserTO> list(final int page, final int size) {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.list, Result.success,
                "Successfully listed all users (page=" + page + ", size=" + size + "): " + userTOs.size());

        return userTOs;
    }

    @Override
    public UserTO read(final Long userId) throws NotFoundException, UnauthorizedRoleException {

        UserTO result = userDataBinder.getUserTO(userId);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success, "Successfully read user: "
                + userId);

        return result;
    }

    @Override
    public UserTO read(final String username) throws NotFoundException, UnauthorizedRoleException {

        UserTO result = userDataBinder.getUserTO(username);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success, "Successfully read user: "
                + username);

        return result;
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()), searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully searched for users: " + result.size());

        return result;
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()), searchCondition,
                page, size);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully searched for users (page=" + page + ", size=" + size + "): " + result.size());

        return result;
    }

    @Override
    public Response create(final UserTO userTO) throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {

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

        WorkflowResult<Map.Entry<Long, Boolean>> created = wfAdapter.create(userTO);

        List<PropagationTask> tasks = propagationManager.getCreateTaskIds(created, userTO.getPassword(),
                userTO.getVirtualAttributes());

        //final List<PropagationTO> propagations = new ArrayList<PropagationTO>();

        taskExecutor.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject beforeObj, final ConnectorObject afterObj) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (beforeObj != null) {
                    propagation.setBeforeObj(connObjectUtil.getConnObjectTO(beforeObj));
                }

                if (afterObj != null) {
                    propagation.setAfterObj(connObjectUtil.getConnObjectTO(afterObj));
                }

               // propagations.add(propagation);
            }
        });

        notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(created.getResult().getKey());
        //savedTO.setPropagationTOs(propagations);

        LOG.debug("About to return created user\n{}", savedTO);

        auditManager.audit(Category.user, UserSubCategory.create, Result.success,
                "Successfully created user: " + savedTO.getUsername());

        URI response = uriInfo.getAbsolutePathBuilder().path(created.getResult().getKey().toString()).build();
        return Response.created(response).build();
    }

    @Override
    public UserTO update(final Long userId, final UserMod userMod) throws NotFoundException, PropagationException,
            UnauthorizedRoleException, WorkflowException {

        userMod.setId(userId);

        LOG.debug("User update called with {}", userMod);

        WorkflowResult<Map.Entry<Long, Boolean>> updated = wfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(updated, userMod.getPassword(),
                userMod.getVirtualAttributesToBeRemoved(), userMod.getVirtualAttributesToBeUpdated(), null);

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();

        taskExecutor.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject before, final ConnectorObject after) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (before != null) {
                    propagation.setBeforeObj(connObjectUtil.getConnObjectTO(before));
                }

                if (after != null) {
                    propagation.setAfterObj(connObjectUtil.getConnObjectTO(after));
                }

                propagations.add(propagation);
            }
        });

        notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

        final UserTO updatedTO = userDataBinder.getUserTO(updated.getResult().getKey());
        updatedTO.getPropagationTOs().addAll(propagations);

        auditManager.audit(Category.user, UserSubCategory.update, Result.success,
                "Successfully updated user: " + updatedTO.getUsername());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @Override
    public UserTO activate(final Long userId, final String token, final Set<String> resourceNames,
            final Boolean performLocally, final Boolean performRemotely) throws WorkflowException,
            NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to activate " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, token, resourceNames, performLocally, performRemotely, true, "activate");
    }

    @Deprecated
    public UserTO activate(final String username, final String token,
            final Set<String> resourceNames, final Boolean performLocally, final Boolean performRemotely)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to activate " + username);

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, token, resourceNames, performLocally, performRemotely, true, "activate");
    }

    @Deprecated
    public UserTO suspend(final String username, final Set<String> resourceNames,
            final Boolean performLocally, final Boolean performRemotely) throws NotFoundException,
            WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + username);

        SyncopeUser user = userDAO.find(username);

        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, false, "suspend");
    }

    @Override
    public UserTO suspend(final Long userId, final Set<String> resourceNames, final Boolean performLocally,
            final Boolean performRemotely) throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, false, "suspend");
    }

    @Deprecated
    public UserTO reactivate(Long userId, final Set<String> resourceNames,
            final Boolean performLocally,
            final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, true, "reactivate");
    }

    @Deprecated
    public UserTO reactivate(String username,
            final Set<String> resourceNames,
            final Boolean performLocally,
            final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + username);

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, true, "reactivate");
    }

    @Override
    public Response delete(final Long userId) throws NotFoundException, WorkflowException,
            PropagationException, UnauthorizedRoleException {
        LOG.debug("User delete called with {}", userId);

        doDelete(userId);

        return Response.ok().build();
    }

    @Deprecated
    public UserTO delete(final String username) throws NotFoundException, WorkflowException,
            PropagationException, UnauthorizedRoleException {
        LOG.debug("User delete called with {}", username);

        UserTO result = userDataBinder.getUserTO(username);
        long userId = result.getId();

        doDelete(userId);

        UserTO response = new UserTO();
        response.setId(userId);
        return response;
    }

    @Override
    public UserTO executeWorkflow(final String taskId, final UserTO userTO) throws WorkflowException,
            NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        WorkflowResult<Long> updated = wfAdapter.execute(userTO, taskId);

        List<PropagationTask> tasks = propagationManager
                .getUpdateTaskIds(new WorkflowResult<Map.Entry<Long, Boolean>>(new DefaultMapEntry(updated
                        .getResult(), null), updated.getPropByRes(), updated.getPerformedTasks()));

        taskExecutor.execute(tasks);

        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", savedTO);

        auditManager.audit(Category.user, UserSubCategory.executeWorkflow, Result.success,
                "Successfully executed workflow action " + taskId + " on user: " + userTO.getUsername());

        return savedTO;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = wfAdapter.getForms();

        auditManager.audit(Category.user, UserSubCategory.getForms, Result.success,
                "Successfully list workflow forms: " + forms.size());

        return forms;
    }

    @Override
    public WorkflowFormTO getFormForUser(final Long userId) throws UnauthorizedRoleException,
            NotFoundException, WorkflowException {

        SyncopeUser user = userDataBinder.getUserFromId(userId);
        WorkflowFormTO result = wfAdapter.getForm(user.getWorkflowId());

        auditManager.audit(Category.user, UserSubCategory.getFormForUser, Result.success,
                "Successfully read workflow form for user: " + user.getUsername());

        return result;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId) throws NotFoundException, WorkflowException {

        WorkflowFormTO result = wfAdapter.claimForm(taskId, SecurityContextHolder.getContext()
                .getAuthentication().getName());

        auditManager.audit(Category.user, UserSubCategory.claimForm, Result.success,
                "Successfully claimed workflow form: " + taskId);

        return result;
    }

    @Override
    public UserTO submitForm(final WorkflowFormTO form) throws NotFoundException, WorkflowException,
            PropagationException, UnauthorizedRoleException {

        LOG.debug("About to process form {}", form);

        WorkflowResult<Map.Entry<Long, String>> updated = wfAdapter.submitForm(form, SecurityContextHolder
                .getContext().getAuthentication().getName());

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                new WorkflowResult<Map.Entry<Long, Boolean>>(new DefaultMapEntry(
                        updated.getResult().getKey(), Boolean.TRUE), updated.getPropByRes(), updated
                        .getPerformedTasks()), updated.getResult().getValue(), null, null);
        taskExecutor.execute(tasks);

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult().getKey());

        auditManager.audit(Category.user, UserSubCategory.submitForm, Result.success,
                "Successfully submitted workflow form for user: " + savedTO.getUsername());

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }

    private UserTO setStatus(final SyncopeUser user, final String token, final Set<String> resourceNames,
            final boolean performLocally, final boolean performRemotely, final boolean status,
            final String task) throws NotFoundException, WorkflowException, UnauthorizedRoleException,
            PropagationException {

        LOG.debug("About to set status of {}" + user);

        WorkflowResult<Long> updated;
        if (performLocally) {
            if ("suspend".equals(task)) {
                updated = wfAdapter.suspend(user.getId());
            } else if ("reactivate".equals(task)) {
                updated = wfAdapter.reactivate(user.getId());
            } else {
                updated = wfAdapter.activate(user.getId(), token);
            }
        } else {
            updated = new WorkflowResult<Long>(user.getId(), null, task);
        }

        // Resources to exclude from propagation.
        Set<String> resources = new HashSet<String>();
        if (performRemotely) {
            if (resourceNames != null) {
                resources.addAll(user.getResourceNames());
                resources.removeAll(resourceNames);
            }
        } else {
            resources.addAll(user.getResourceNames());
        }

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(user, status, resources);

        taskExecutor.execute(tasks);
        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());

        auditManager.audit(
                Category.user,
                UserSubCategory.setStatus,
                Result.success,
                "Successfully changed status to " + savedTO.getStatus() + " for user: "
                        + savedTO.getUsername());

        LOG.debug("About to return updated user\n{}", savedTO);

        return savedTO;
    }

    protected void doDelete(final Long userId) throws NotFoundException, WorkflowException,
            PropagationException, UnauthorizedRoleException {
        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after wfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        notificationManager.createTasks(userId, Collections.singleton("delete"));

        List<PropagationTask> tasks = propagationManager.getDeleteTaskIds(userId);

        //final UserTO userTO = new UserTO();
        //userTO.setId(userId);

        taskExecutor.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject before, final ConnectorObject after) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (before != null) {
                    propagation.setBeforeObj(connObjectUtil.getConnObjectTO(before));
                }

                if (after != null) {
                    propagation.setAfterObj(connObjectUtil.getConnObjectTO(after));
                }

                //userTO.addPropagationTO(propagation);
            }
        });

        wfAdapter.delete(userId);

        auditManager.audit(Category.user, UserSubCategory.delete, Result.success,
                "Successfully deleted userId: " + userId);

        LOG.debug("User successfully deleted: {}", userId);
    }

    @Override
    public void setUriInfo(UriInfo ui) {
        this.uriInfo = ui;
    }
}
