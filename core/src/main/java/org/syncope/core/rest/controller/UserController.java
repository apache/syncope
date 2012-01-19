/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.propagation.PropagationException;
import org.syncope.core.rest.data.UserDataBinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.core.notification.NotificationManager;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.propagation.PropagationManager;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowException;
import org.syncope.core.workflow.WorkflowResult;

/**
 * Note that this controller does not extend AbstractController, hence does not 
 * provide any Spring's @Transactional logic at class level.
 *
 * @see AbstractController
 */
@Controller
@RequestMapping("/user")
public class UserController {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO searchDAO;

    @Autowired
    private UserDataBinder dataBinder;

    @Autowired
    private UserWorkflowAdapter wfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private NotificationManager notificationManager;

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/verifyPassword/{userId}")
    @Transactional(readOnly = true)
    public ModelAndView verifyPassword(@PathVariable("userId") Long userId,
            @RequestParam("password") final String password)
            throws NotFoundException, UnauthorizedRoleException {

        return new ModelAndView().addObject(
                dataBinder.verifyPassword(userId, password));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/count")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public ModelAndView count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(userDAO.count(adminRoleIds));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/count")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public ModelAndView searchCount(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(
                searchDAO.count(adminRoleIds, searchCondition));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list() {
        List<SyncopeUser> users = userDAO.findAll(EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()));
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(dataBinder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> list(
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(dataBinder.getUserTO(user));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable("userId") final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        return dataBinder.getUserTO(userId);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@RequestParam("username") final String username)
            throws NotFoundException, UnauthorizedRoleException {

        return dataBinder.getUserTO(username);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(dataBinder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<UserTO> search(
            @RequestBody final NodeCond searchCondition,
            @PathVariable("page") final int page,
            @PathVariable("size") final int size)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeUser> matchingUsers = searchDAO.search(
                EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(dataBinder.getUserTO(user));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(final HttpServletResponse response,
            @RequestBody final UserTO userTO)
            throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {

        LOG.debug("User create called with {}", userTO);

        Set<Long> requestRoleIds =
                new HashSet<Long>(userTO.getMemberships().size());
        for (MembershipTO membership : userTO.getMemberships()) {
            requestRoleIds.add(membership.getRoleId());
        }
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        requestRoleIds.removeAll(adminRoleIds);
        if (!requestRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(requestRoleIds);
        }

        WorkflowResult<Map.Entry<Long, Boolean>> created =
                wfAdapter.create(userTO);

        List<PropagationTask> tasks = propagationManager.getCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirtualAttributes());
        propagationManager.execute(tasks);

        notificationManager.createTasks(new WorkflowResult<Long>(
                created.getResult().getKey(),
                created.getPropByRes(),
                created.getPerformedTasks()));

        final UserTO savedTO = dataBinder.getUserTO(
                created.getResult().getKey());

        LOG.debug("About to return created user\n{}", savedTO);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody final UserMod userMod)
            throws NotFoundException, PropagationException,
            UnauthorizedRoleException, WorkflowException {

        LOG.debug("User update called with {}", userMod);

        WorkflowResult<Long> updated = wfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updated, userMod.getPassword(),
                userMod.getVirtualAttributesToBeRemoved(),
                userMod.getVirtualAttributesToBeUpdated(), null);
        propagationManager.execute(tasks);

        notificationManager.createTasks(updated);

        final UserTO updatedTO =
                dataBinder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO activate(
            @RequestBody final UserTO userTO,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true")
            final Boolean performLocal)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to activate " + userTO.getId());


        SyncopeUser user = userDAO.find(userTO.getId());
        if (user == null) {
            throw new NotFoundException("User " + userTO.getId());
        }

        return setStatus(user, resourceNames, performLocal, true, "activate");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/suspend/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO suspend(
            @PathVariable("userId") final Long userId,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true")
            final Boolean performLocal)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + userId);


        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, resourceNames, performLocal, false, "suspend");
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/reactivate/{userId}")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO reactivate(final @PathVariable("userId") Long userId,
            @RequestParam(required = false) final Set<String> resourceNames,
            @RequestParam(required = false, defaultValue = "true")
            final Boolean performLocal)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return setStatus(user, resourceNames, performLocal, true, "reactivate");
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException, PropagationException,
            UnauthorizedRoleException {

        LOG.debug("User delete called with {}", userId);

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after wfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        notificationManager.createTasks(
                new WorkflowResult<Long>(userId, null, "delete"));

        List<PropagationTask> tasks =
                propagationManager.getDeleteTaskIds(userId);
        propagationManager.execute(tasks);

        wfAdapter.delete(userId);

        LOG.debug("User successfully deleted: {}", userId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/execute/workflow/{taskId}")
    public UserTO executeWorkflow(
            @RequestBody final UserTO userTO,
            @PathVariable("taskId") final String taskId)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        WorkflowResult<Long> updated = wfAdapter.execute(userTO, taskId);

        List<PropagationTask> tasks =
                propagationManager.getUpdateTaskIds(updated, null);

        propagationManager.execute(tasks);

        notificationManager.createTasks(updated);

        final UserTO savedTO = dataBinder.getUserTO(updated.getResult());

        LOG.debug("About to return updated user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/workflow/form/list")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<WorkflowFormTO> getForms() {
        return wfAdapter.getForms();
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/workflow/form/{userId}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public WorkflowFormTO getFormForUser(
            @PathVariable("userId") final Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        SyncopeUser user = dataBinder.getUserFromId(userId);
        return wfAdapter.getForm(user.getWorkflowId());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/workflow/form/claim/{taskId}")
    @Transactional(rollbackFor = {Throwable.class})
    public WorkflowFormTO claimForm(@PathVariable("taskId") final String taskId)
            throws NotFoundException, WorkflowException {

        return wfAdapter.claimForm(taskId,
                SecurityContextHolder.getContext().
                getAuthentication().getName());
    }

    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/workflow/form/submit")
    @Transactional(rollbackFor = {Throwable.class})
    public UserTO submitForm(@RequestBody final WorkflowFormTO form)
            throws NotFoundException, WorkflowException, PropagationException,
            UnauthorizedRoleException {

        LOG.debug("About to process form {}", form);

        WorkflowResult<Map.Entry<Long, String>> updated =
                wfAdapter.submitForm(form, SecurityContextHolder.getContext().
                getAuthentication().getName());

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                new WorkflowResult<Long>(updated.getResult().getKey(),
                updated.getPropByRes(), updated.getPerformedTasks()),
                updated.getResult().getValue(), null, null, Boolean.TRUE);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(
                updated.getResult().getKey());

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }

    private UserTO setStatus(
            final SyncopeUser user,
            final Set<String> resourceNames,
            final boolean performLocal,
            final boolean status,
            final String performedTask)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + user.getId());

        List<PropagationTask> tasks = null;
        WorkflowResult<Long> updated = null;

        if (performLocal) {
            // perform local changes

            if ("suspend".equals(performedTask)) {
                updated = wfAdapter.suspend(user.getId());
            } else if ("reactivate".equals(performedTask)) {
                updated = wfAdapter.reactivate(user.getId());
            } else {
                updated = wfAdapter.activate(user.getId(), user.getToken());
            }
        } else {
            // do not perform local changes
            updated = new WorkflowResult<Long>(user.getId(), null, performedTask);
        }

        // Resources to exclude from propagation.
        Set<String> resources = new HashSet<String>();
        if (resourceNames != null) {
            resources.addAll(user.getResourceNames());
            resources.removeAll(resourceNames);
        }

        tasks = propagationManager.getUpdateTaskIds(user, status, resources);

        propagationManager.execute(tasks);
        notificationManager.createTasks(updated);

        final UserTO savedTO = dataBinder.getUserTO(updated.getResult());

        LOG.debug("About to return suspended user\n{}", savedTO);

        return savedTO;
    }
}
