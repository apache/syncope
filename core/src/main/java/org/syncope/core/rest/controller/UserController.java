
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
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.UserTOs;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.rest.data.UserDataBinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.NodeSearchCondition;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowActionsTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.rest.data.InvalidSearchConditionException;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.SpringHibernateJPAWorkflowStore;
import org.syncope.core.workflow.WorkflowInitException;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/user")
public class UserController extends AbstractController {

    @Autowired
    private SyncopeUserDAO syncopeUserDAO;
    @Autowired
    private UserDataBinder userDataBinder;
    @Autowired
    private Workflow userWorkflow;
    @Autowired(required = false)
    private SpringHibernateJPAWorkflowStore workflowStore;
    @Autowired
    private PropagationManager propagationManager;

    private Integer findWorkflowAction(Long workflowId, String actionName) {

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

    public SyncopeUser doExecuteAction(String actionName, Long userId,
            Map<String, Object> moreInputs)
            throws WorkflowException, NotFoundException {

        SyncopeUser syncopeUser = syncopeUserDAO.find(userId);

        if (syncopeUser == null) {
            log.error("Could not find user '" + userId + "'");

            throw new NotFoundException(String.valueOf(userId));
        }

        Map<String, Object> inputs = new HashMap<String, Object>();
        if (moreInputs != null && !moreInputs.isEmpty()) {
            inputs.putAll(moreInputs);
        }
        inputs.put(Constants.SYNCOPE_USER, syncopeUser);

        Integer actionId = findWorkflowAction(syncopeUser.getWorkflowId(),
                actionName);
        if (actionId == null) {
            throw new NotFoundException(actionName);
        }

        try {
            userWorkflow.doAction(syncopeUser.getWorkflowId(),
                    actionId, inputs);
        } catch (InvalidActionException e) {
            throw new WorkflowException(e);
        }

        return syncopeUserDAO.save(syncopeUser);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/action/{actionName}")
    public UserTO executeAction(HttpServletResponse response,
            @RequestBody UserTO userTO,
            @PathVariable(value = "actionName") String actionName)
            throws WorkflowException, NotFoundException {

        return userDataBinder.getUserTO(
                doExecuteAction(actionName, userTO.getId(), null), userWorkflow);
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
    public UserTOs list() {
        List<SyncopeUser> users = syncopeUserDAO.findAll();
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user, userWorkflow));
        }

        UserTOs result = new UserTOs();
        result.setUsers(userTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    public UserTO read(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");

            throw new NotFoundException(String.valueOf(userId));
        }

        return userDataBinder.getUserTO(user, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/actions/{userId}")
    public WorkflowActionsTO getActions(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");

            throw new NotFoundException(String.valueOf(userId));
        }

        WorkflowActionsTO result = new WorkflowActionsTO();

        WorkflowDescriptor workflowDescriptor =
                userWorkflow.getWorkflowDescriptor(Constants.USER_WORKFLOW);
        int[] availableActions = userWorkflow.getAvailableActions(
                user.getWorkflowId(), Collections.EMPTY_MAP);
        for (int i = 0; i < availableActions.length; i++) {
            result.addAction(workflowDescriptor.getAction(
                    availableActions[i]).getName());
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    public UserTOs search(@RequestBody NodeSearchCondition searchCondition)
            throws InvalidSearchConditionException {

        if (log.isDebugEnabled()) {
            log.debug("search called with condition " + searchCondition);
        }

        if (!searchCondition.checkValidity()) {
            log.error("Invalid search condition: " + searchCondition);

            throw new InvalidSearchConditionException();
        }

        List<SyncopeUser> matchingUsers =
                syncopeUserDAO.search(searchCondition);
        UserTOs result = new UserTOs();
        for (SyncopeUser user : matchingUsers) {
            result.addUser(userDataBinder.getUserTO(user, userWorkflow));
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/status/{userId}")
    public ModelAndView getStatus(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");

            throw new NotFoundException(String.valueOf(userId));
        }

        List<Step> currentSteps = userWorkflow.getCurrentSteps(
                user.getWorkflowId());
        if (currentSteps == null || currentSteps.isEmpty()) {
            return null;
        }

        ModelAndView mav = new ModelAndView();
        mav.addObject(currentSteps.iterator().next().getStatus());
        return mav;
    }

    private Set<String> getSyncResourceNames(SyncopeUser syncopeUser,
            Set<Long> syncRoles, Set<String> syncResources) {

        if ((syncRoles == null || syncRoles.isEmpty()
                && (syncResources == null || syncResources.isEmpty()))) {
            return Collections.EMPTY_SET;
        }

        Set<String> syncResourceNames = new HashSet<String>();

        for (Resource resource : syncopeUser.getResources()) {
            if (syncResources.contains(resource.getName())) {
                syncResourceNames.add(resource.getName());
            }
        }
        for (SyncopeRole role : syncopeUser.getRoles()) {
            if (syncRoles.contains(role.getId())) {
                for (Resource resource : role.getResources()) {
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

        if (log.isDebugEnabled()) {
            log.debug("create called with parameters " + userTO
                    + "\n" + syncRoles + "\n" + syncResources);
        }

        // By default, ignore id in UserTO:
        // set it explicitely in case of overwrite
        userTO.setId(0);

        WorkflowInitException wie = null;
        Long workflowId = null;
        try {
            workflowId = userWorkflow.initialize(Constants.USER_WORKFLOW, 0,
                    Collections.singletonMap(Constants.USER_TO, userTO));
        } catch (WorkflowInitException e) {
            log.error("During workflow initialization: " + e);
            wie = e;

            // Removing dirty workflow entry
            if (workflowStore != null && e.getWorkflowEntryId() != null) {
                workflowStore.delete(e.getWorkflowEntryId());
            }

            // Use the found workflow id
            workflowId = wie.getWorkflowId();
        }

        if (wie != null) {
            switch (wie.getExceptionOperation()) {

                case OVERWRITE:
                    Integer resetActionId = findWorkflowAction(
                            wie.getWorkflowId(), Constants.ACTION_RESET);
                    if (resetActionId != null) {
                        doExecuteAction(Constants.ACTION_RESET,
                                wie.getSyncopeUserId(),
                                Collections.singletonMap(Constants.USER_TO,
                                (Object) userTO));
                    }

                    userTO.setId(wie.getSyncopeUserId());
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

        SyncopeUser syncopeUser = userDataBinder.createSyncopeUser(userTO);
        syncopeUser.setWorkflowId(workflowId);
        syncopeUser.setCreationTime(new Date());
        syncopeUser = syncopeUserDAO.save(syncopeUser);

        // Check if attributes with unique schema have unique values
        userDataBinder.checkUniqueness(syncopeUser);

        // Now that user is created locally, let's propagate
        Set<String> syncResourceNames =
                getSyncResourceNames(syncopeUser, syncRoles, syncResources);
        if (log.isDebugEnabled() && !syncResourceNames.isEmpty()) {
            log.debug("About to propagate synchronously onto resources "
                    + syncResourceNames);
        }
        Set<String> propagatedResources =
                propagationManager.create(syncopeUser, syncResourceNames);
        if (log.isDebugEnabled()) {
            log.debug("Propagated onto resources " + propagatedResources);
        }

        // User is created locally and propagated, let's advance on the workflow
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, syncopeUser);

        int[] availableWorkflowActions = userWorkflow.getAvailableActions(
                workflowId, null);
        for (int availableWorkflowAction : availableWorkflowActions) {
            userWorkflow.doAction(workflowId, availableWorkflowAction,
                    inputs);
        }
        syncopeUser = syncopeUserDAO.save(syncopeUser);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return userDataBinder.getUserTO(syncopeUser, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(@RequestBody UserMod userMod,
            @RequestParam(value = "syncRoles",
            required = false) Set<Long> syncRoles,
            @RequestParam(value = "syncResources",
            required = false) Set<String> syncResources)
            throws NotFoundException, PropagationException, WorkflowException {

        if (log.isDebugEnabled()) {
            log.debug("update called with parameter " + userMod);
        }

        SyncopeUser syncopeUser = syncopeUserDAO.find(userMod.getId());
        if (syncopeUser == null) {
            log.error("Could not find user '" + userMod.getId() + "'");

            throw new NotFoundException(String.valueOf(userMod.getId()));
        }

        // First of all, let's check if update is allowed
        syncopeUser = doExecuteAction(Constants.ACTION_UPDATE,
                syncopeUser.getId(), null);

        // Update user with provided userMod
        ResourceOperations resourceOperations =
                userDataBinder.updateSyncopeUser(syncopeUser, userMod);
        syncopeUser = syncopeUserDAO.save(syncopeUser);

        // Check if attributes with unique schema have unique values
        userDataBinder.checkUniqueness(syncopeUser);

        // Now that user is update locally, let's propagate
        Set<String> syncResourceNames =
                getSyncResourceNames(syncopeUser, syncRoles, syncResources);
        if (log.isDebugEnabled() && !syncResourceNames.isEmpty()) {
            log.debug("About to propagate synchronously onto resources "
                    + syncResourceNames);
        }
        Set<String> propagatedResources =
                propagationManager.update(syncopeUser,
                resourceOperations, syncResourceNames);
        if (log.isDebugEnabled()) {
            log.debug("Propagated onto resources " + propagatedResources);
        }

        return userDataBinder.getUserTO(syncopeUser, userWorkflow);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(@PathVariable("userId") Long userId)
            throws NotFoundException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");

            throw new NotFoundException(String.valueOf(userId));
        } else {
            if (workflowStore != null && user.getWorkflowId() != null) {
                workflowStore.delete(user.getWorkflowId());
            }

            syncopeUserDAO.delete(userId);
        }
    }
}
