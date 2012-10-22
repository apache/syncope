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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.PropagationTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.to.WorkflowFormTO;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.UserSearchDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationHandler;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.UserSubCategory;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Note that this controller does not extend AbstractController, hence does not provide any Spring's Transactional logic
 * at class level.
 *
 * @see AbstractController
 */
@Controller
@RequestMapping("/user")
public class UserController {

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
    private NotificationManager notificationManager;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/verifyPassword/{username}")
    @Transactional(readOnly = true)
    public ModelAndView verifyPassword(@PathVariable("username") String username,
            @RequestParam("password") final String password)
            throws NotFoundException, UnauthorizedRoleException {

        auditManager.audit(Category.user, UserSubCategory.create, Result.success,
                "Verified password for: " + username);

        return new ModelAndView().addObject(userDataBinder.verifyPassword(username, password));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/count")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public ModelAndView count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(userDAO.count(adminRoleIds));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search/count")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public ModelAndView searchCount(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(searchDAO.count(adminRoleIds, searchCondition));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list() {
        List<SyncopeUser> users =
                userDAO.findAll(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));

        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.list, Result.success,
                "Successfully listed all users: " + userTOs.size());

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list(@PathVariable("page") final int page, @PathVariable("size") final int size) {

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

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{userId}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable("userId") final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        UserTO result = userDataBinder.getUserTO(userId);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully read user: " + userId);

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/readByUsername/{username}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable final String username)
            throws NotFoundException, UnauthorizedRoleException {

        UserTO result = userDataBinder.getUserTO(username);

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully read user: " + username);

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers = searchDAO.search(EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully searched for users: " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(@RequestBody final NodeCond searchCondition, @PathVariable("page") final int page,
            @PathVariable("size") final int size)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeUser> matchingUsers = searchDAO.search(EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition, page, size);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user));
        }

        auditManager.audit(Category.user, UserSubCategory.read, Result.success,
                "Successfully searched for users (page=" + page + ", size=" + size + "): " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public UserTO create(final HttpServletResponse response, @RequestBody final UserTO userTO)
            throws PropagationException, UnauthorizedRoleException, WorkflowException, NotFoundException {

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

        List<PropagationTask> tasks = propagationManager.getCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirtualAttributes());

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();

        propagationManager.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject before, final ConnectorObject after) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (before != null) {
                    propagation.setBefore(connObjectUtil.getConnObjectTO(before));
                }

                if (after != null) {
                    propagation.setAfter(connObjectUtil.getConnObjectTO(after));
                }

                propagations.add(propagation);
            }
        });

        notificationManager.createTasks(created.getResult().getKey(), created.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(created.getResult().getKey());
        savedTO.setPropagationTOs(propagations);

        LOG.debug("About to return created user\n{}", savedTO);

        auditManager.audit(Category.user, UserSubCategory.create, Result.success,
                "Successfully created user: " + savedTO.getUsername());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public UserTO update(@RequestBody final UserMod userMod)
            throws NotFoundException, PropagationException, UnauthorizedRoleException, WorkflowException {

        LOG.debug("User update called with {}", userMod);

        WorkflowResult<Map.Entry<Long, Boolean>> updated = wfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(updated, userMod.getPassword(), userMod.
                getVirtualAttributesToBeRemoved(), userMod.getVirtualAttributesToBeUpdated(), null);

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();

        propagationManager.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject before, final ConnectorObject after) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (before != null) {
                    propagation.setBefore(connObjectUtil.getConnObjectTO(before));
                }

                if (after != null) {
                    propagation.setAfter(connObjectUtil.getConnObjectTO(after));
                }

                propagations.add(propagation);
            }
        });

        notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());

        final UserTO updatedTO = userDataBinder.getUserTO(updated.getResult().getKey());
        updatedTO.setPropagationTOs(propagations);

        auditManager.audit(Category.user, UserSubCategory.update, Result.success,
                "Successfully updated user: " + updatedTO.getUsername());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/activate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(
            @PathVariable("userId") final Long userId,
            @RequestParam(required = true) final String token,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to activate " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, token, resourceNames, performLocally, performRemotely, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/activateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(
            @PathVariable("username") final String username,
            @RequestParam(required = true) final String token,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to activate " + username);

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, token, resourceNames, performLocally, performRemotely, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/suspendByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("username") final String username,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + username);

        SyncopeUser user = userDAO.find(username);

        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/suspend/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("userId") final Long userId,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/reactivate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final @PathVariable("userId") Long userId,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, true, "reactivate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/reactivateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final @PathVariable("username") String username,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true") final Boolean performLocally,
            @RequestParam(required = false, defaultValue = "true") final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + username);

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        return setStatus(user, null, resourceNames, performLocally, performRemotely, true, "reactivate");
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{userId}")
    public UserTO delete(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException, PropagationException, UnauthorizedRoleException {
        LOG.debug("User delete called with {}", userId);

        return deleteByUserId(userId);
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/deleteByUsername/{username}")
    public UserTO delete(@PathVariable final String username)
            throws NotFoundException, WorkflowException, PropagationException, UnauthorizedRoleException {
        LOG.debug("User delete called with {}", username);

        UserTO result = userDataBinder.getUserTO(username);
        long userId = result.getId();

        return deleteByUserId(userId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/execute/workflow/{taskId}")
    public UserTO executeWorkflow(@RequestBody final UserTO userTO, @PathVariable("taskId") final String taskId)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException {

        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        WorkflowResult<Long> updated = wfAdapter.execute(userTO, taskId);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(new WorkflowResult<Map.Entry<Long, Boolean>>(
                new DefaultMapEntry(updated.getResult(), null), updated.getPropByRes(), updated.getPerformedTasks()));

        propagationManager.execute(tasks);

        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", savedTO);

        auditManager.audit(Category.user, UserSubCategory.executeWorkflow, Result.success,
                "Successfully executed workflow action " + taskId + " on user: " + userTO.getUsername());

        return savedTO;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/list")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = wfAdapter.getForms();

        auditManager.audit(Category.user, UserSubCategory.getForms, Result.success,
                "Successfully list workflow forms: " + forms.size());

        return forms;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public WorkflowFormTO getFormForUser(@PathVariable("userId") final Long userId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException {

        SyncopeUser user = userDataBinder.getUserFromId(userId);
        WorkflowFormTO result = wfAdapter.getForm(user.getWorkflowId());

        auditManager.audit(Category.user, UserSubCategory.getFormForUser, Result.success,
                "Successfully read workflow form for user: " + user.getUsername());

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/claim/{taskId}")
    @Transactional(rollbackFor = {Throwable.class})
    public WorkflowFormTO claimForm(@PathVariable("taskId") final String taskId)
            throws NotFoundException, WorkflowException {

        WorkflowFormTO result = wfAdapter.claimForm(taskId,
                SecurityContextHolder.getContext().getAuthentication().getName());

        auditManager.audit(Category.user, UserSubCategory.claimForm, Result.success,
                "Successfully claimed workflow form: " + taskId);

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @RequestMapping(method = RequestMethod.POST, value = "/workflow/form/submit")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO submitForm(@RequestBody final WorkflowFormTO form)
            throws NotFoundException, WorkflowException, PropagationException, UnauthorizedRoleException {

        LOG.debug("About to process form {}", form);

        WorkflowResult<Map.Entry<Long, String>> updated = wfAdapter.submitForm(form, SecurityContextHolder.getContext().
                getAuthentication().getName());

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(new WorkflowResult<Map.Entry<Long, Boolean>>(
                new DefaultMapEntry(updated.getResult().getKey(), Boolean.TRUE), updated.getPropByRes(), updated.
                getPerformedTasks()), updated.getResult().getValue(), null, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult().getKey());

        auditManager.audit(Category.user, UserSubCategory.submitForm, Result.success,
                "Successfully submitted workflow form for user: " + savedTO.getUsername());

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }

    private UserTO setStatus(final SyncopeUser user, final String token, final Set<String> resourceNames,
            final boolean performLocally, final boolean performRemotely, final boolean status, final String task)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException {

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

        propagationManager.execute(tasks);
        notificationManager.createTasks(updated.getResult(), updated.getPerformedTasks());

        final UserTO savedTO = userDataBinder.getUserTO(updated.getResult());

        auditManager.audit(Category.user, UserSubCategory.setStatus, Result.success,
                "Successfully changed status to " + savedTO.getStatus() + " for user: " + savedTO.getUsername());

        LOG.debug("About to return updated user\n{}", savedTO);

        return savedTO;
    }

    private UserTO deleteByUserId(final Long userId)
            throws NotFoundException, WorkflowException, PropagationException, UnauthorizedRoleException {
        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after wfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        notificationManager.createTasks(userId, Collections.singleton("delete"));

        List<PropagationTask> tasks = propagationManager.getDeleteTaskIds(userId);

        final UserTO userTO = new UserTO();
        userTO.setId(userId);

        propagationManager.execute(tasks, new PropagationHandler() {

            @Override
            public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
                    final ConnectorObject before, final ConnectorObject after) {

                final PropagationTO propagation = new PropagationTO();
                propagation.setResourceName(resourceName);
                propagation.setStatus(executionStatus);

                if (before != null) {
                    propagation.setBefore(connObjectUtil.getConnObjectTO(before));
                }

                if (after != null) {
                    propagation.setAfter(connObjectUtil.getConnObjectTO(after));
                }

                userTO.addPropagationTO(propagation);
            }
        });

        wfAdapter.delete(userId);

        auditManager.audit(Category.user, UserSubCategory.delete, Result.success,
                "Successfully deleted user: " + userTO.getUsername());

        LOG.debug("User successfully deleted: {}", userId);

        return userTO;
    }
}
