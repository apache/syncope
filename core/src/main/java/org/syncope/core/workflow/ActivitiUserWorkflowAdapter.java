/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.types.PropagationOperation;

/**
 * Activiti (http://www.activiti.org/) based implementation.
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class ActivitiUserWorkflowAdapter implements UserWorkflowAdapter {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(ActivitiUserWorkflowAdapter.class);

    public static final String SYNCOPE_USER = "syncopeUser";

    public static final String USER_TO = "userTO";

    public static final String USER_MOD = "userMod";

    public static final String EMAIL_KIND = "emailKind";

    public static final String ACTION = "action";

    public static final String TOKEN = "token";

    public static String PROP_BY_RESOURCE = "propByResource";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PropagationManager propagationManager;

    private void setStatus(final String processInstanceId,
            final SyncopeUser user) {

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(
                processInstanceId).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})",
                    tasks.size());
        } else {
            user.setStatus(tasks.get(0).getTaskDefinitionKey());
        }
    }

    @Override
    public SyncopeUser create(final UserTO userTO,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(USER_TO, userTO);

        final ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(
                    "userWorkflow", variables);
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        SyncopeUser user = (SyncopeUser) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), SYNCOPE_USER);
        setStatus(processInstance.getProcessInstanceId(), user);
        user = userDAO.save(user);

        // create and save Activiti user
        User activitiUser = identityService.newUser(user.getId().toString());
        activitiUser.setPassword(userTO.getPassword());
        identityService.saveUser(activitiUser);

        // Now that user is created locally, let's propagate
        Set<String> mandatoryResourceNames =
                userService.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.create(
                user, userTO.getPassword(), mandatoryResourceNames);

        return user;
    }

    private void doExecuteAction(final SyncopeUser user,
            final String action, final Map<String, Object> moreVariables)
            throws WorkflowException {

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(SYNCOPE_USER, user);
        variables.put(ACTION, action);
        if (moreVariables != null && !moreVariables.isEmpty()) {
            variables.putAll(moreVariables);
        }

        if (StringUtils.isBlank(user.getWorkflowId())) {
            throw new WorkflowException(
                    new NotFoundException("Empty workflow id"));
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(
                user.getWorkflowId()).list();
        if (tasks.size() != 1) {
            LOG.warn("Expected a single task, found {}", tasks.size());
        } else {
            try {
                taskService.complete(tasks.get(0).getId(), variables);
            } catch (ActivitiException e) {
                throw new WorkflowException(e);
            }
        }
    }

    @Override
    public SyncopeUser activate(final SyncopeUser user, final String token)
            throws WorkflowException, PropagationException {

        doExecuteAction(user, "activate",
                Collections.singletonMap(TOKEN, (Object) token));
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(
                PropagationOperation.UPDATE, user.getTargetResources());
        propagationManager.update(user, null, propByRes, null);

        return updated;
    }

    @Override
    public SyncopeUser update(final SyncopeUser user, final UserMod userMod,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        doExecuteAction(user, "update",
                Collections.singletonMap(USER_MOD, (Object) userMod));
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes =
                (PropagationByResource) runtimeService.getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE);

        // Now that user is updated locally, let's propagate
        Set<String> mandatoryResourceNames =
                userService.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.update(updated, userMod.getPassword(),
                propByRes, mandatoryResourceNames);

        return updated;
    }

    @Override
    public SyncopeUser suspend(final SyncopeUser user)
            throws WorkflowException, PropagationException {

        doExecuteAction(user, "suspend", null);
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(
                PropagationOperation.UPDATE, user.getTargetResources());
        propagationManager.update(user, null, propByRes, null);

        return updated;
    }

    @Override
    public SyncopeUser reactivate(final SyncopeUser user)
            throws WorkflowException, PropagationException {

        doExecuteAction(user, "reactivate", null);
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(
                PropagationOperation.UPDATE, user.getTargetResources());
        propagationManager.update(user, null, propByRes, null);

        return updated;
    }

    @Override
    public void delete(final SyncopeUser user,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        // Propagate delete
        Set<String> mandatoryResourceNames =
                userService.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.delete(user, mandatoryResourceNames);

        doExecuteAction(user, "delete", null);

        identityService.deleteUser(user.getId().toString());
    }
}
