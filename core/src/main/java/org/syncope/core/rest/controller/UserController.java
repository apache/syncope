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
import org.syncope.core.persistence.propagation.PropagationException;
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
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowException;

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

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/verifyPassword/{userId}")
    @Transactional(readOnly = true)
    public ModelAndView verifyPassword(@PathVariable("userId") Long userId,
            @RequestParam("password") final String password)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = dataBinder.getUserFromId(userId);

        SyncopeUser passwordUser = new SyncopeUser();
        passwordUser.setPassword(password, user.getCipherAlgoritm(), 0);

        return new ModelAndView().addObject(user.getPassword().
                equalsIgnoreCase(passwordUser.getPassword()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
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
            userTOs.add(dataBinder.getUserTO(user.getId()));
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
            userTOs.add(dataBinder.getUserTO(user.getId()));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public UserTO read(@PathVariable("userId") final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = dataBinder.getUserFromId(userId);

        return dataBinder.getUserTO(user.getId());
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
            result.add(dataBinder.getUserTO(user.getId()));
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
            result.add(dataBinder.getUserTO(user.getId()));
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

        Map.Entry<Long, Boolean> created = wfAdapter.create(userTO);

        List<PropagationTask> tasks = propagationManager.getCreateTaskIds(
                created.getKey(), userTO.getPassword(),
                userTO.getVirtualAttributes(), created.getValue());
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(created.getKey());

        LOG.debug("About to return created user\n{}", savedTO);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    public UserTO activate(@RequestBody final UserTO userTO)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException, PropagationException {

        Long updatedId = wfAdapter.activate(userTO.getId(), userTO.getToken());

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updatedId, null, null, null, Boolean.TRUE, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(updatedId);

        LOG.debug("About to return activated user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody final UserMod userMod)
            throws NotFoundException, PropagationException,
            UnauthorizedRoleException, WorkflowException {

        LOG.debug("User update called with {}", userMod);

        Map.Entry<Long, PropagationByResource> updated =
                wfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updated.getKey(), userMod.getPassword(),
                userMod.getVirtualAttributesToBeRemoved(),
                userMod.getVirtualAttributesToBeAdded(),
                null, updated.getValue());
        propagationManager.execute(tasks);

        final UserTO updatedTO = dataBinder.getUserTO(updated.getKey());

        LOG.debug("About to return updated user\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/suspend/{userId}")
    public UserTO suspend(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to suspend " + userId);

        Long updatedId = wfAdapter.suspend(userId);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updatedId, null, null, null, Boolean.FALSE, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(updatedId);

        LOG.debug("About to return suspended user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/reactivate/{userId}")
    public UserTO reactivate(final @PathVariable("userId") Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to reactivate " + userId);

        Long updatedId = wfAdapter.reactivate(userId);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updatedId, null, null, null, Boolean.TRUE, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(updatedId);

        LOG.debug("About to return suspended user\n{}", savedTO);

        return savedTO;
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException, PropagationException,
            UnauthorizedRoleException {

        LOG.debug("User delete called with {}", userId);

        List<PropagationTask> tasks =
                propagationManager.getDeleteTaskIds(userId);
        propagationManager.execute(tasks);

        wfAdapter.delete(userId);

        LOG.debug("User successfully deleted: {}", userId);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/execute/workflow/{taskId}")
    public UserTO executeWorkflow(@RequestBody final UserTO userTO,
            @PathVariable("taskId") final String taskId)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException, PropagationException {

        LOG.debug("About to execute {} on {}", taskId, userTO.getId());

        Long updatedId = wfAdapter.execute(userTO, taskId);

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updatedId, null, null, null, null, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(updatedId);

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
            throws NotFoundException, WorkflowException, PropagationException {

        LOG.debug("About to process form {}", form);

        Long updatedId = wfAdapter.submitForm(form,
                SecurityContextHolder.getContext().
                getAuthentication().getName());

        List<PropagationTask> tasks = propagationManager.getUpdateTaskIds(
                updatedId, null, null, null, Boolean.TRUE, null);
        propagationManager.execute(tasks);

        final UserTO savedTO = dataBinder.getUserTO(updatedId);

        LOG.debug("About to return user after form processing\n{}", savedTO);

        return savedTO;
    }
}
