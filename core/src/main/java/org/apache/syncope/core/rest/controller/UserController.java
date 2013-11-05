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

import static org.apache.syncope.core.rest.controller.AbstractController.LOG;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.BulkActionRes.Status;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
public class UserController extends AbstractController<UserTO> {

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

    @RequestMapping(method = RequestMethod.GET, value = "/verifyPassword/{username}")
    public ModelAndView verifyPassword(@PathVariable("username") String username,
            @RequestParam("password") final String password) {

        return new ModelAndView().addObject(verifyPasswordInternal(username, password));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true)
    public Boolean verifyPasswordInternal(final String username, final String password) {
        return binder.verifyPassword(username, password);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/count")
    public ModelAndView count() {
        return new ModelAndView().addObject(countInternal());
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public int countInternal() {
        return userDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/search/count")
    public ModelAndView searchCount(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        return new ModelAndView().addObject(searchCountInternal(searchCondition));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public int searchCountInternal(final NodeCond searchCondition) throws InvalidSearchConditionException {
        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        return searchDAO.count(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, AttributableUtil.getInstance(AttributableType.USER));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list() {
        List<SyncopeUser> users =
                userDAO.findAll(EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()));

        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(binder.getUserTO(user));
        }

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
            userTOs.add(binder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{userId}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable("userId") final Long userId) {
        return binder.getUserTO(userId);
    }

    @PreAuthorize("#username == authentication.name or hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/readByUsername/{username}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable final String username) {
        return binder.getUserTO(username);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/read/self")
    @Transactional(readOnly = true)
    public UserTO read() {
        return binder.getAuthenticatedUserTO();
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        return search(searchCondition, -1, -1);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(@RequestBody final NodeCond searchCondition, @PathVariable("page") final int page,
            @PathVariable("size") final int size)
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

        return result;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public UserTO create(final HttpServletResponse response, @RequestBody final UserTO userTO) {
        UserTO savedTO = createInternal(userTO);
        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    public UserTO createInternal(final UserTO userTO) {
        LOG.debug("User create called with {}", userTO);

        // Check that this operation is allowed to be performed by caller
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
                created, actual.getPassword(), actual.getVirtualAttributes());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO savedTO = binder.getUserTO(created.getResult().getKey());
        savedTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        LOG.debug("About to return created user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public UserTO update(@RequestBody final UserMod userMod) {
        LOG.debug("User update called with {}", userMod);

        // AttributableMod transformation (if configured)
        UserMod actual = attrTransformer.transform(userMod);
        LOG.debug("Transformed: {}", actual);

        final String changedPwd = actual.getPassword();

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
                    actual.getVirtualAttributesToBeRemoved(), actual.getVirtualAttributesToBeUpdated());
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
                toBeExcluded.addAll(actual.getResourcesToBeAdded());
                toBeExcluded.removeAll(pwdResourceNames);
                tasks.addAll(propagationManager.getUserUpdateTaskIds(
                        updated,
                        changedPwd,
                        actual.getVirtualAttributesToBeRemoved(),
                        actual.getVirtualAttributesToBeUpdated(),
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
                        actual.getVirtualAttributesToBeRemoved(),
                        actual.getVirtualAttributesToBeUpdated(),
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

        // 4. prepare result, including propagation status on external resources
        final UserTO updatedTO = binder.getUserTO(updated.getResult().getKey());
        updatedTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/activate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(@PathVariable("userId") final Long userId,
            @RequestParam(required = true) final String token) {

        return activate(userId, token, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/activate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(@PathVariable("userId") final Long userId,
            @RequestParam(required = true) final String token,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to activate " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, token, propagationRequestTO, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/activateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(@PathVariable("username") final String username,
            @RequestParam(required = true) final String token) {

        return activate(username, token, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/activateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO activate(@PathVariable("username") final String username,
            @RequestParam(required = true) final String token,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to activate " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, token, propagationRequestTO, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/suspend/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("userId") final Long userId) {

        return suspend(userId, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/suspend/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("userId") final Long userId,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to suspend " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, null, propagationRequestTO, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/suspendByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("username") final String username) {

        return suspend(username, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/suspendByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(@PathVariable("username") final String username,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to suspend " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, null, propagationRequestTO, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET, value = "/reactivate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(@PathVariable("userId") final Long userId) {

        return reactivate(userId, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/reactivate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(@PathVariable("userId") final Long userId,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = binder.getUserFromId(userId);

        return setStatus(user, null, propagationRequestTO, true, "reactivate");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/reactivateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(@PathVariable("username") final String username) {
        return reactivate(username, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/reactivateByUsername/{username}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(@PathVariable("username") final String username,
            @RequestBody final PropagationRequestTO propagationRequestTO) {

        LOG.debug("About to reactivate " + username);

        SyncopeUser user = binder.getUserFromUsername(username);

        return setStatus(user, null, propagationRequestTO, true, "reactivate");
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{userId}")
    public UserTO delete(@PathVariable("userId") final Long userId) {
        LOG.debug("User delete called with {}", userId);

        return doDelete(userId);
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/deleteByUsername/{username}")
    public UserTO delete(@PathVariable final String username) {
        LOG.debug("User delete called with {}", username);

        UserTO result = binder.getUserTO(username);
        long userId = result.getId();

        return doDelete(userId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/execute/workflow/{taskId}")
    public UserTO executeWorkflow(@RequestBody final UserTO userTO, @PathVariable("taskId") final String taskId) {
        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        WorkflowResult<Long> updated = uwfAdapter.execute(userTO, taskId);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                new WorkflowResult<Map.Entry<Long, Boolean>>(new SimpleEntry<Long, Boolean>(updated.getResult(), null),
                updated.getPropByRes(), updated.getPerformedTasks()));

        taskExecutor.execute(tasks);

        final UserTO savedTO = binder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/list")
    @Transactional(rollbackFor = {Throwable.class})
    public List<WorkflowFormTO> getForms() {
        return uwfAdapter.getForms();
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public WorkflowFormTO getFormForUser(@PathVariable("userId") final Long userId) {
        SyncopeUser user = binder.getUserFromId(userId);
        return uwfAdapter.getForm(user.getWorkflowId());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @RequestMapping(method = RequestMethod.GET, value = "/workflow/form/claim/{taskId}")
    @Transactional(rollbackFor = {Throwable.class})
    public WorkflowFormTO claimForm(@PathVariable("taskId") final String taskId) {
        return uwfAdapter.claimForm(taskId, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @RequestMapping(method = RequestMethod.POST, value = "/workflow/form/submit")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO submitForm(@RequestBody final WorkflowFormTO form) {
        LOG.debug("About to process form {}", form);

        WorkflowResult<Map.Entry<Long, String>> updated = uwfAdapter.submitForm(form,
                SecurityContextHolder.getContext().getAuthentication().getName());

        // propByRes can be made empty by the workflow definition is no propagation should occur 
        // (for example, with rejected users)
        if (updated.getPropByRes() != null && !updated.getPropByRes().isEmpty()) {
            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<Long, Boolean>>(
                    new SimpleEntry<Long, Boolean>(updated.getResult().getKey(), Boolean.TRUE),
                    updated.getPropByRes(),
                    updated.getPerformedTasks()),
                    updated.getResult().getValue(),
                    null,
                    null,
                    null);
            taskExecutor.execute(tasks);
        }

        final UserTO savedTO = binder.getUserTO(updated.getResult().getKey());

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }

    protected UserTO setStatus(final SyncopeUser user, final String token,
            final PropagationRequestTO propagationRequestTO, final boolean status, final String task) {

        LOG.debug("About to set status of {}" + user);

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
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }

        final UserTO savedTO = binder.getUserTO(updated.getResult());
        savedTO.setPropagationStatusTOs(propReporter.getStatuses());

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

            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

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

        userTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        uwfAdapter.delete(userId);

        LOG.debug("User successfully deleted: {}", userId);

        return userTO;
    }

    @PreAuthorize("(hasRole('USER_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE) or "
            + "(hasRole('USER_UPDATE') and "
            + "(#bulkAction.operation == #bulkAction.operation.REACTIVATE or "
            + "#bulkAction.operation == #bulkAction.operation.SUSPEND))")
    @RequestMapping(method = RequestMethod.POST, value = "/bulk")
    public BulkActionRes bulkAction(@RequestBody final BulkAction bulkAction) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected UserTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Object id = null;

        if (ArrayUtils.isNotEmpty(args) && !"claimForm".equals(method.getName())) {
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

        if (id != null) {
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
