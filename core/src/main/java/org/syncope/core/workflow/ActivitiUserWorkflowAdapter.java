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
import javassist.NotFoundException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.propagation.PropagationByResource;

/**
 * Activiti (http://www.activiti.org/) based implementation.
 */
public class ActivitiUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

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

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

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
    public Map.Entry<Long, Boolean> create(final UserTO userTO)
            throws WorkflowException {

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
        identityService.saveUser(activitiUser);

        Boolean enable = (Boolean) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), PROPAGATE_ENABLE);

        return new DefaultMapEntry(user.getId(), enable);
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
    protected Long doActivate(final SyncopeUser user, final String token)
            throws WorkflowException {

        doExecuteAction(user, "activate",
                Collections.singletonMap(TOKEN, (Object) token));
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Map.Entry<Long, PropagationByResource> doUpdate(
            final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        doExecuteAction(user, "update",
                Collections.singletonMap(USER_MOD, (Object) userMod));
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes =
                (PropagationByResource) runtimeService.getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE);

        return new DefaultMapEntry(updated.getId(), propByRes);
    }

    @Override
    protected Long doSuspend(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "suspend", null);
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Long doReactivate(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "reactivate", null);
        setStatus(user.getWorkflowId(), user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "delete", null);
        userDAO.delete(user);

        identityService.deleteUser(user.getId().toString());
    }
}
