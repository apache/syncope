
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
import org.syncope.client.to.SearchParameters;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.UserTOs;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.rest.data.UserDataBinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
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
    private SyncopeConfigurationDAO syncopeConfigurationDAO;
    @Autowired
    private UserDataBinder userDataBinder;
    @Autowired
    private Workflow userWorkflow;
    @Autowired(required = false)
    private SpringHibernateJPAWorkflowStore workflowStore;
    @Autowired
    private PropagationManager propagationManager;

    @Transactional
    private UserTO executeAction(String actionName,
            HttpServletResponse response, UserTO userTO) throws IOException {

        SyncopeUser syncopeUser = syncopeUserDAO.find(userTO.getId());

        if (syncopeUser == null) {
            log.error("Could not find user '" + userTO.getId() + "'");
            return throwNotFoundException(
                    String.valueOf(userTO.getId()), response);
        }

        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, syncopeUser);
        inputs.put(Constants.TOKEN, userTO.getToken());

        WorkflowDescriptor workflowDescriptor =
                userWorkflow.getWorkflowDescriptor(Constants.USER_WORKFLOW);

        int[] actions = userWorkflow.getAvailableActions(
                syncopeUser.getWorkflowEntryId(), inputs);
        Integer actionId = null;
        for (int i = 0; i < actions.length && actionId == null; i++) {
            if (actionName.equals(
                    workflowDescriptor.getAction(actions[i]).getName())) {

                actionId = actions[i];
            }
        }
        if (actionId == null) {
            return throwNotFoundException(actionName, response);
        }

        try {
            userWorkflow.doAction(syncopeUser.getWorkflowEntryId(),
                    actionId, inputs);
        } catch (WorkflowException e) {
            log.error("While performing " + actionName, e);

            return throwWorkflowException(e, response);
        }

        syncopeUser = syncopeUserDAO.save(syncopeUser);
        return userDataBinder.getUserTO(syncopeUser);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/activate")
    public UserTO activate(HttpServletResponse response,
            @RequestBody UserTO userTO)
            throws IOException {

        return executeAction(Constants.ACTION_ACTIVATE, response, userTO);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public UserTO create(HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody UserTO userTO,
            @RequestParam(value = "syncRoles",
            required = false) Set<Long> syncRoles,
            @RequestParam(value = "syncResources",
            required = false) Set<String> syncResources)
            throws IOException {

        if (syncRoles == null) {
            syncRoles = Collections.EMPTY_SET;
        }
        if (syncResources == null) {
            syncResources = Collections.EMPTY_SET;
        }

        if (log.isDebugEnabled()) {
            log.debug("create called with parameters " + userTO
                    + "\n" + syncRoles + "\n" + syncResources);
        }

        WorkflowInitException wie = null;
        Long workflowId = null;
        try {
            workflowId = userWorkflow.initialize(Constants.USER_WORKFLOW, 0,
                    Collections.singletonMap(Constants.USER_TO, userTO));
        } catch (WorkflowInitException e) {
            log.error("During workflow initialization: " + e, e);
            wie = e;

            // Removing dirty workflow entry
            if (workflowStore != null && e.getWorkflowEntryId() != null) {
                workflowStore.delete(e.getWorkflowEntryId());
            }
        } catch (WorkflowException e) {
            log.error("Unexpected workflow exception", e);

            return throwWorkflowException(e, response);
        }

        if (wie != null) {
            switch (wie.getExceptionOperation()) {
                case OVERWRITE:
                    return update(response, userTO);
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

                    return throwCompositeException(compositeException,
                            response);
            }
        }

        SyncopeUser syncopeUser = null;
        try {
            syncopeUser = userDataBinder.createSyncopeUser(userTO);
        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + userTO, e);

            // Removing dirty workflow entry
            if (workflowStore != null) {
                workflowStore.delete(workflowId);
            }

            return throwCompositeException(e, response);
        }
        syncopeUser.setWorkflowEntryId(workflowId);
        syncopeUser.setCreationTime(new Date());
        syncopeUser = syncopeUserDAO.save(syncopeUser);

        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put(Constants.SYNCOPE_USER, syncopeUser);

        int[] availableWorkflowActions = userWorkflow.getAvailableActions(
                workflowId, null);
        for (int availableWorkflowAction : availableWorkflowActions) {
            try {
                userWorkflow.doAction(workflowId, availableWorkflowAction,
                        inputs);
            } catch (WorkflowException e) {
                log.error("Unexpected workflow exception", e);

                return throwWorkflowException(e, response);
            }
        }
        syncopeUser = syncopeUserDAO.save(syncopeUser);

        // Now that user is created locally, let's propagate
        Set<String> synchronous = new HashSet<String>();
        for (Resource resource : syncopeUser.getResources()) {
            if (syncResources.contains(resource.getName())) {
                synchronous.add(resource.getName());
            }
        }
        for (SyncopeRole role : syncopeUser.getRoles()) {
            if (syncRoles.contains(role.getId())) {
                for (Resource resource : role.getResources()) {
                    synchronous.add(resource.getName());
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("About to propagate synchronously on the following "
                    + "resources " + synchronous);
        }

        try {
            propagationManager.provision(syncopeUser, synchronous);
        } catch (PropagationException e) {
            log.error("Propagation exception", e);

            return throwPropagationException(e, response);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return userDataBinder.getUserTO(syncopeUser);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{userId}")
    public void delete(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            throwNotFoundException(String.valueOf(userId), response);
        } else {
            if (workflowStore != null && user.getWorkflowEntryId() != null) {
                workflowStore.delete(user.getWorkflowEntryId());
            }

            syncopeUserDAO.delete(userId);
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public UserTOs list(HttpServletRequest request) throws IOException {
        List<SyncopeUser> users = syncopeUserDAO.findAll();
        List<UserTO> userTOs = new ArrayList<UserTO>(users.size());

        for (SyncopeUser user : users) {
            userTOs.add(userDataBinder.getUserTO(user));
        }

        UserTOs result = new UserTOs();
        result.setUsers(userTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{userId}")
    public UserTO read(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        return userDataBinder.getUserTO(user);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.GET,
    value = "/generateToken/{userId}")
    public UserTO generateToken(HttpServletResponse response,
            @PathVariable("userId") Long userId)
            throws IOException {

        UserTO userTO = new UserTO();
        userTO.setId(userId);
        return executeAction(Constants.ACTION_GENERATE_TOKEN, response, userTO);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/verifyToken")
    public UserTO verifyToken(HttpServletResponse response,
            @RequestBody UserTO userTO)
            throws IOException {

        return executeAction(Constants.ACTION_VERIFY_TOKEN, response, userTO);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/search")
    public UserTOs search(HttpServletResponse response,
            @RequestBody SearchParameters searchParameters)
            throws IOException {

        log.info("search called with parameter " + searchParameters);

        List<UserTO> userTOs = new ArrayList<UserTO>();
        UserTOs result = new UserTOs();

        result.setUsers(userTOs);

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/status/{userId}")
    public ModelAndView getStatus(HttpServletResponse response,
            @PathVariable("userId") Long userId) throws IOException {

        SyncopeUser user = syncopeUserDAO.find(userId);

        if (user == null) {
            log.error("Could not find user '" + userId + "'");
            return throwNotFoundException(String.valueOf(userId), response);
        }

        List<Step> currentSteps = userWorkflow.getCurrentSteps(
                user.getWorkflowEntryId());
        if (currentSteps == null || currentSteps.isEmpty()) {
            return null;
        }

        ModelAndView mav = new ModelAndView();
        mav.addObject(currentSteps.iterator().next().getStatus());
        return mav;
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public UserTO update(HttpServletResponse response,
            @RequestBody UserTO userTO)
            throws IOException {

        log.info("update called with parameter " + userTO);

        return userTO;
    }
}
