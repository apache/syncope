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

import com.opensymphony.workflow.InvalidActionException;
import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.spi.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
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
import javax.servlet.http.HttpServletResponse;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.UserMod;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowActionsTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.rest.data.InvalidSearchConditionException;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.WorkflowInitException;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;

    @Autowired
    private JPAWorkflowEntryDAO workflowEntryDAO;

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private Workflow userWorkflow;

    @Autowired
    private PropagationManager propagationManager;

    public Integer findWorkflowAction(final Long workflowId,
            final String actionName) {

        WorkflowDescriptor workflowDescriptor =
                userWorkflow.getWorkflowDescriptor(Constants.USER_WORKFLOW);

        int[] actions = userWorkflow.getAvailableActions(workflowId, null);

        Integer actionId = null;
        for (int i = 0; i < actions.length && actionId == null; i++) {
            if (actionName.equals(
                    workflowDescriptor.getAction(actions[i]).getName())) {

                actionId = actions[i];
            }
        }

        Map<Integer, ActionDescriptor> commonActions =
                workflowDescriptor.getCommonActions();
        for (Integer actionNumber : commonActions.keySet()) {
            if (actionName.equals(commonActions.get(actionNumber).getName())) {
                actionId = actionNumber;
            }
        }

        return actionId;
    }

    public SyncopeUser doExecuteAction(final String actionName,
            final Long userId,
            final Map<String, Object> moreInputs)
            throws WorkflowException, NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        Map<String, Object> inputs = new HashMap<String, Object>();
        if (moreInputs != null && !moreInputs.isEmpty()) {
            inputs.putAll(moreInputs);
        }
        inputs.put(Constants.SYNCOPE_USER, user);

        Integer actionId = findWorkflowAction(user.getWorkflowId(),
                actionName);
        if (actionId == null) {
            throw new NotFoundException("Workflow action '" + actionName + "'");
        }

        try {
            userWorkflow.doAction(user.getWorkflowId(),
                    actionId, inputs);
        } catch (InvalidActionException e) {
            throw new WorkflowException(e);
        }

        return syncopeUserDAO.save(user);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/action/{actionName}")
    public UserTO executeAction(HttpServletResponse response,
            @RequestBody UserTO userTO,
            @PathVariable(value = "actionName") String actionName)
            throws WorkflowException, NotFoundException {

        return userDataBinder.getUserTO(
                doExecuteAction(actionName, userTO.getId(), null),
                userWorkflow);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    public UserTO activate(@RequestBody UserTO userTO)
            throws WorkflowException, NotFoundException {

        return userDataBinder.getUserTO(
                doExecuteAction(Constants.ACTION_ACTIVATE, userTO.getId(),
                Collections.singletonMap(Constants.TOKEN,
                (Object) userTO.getToken())), userWorkflow);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/generateToken/{userId}")
    public UserTO generateToken(@PathVariable("userId") Long userId)
            throws WorkflowException, NotFoundException {

        UserTO userTO = new UserTO();
        userTO.setId(userId);
        return userDataBinder.getUserTO(
                doExecuteAction(Constants.ACTION_GENERATE_TOKEN,
                userTO.getId(), null),
                userWorkflow);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/verifyToken")
    public UserTO verifyToken(@RequestBody UserTO userTO)
            throws WorkflowException, NotFoundException {

        return userDataBinder.getUserTO(
                doExecuteAction(Constants.ACTION_VERIFY_TOKEN, userTO.getId(),
                Collections.singletonMap(Constants.TOKEN,
                (Object) userTO.getToken())),
                userWorkflow);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<UserTO> list() {
        List<SyncopeUser> users = syncopeUserDAO.findAll();
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user, userWorkflow));
        }

        return userTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/paginatedList/{page}/{size}")
    public List<UserTO> paginatedList(
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {
        List<SyncopeUser> users = syncopeUserDAO.findAll(page, size);
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());
        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user, userWorkflow));
        }

        return userTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    public UserTO read(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return userDataBinder.getUserTO(user, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/actions/{userId}")
    public WorkflowActionsTO getActions(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        WorkflowActionsTO result = new WorkflowActionsTO();

        int[] availableActions = userWorkflow.getAvailableActions(
                user.getWorkflowId(), Collections.EMPTY_MAP);
        for (int i = 0; i < availableActions.length; i++) {
            result.addAction(
                    userWorkflow.getWorkflowDescriptor(Constants.USER_WORKFLOW).
                    getAction(availableActions[i]).getName());
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    public List<UserTO> search(@RequestBody NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("search called with condition " + searchCondition);
        }

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: " + searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers =
                syncopeUserDAO.search(searchCondition);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user, userWorkflow));
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/paginatedSearch/{page}/{size}")
    public List<UserTO> paginatedSearch(
            @RequestBody final NodeCond searchCondition,
            @PathVariable("page") final int page,
            @PathVariable("size") final int size)
            throws InvalidSearchConditionException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("search called with condition " + searchCondition);
        }

        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition: " + searchCondition);
            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers =
                syncopeUserDAO.search(searchCondition, page, size);
        List<UserTO> result = new ArrayList<UserTO>(matchingUsers.size());
        for (SyncopeUser user : matchingUsers) {
            result.add(userDataBinder.getUserTO(user, userWorkflow));
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/status/{userId}")
    public ModelAndView getStatus(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        List<Step> currentSteps = userWorkflow.getCurrentSteps(
                user.getWorkflowId());

        ModelAndView mav = new ModelAndView();
        if (currentSteps != null && !currentSteps.isEmpty()) {
            mav.addObject(currentSteps.iterator().next().getStatus());
        }

        return mav;
    }

    private Set<String> getSyncResourceNames(SyncopeUser user,
            Set<Long> syncRoles, Set<String> syncResources) {

        if ((syncRoles == null || syncRoles.isEmpty()
                && (syncResources == null || syncResources.isEmpty()))) {
            return Collections.EMPTY_SET;
        }

        Set<String> syncResourceNames = new HashSet<String>();

        for (TargetResource resource : user.getTargetResources()) {
            if (syncResources.contains(resource.getName())) {
                syncResourceNames.add(resource.getName());
            }
        }
        for (SyncopeRole role : user.getRoles()) {
            if (syncRoles.contains(role.getId())) {
                for (TargetResource resource : role.getTargetResources()) {
                    syncResourceNames.add(resource.getName());
                }
            }
        }

        return syncResourceNames;
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(HttpServletResponse response,
            @RequestBody UserTO userTO,
            @RequestParam(value = "syncRoles",
            required = false) Set<Long> syncRoles,
            @RequestParam(value = "syncResources",
            required = false) Set<String> syncResources)
            throws SyncopeClientCompositeErrorException,
            WorkflowException, PropagationException, NotFoundException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("create called with parameters " + userTO + "\n"
                    + syncRoles + "\n" + syncResources);
        }

        // The user to be created
        SyncopeUser user = null;

        WorkflowInitException wie = null;
        Long workflowId = null;
        try {
            workflowId = userWorkflow.initialize(Constants.USER_WORKFLOW, 0,
                    Collections.singletonMap(Constants.USER_TO, userTO));
        } catch (WorkflowInitException e) {
            LOG.error("During workflow initialization: " + e);
            wie = e;

            // Removing dirty workflow entry
            if (e.getWorkflowEntryId() != null) {
                workflowEntryDAO.delete(e.getWorkflowEntryId());
            }

            // Use the found workflow id
            workflowId = wie.getWorkflowId();
        }

        if (wie != null) {
            switch (wie.getExceptionOperation()) {

                case OVERWRITE:
                    final Integer resetActionId = findWorkflowAction(
                            wie.getWorkflowId(), Constants.ACTION_RESET);
                    if (resetActionId == null) {
                        user = syncopeUserDAO.find(wie.getSyncopeUserId());
                        if (user == null) {
                            throw new NotFoundException("User "
                                    + wie.getSyncopeUserId());
                        }
                    } else {
                        user = doExecuteAction(
                                Constants.ACTION_RESET,
                                wie.getSyncopeUserId(),
                                Collections.singletonMap(Constants.USER_TO,
                                (Object) userTO));
                    }
                    break;

                case REJECT:
                    SyncopeClientCompositeErrorException compositeException =
                            new SyncopeClientCompositeErrorException(
                            HttpStatus.BAD_REQUEST);
                    SyncopeClientException rejectedUserCreate =
                            new SyncopeClientException(
                            SyncopeClientExceptionType.RejectedUserCreate);
                    rejectedUserCreate.addElement(
                            String.valueOf(wie.getSyncopeUserId()));
                    compositeException.addException(rejectedUserCreate);

                    throw compositeException;
            }
        }

        // No overwrite: let's create a fresh new user
        if (user == null) {
            user = new SyncopeUser();
        }

        userDataBinder.create(user, userTO);

        user.setWorkflowId(workflowId);
        user = syncopeUserDAO.save(user);

        // Check if attributes with unique schema have unique values
        userDataBinder.checkUniqueness(user);

        // Now that user is created locally, let's propagate
        Set<String> syncResourceNames =
                getSyncResourceNames(user, syncRoles, syncResources);

        if (!syncResourceNames.isEmpty()) {
            LOG.debug("About to propagate synchronously onto resources {}",
                    syncResourceNames);
        }

        propagationManager.create(user, syncResourceNames);

        // User is created locally and propagated, let's advance on the workflow
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, user);

        int[] availableWorkflowActions =
                userWorkflow.getAvailableActions(workflowId, null);
        LOG.debug("Available workflow actions for user {}: {}",
                user, availableWorkflowActions);

        for (int availableWorkflowAction : availableWorkflowActions) {
            userWorkflow.doAction(
                    workflowId, availableWorkflowAction, inputs);
        }

        user = syncopeUserDAO.save(user);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return userDataBinder.getUserTO(user, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody UserMod userMod,
            @RequestParam(value = "syncRoles",
            required = false) Set<Long> syncRoles,
            @RequestParam(value = "syncResources",
            required = false) Set<String> syncResources)
            throws NotFoundException, PropagationException, WorkflowException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("update called with parameter " + userMod);
        }

        SyncopeUser user = syncopeUserDAO.find(userMod.getId());
        if (user == null) {
            throw new NotFoundException("User " + userMod.getId());
        }

        // First of all, let's check if update is allowed
        user = doExecuteAction(Constants.ACTION_UPDATE,
                userMod.getId(), Collections.singletonMap(Constants.USER_MOD,
                (Object) userMod));

        // Update user with provided userMod
        ResourceOperations resourceOperations =
                userDataBinder.update(user, userMod);
        user = syncopeUserDAO.save(user);

        // Check if attributes with unique schema have unique values
        userDataBinder.checkUniqueness(user);

        // Now that user is update locally, let's propagate
        Set<String> syncResourceNames =
                getSyncResourceNames(user, syncRoles, syncResources);
        if (LOG.isDebugEnabled() && !syncResourceNames.isEmpty()) {
            LOG.debug("About to propagate synchronously onto resources "
                    + syncResourceNames);
        }

        propagationManager.update(user, resourceOperations, syncResourceNames);

        return userDataBinder.getUserTO(user, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") Long userId,
            @RequestParam(value = "syncRoles",
            required = false) Set<Long> syncRoles,
            @RequestParam(value = "syncResources",
            required = false) Set<String> syncResources)
            throws NotFoundException, WorkflowException, PropagationException {

        SyncopeUser user = syncopeUserDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        doExecuteAction(Constants.ACTION_DELETE, userId, null);

        // Propagate delete
        Set<String> syncResourceNames =
                getSyncResourceNames(user, syncRoles, syncResources);
        if (LOG.isDebugEnabled() && !syncResourceNames.isEmpty()) {
            LOG.debug("About to propagate synchronously onto resources "
                    + syncResourceNames);
        }

        propagationManager.delete(user, syncResourceNames);

        // Now that delete has been propagated, let's remove everything
        if (user.getWorkflowId() != null) {
            workflowEntryDAO.delete(user.getWorkflowId());
        }
        syncopeUserDAO.delete(userId);
    }
}
