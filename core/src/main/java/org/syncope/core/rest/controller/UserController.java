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

import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.spi.Step;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowActionsTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.rest.data.UserDataBinder.CheckInResult;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.WFUtils;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserSearchDAO userSearchDAO;

    @Autowired
    private JPAWorkflowEntryDAO workflowEntryDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Resource(name = "userWorkflow")
    private Workflow workflow;

    @Autowired
    private PropagationManager propagationManager;

    private SyncopeUser getUserFromId(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        Set<Long> notAdminRoleIds = new HashSet<Long>();
        for (SyncopeRole role : user.getRoles()) {
            if (!adminRoleIds.contains(role.getId())) {
                notAdminRoleIds.add(role.getId());
            }
        }
        if (!notAdminRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(notAdminRoleIds);
        }

        return user;
    }

    private UserTO executeAction(SyncopeUser user, String actionName,
            Map<String, Object> moreInputs)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException {

        Map<String, Object> inputs = new HashMap<String, Object>();
        if (moreInputs != null && !moreInputs.isEmpty()) {
            inputs.putAll(moreInputs);
        }

        inputs.put(Constants.SYNCOPE_USER, user);

        WFUtils.doExecuteAction(workflow,
                Constants.USER_WORKFLOW,
                actionName,
                user.getWorkflowId(),
                inputs);

        user = userDAO.save(user);

        return userDataBinder.getUserTO(user, workflow);
    }

    private UserTO executeAction(UserTO userTO, String actionName,
            Map<String, Object> moreInputs)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userTO.getId());

        return executeAction(user, actionName, moreInputs);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    public UserTO activate(@RequestBody UserTO userTO)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException {

        return executeAction(userTO, Constants.ACTION_ACTIVATE,
                Collections.singletonMap(
                Constants.TOKEN, (Object) userTO.getToken()));
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/generateToken/{userId}")
    public UserTO generateToken(@PathVariable("userId") Long userId)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException {

        UserTO userTO = new UserTO();
        userTO.setId(userId);

        return executeAction(userTO, Constants.ACTION_GENERATE_TOKEN, null);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/verifyToken")
    public UserTO verifyToken(@RequestBody UserTO userTO)
            throws WorkflowException, NotFoundException,
            UnauthorizedRoleException {

        return executeAction(userTO, Constants.ACTION_VERIFY_TOKEN,
                Collections.singletonMap(
                Constants.TOKEN, (Object) userTO.getToken()));
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/suspend/{userId}")
    public UserTO suspend(@PathVariable("userId") final Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException {

        LOG.debug("About to suspend " + userId);

        SyncopeUser user = getUserFromId(userId);

        executeAction(user, "suspend", null);
        user = userDAO.save(user);

        return userDataBinder.getUserTO(user, workflow);
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/reactivate/{userId}")
    public UserTO reactivate(final @PathVariable("userId") Long userId)
            throws NotFoundException, WorkflowException,
            UnauthorizedRoleException {

        LOG.debug("About to reactivate " + userId);

        SyncopeUser user = getUserFromId(userId);

        executeAction(user, "reactivate", null);
        user = userDAO.save(user);

        return userDataBinder.getUserTO(user, workflow);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/verifyPassword/{userId}")
    @Transactional(readOnly = true)
    public ModelAndView verifyPassword(@PathVariable("userId") Long userId,
            @RequestParam("password") final String password)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        SyncopeUser passwordUser = new SyncopeUser();
        passwordUser.setPassword(password, user.getCipherAlgoritm());

        return new ModelAndView().addObject(user.getPassword().
                equalsIgnoreCase(passwordUser.getPassword()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/count")
    public ModelAndView count() {
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(userDAO.count(adminRoleIds));
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/count")
    @Transactional(readOnly = true)
    public ModelAndView searchCount(@RequestBody NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        return new ModelAndView().addObject(
                userSearchDAO.count(adminRoleIds, searchCondition));
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    @Transactional(readOnly = true)
    public List<UserTO> list() {
        List<SyncopeUser> users = userDAO.findAll(EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()));
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user, workflow));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list/{page}/{size}")
    @Transactional(readOnly = true)
    public List<UserTO> list(
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeUser> users = userDAO.findAll(adminRoleIds, page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user, workflow));
        }

        return userTOs;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    @Transactional(readOnly = true)
    public UserTO read(@PathVariable("userId") Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        return userDataBinder.getUserTO(user, workflow);
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/actions/{userId}")
    @Transactional(readOnly = true)
    public WorkflowActionsTO getActions(@PathVariable("userId") Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        WorkflowActionsTO result = new WorkflowActionsTO();

        int[] availableActions = workflow.getAvailableActions(
                user.getWorkflowId(), Collections.EMPTY_MAP);
        for (int i = 0; i < availableActions.length; i++) {
            result.addAction(
                    workflow.getWorkflowDescriptor(Constants.USER_WORKFLOW).
                    getAction(availableActions[i]).getName());
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    @Transactional(readOnly = true)
    public List<UserTO> search(@RequestBody NodeCond searchCondition)
            throws InvalidSearchConditionException {

        LOG.debug("User search called with condition {}", searchCondition);

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers = userSearchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.
                getOwnedEntitlementNames()), searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user, workflow));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/search/{page}/{size}")
    @Transactional(readOnly = true)
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

        final List<SyncopeUser> matchingUsers = userSearchDAO.search(
                EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames()),
                searchCondition, page, size);

        final List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user, workflow));
        }

        return result;
    }

    @PreAuthorize("hasRole('USER_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/status/{userId}")
    @Transactional(readOnly = true)
    public ModelAndView getStatus(@PathVariable("userId") Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        List<Step> currentSteps = workflow.getCurrentSteps(
                user.getWorkflowId());

        ModelAndView mav = new ModelAndView();
        if (currentSteps != null && !currentSteps.isEmpty()) {
            mav.addObject(currentSteps.iterator().next().getStatus());
        }

        return mav;
    }

    private Set<String> getMandatoryResourceNames(SyncopeUser user,
            Set<Long> mandatoryRoles, Set<String> mandatoryResources) {

        if (mandatoryRoles == null) {
            mandatoryRoles = Collections.EMPTY_SET;
        }
        if (mandatoryResources == null) {
            mandatoryResources = Collections.EMPTY_SET;
        }

        Set<String> mandatoryResourceNames = new HashSet<String>();

        for (TargetResource resource : user.getTargetResources()) {
            if (mandatoryResources.contains(resource.getName())) {
                mandatoryResourceNames.add(resource.getName());
            }
        }
        for (SyncopeRole role : user.getRoles()) {
            if (mandatoryRoles.contains(role.getId())) {
                for (TargetResource resource : role.getTargetResources()) {
                    mandatoryResourceNames.add(resource.getName());
                }
            }
        }

        return mandatoryResourceNames;
    }

    @PreAuthorize("hasRole('USER_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(HttpServletResponse response,
            @RequestBody UserTO userTO,
            @RequestParam(value = "mandatoryRoles",
            required = false) Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) Set<String> mandatoryResources)
            throws SyncopeClientCompositeErrorException,
            DataIntegrityViolationException, WorkflowException,
            PropagationException, NotFoundException, UnauthorizedRoleException {

        LOG.debug("User create called with parameters {}\n{}\n{}",
                new Object[]{userTO, mandatoryRoles, mandatoryResources});

        CheckInResult checkInResult = userDataBinder.checkIn(userTO);
        LOG.debug("Check-in result: {}", checkInResult);

        switch (checkInResult.getAction()) {
            case CREATE:
                break;

            case OVERWRITE:
                delete(checkInResult.getSyncopeUserId(),
                        mandatoryRoles, mandatoryResources);
                break;

            case REJECT:
                SyncopeClientCompositeErrorException compositeException =
                        new SyncopeClientCompositeErrorException(
                        HttpStatus.BAD_REQUEST);
                SyncopeClientException rejectedUserCreate =
                        new SyncopeClientException(
                        SyncopeClientExceptionType.RejectedUserCreate);
                rejectedUserCreate.addElement(
                        String.valueOf(checkInResult.getSyncopeUserId()));
                compositeException.addException(rejectedUserCreate);

                throw compositeException;

            default:
        }

        // The user to be created
        SyncopeUser user = new SyncopeUser();
        userDataBinder.create(user, userTO);

        // Check if roles requested for this user are allowed to be
        // administrated by the caller
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        Set<Long> notAdminRoleIds = new HashSet<Long>();
        for (SyncopeRole role : user.getRoles()) {
            if (!adminRoleIds.contains(role.getId())) {
                notAdminRoleIds.add(role.getId());
            }
        }
        if (!notAdminRoleIds.isEmpty()) {
            throw new UnauthorizedRoleException(notAdminRoleIds);
        }

        // Create the user
        user = userDAO.save(user);

        // Now that user is created locally, let's propagate
        Set<String> mandatoryResourceNames = getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);

        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate mandatory onto resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.create(
                user, userTO.getPassword(), mandatoryResourceNames);

        // User is created locally and propagated, let's advance on the workflow
        final Long workflowId =
                workflow.initialize(Constants.USER_WORKFLOW, 0, null);
        user.setWorkflowId(workflowId);

        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, user);
        inputs.put(Constants.USER_TO, userTO);

        int[] wfActions = workflow.getAvailableActions(workflowId, null);
        LOG.debug("Available workflow actions for user {}: {}",
                user, wfActions);

        for (int wfAction : wfActions) {
            LOG.debug("About to execute action {} on user {}", wfAction, user);
            workflow.doAction(workflowId, wfAction, inputs);
            LOG.debug("Action {} on user {} run successfully", wfAction, user);
        }

        user = userDAO.save(user);

        final UserTO savedTO = userDataBinder.getUserTO(user, workflow);
        LOG.debug("About to return create user\n{}", savedTO);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('USER_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody UserMod userMod,
            @RequestParam(value = "mandatoryRoles",
            required = false) Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) Set<String> mandatoryResources)
            throws NotFoundException, PropagationException, WorkflowException,
            UnauthorizedRoleException {

        LOG.debug("User update called with parameters {}\n{}\n{}",
                new Object[]{userMod, mandatoryRoles, mandatoryResources});

        SyncopeUser user = getUserFromId(userMod.getId());

        // First of all, let's check if update is allowed
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, user);
        inputs.put(Constants.USER_MOD, userMod);

        WFUtils.doExecuteAction(workflow,
                Constants.USER_WORKFLOW,
                Constants.ACTION_UPDATE,
                user.getWorkflowId(),
                inputs);

        // Update user with provided userMod
        ResourceOperations resourceOperations =
                userDataBinder.update(user, userMod);
        user = userDAO.save(user);

        // Now that user is update locally, let's propagate
        Set<String> mandatoryResourceNames = getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate mandatory onto resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.update(user, userMod.getPassword(),
                resourceOperations, mandatoryResourceNames);

        return userDataBinder.getUserTO(user, workflow);
    }

    @PreAuthorize("hasRole('USER_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") Long userId,
            @RequestParam(value = "mandatoryRoles",
            required = false) Set<Long> mandatoryRoles,
            @RequestParam(value = "mandatoryResources",
            required = false) Set<String> mandatoryResources)
            throws NotFoundException, WorkflowException, PropagationException,
            UnauthorizedRoleException {

        SyncopeUser user = getUserFromId(userId);

        WFUtils.doExecuteAction(workflow,
                Constants.USER_WORKFLOW,
                Constants.ACTION_DELETE,
                user.getWorkflowId(),
                Collections.singletonMap(Constants.SYNCOPE_USER,
                (Object) user));

        // Propagate delete
        Set<String> mandatoryResourceNames = getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate mandatory onto resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.delete(user, mandatoryResourceNames);

        // Now that delete has been propagated, let's remove everything
        if (user.getWorkflowId() != null) {
            workflowEntryDAO.delete(user.getWorkflowId());
        }
        userDAO.delete(userId);
    }
}
