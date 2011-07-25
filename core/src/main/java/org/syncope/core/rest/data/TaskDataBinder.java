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
package org.syncope.core.rest.data;

import java.util.Date;
import java.util.List;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.util.TaskUtil;

@Component
public class TaskDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {
        "executions", "resource", "defaultResources", "defaultRoles",
        "updateIdentities"};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {
        "task"};

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private RoleDAO roleDAO;

    public SchedTask getSchedTask(final SchedTaskTO taskTO,
            final TaskUtil taskUtil)
            throws NotFoundException {

        SchedTask task = taskUtil.newTask();
        task.setCronExpression(taskTO.getCronExpression());

        if (task instanceof SchedTask) {
            task.setJobClassName(taskTO.getJobClassName());
        }
        if (task instanceof SyncTask) {
            SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;

            TargetResource resource = resourceDAO.find(syncTaskTO.getResource());
            if (resource == null) {
                throw new NotFoundException("Resource "
                        + syncTaskTO.getResource());
            }
            ((SyncTask) task).setResource(resource);

            for (String resourceName : syncTaskTO.getDefaultResources()) {
                resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.warn("Ignoring invalid resource " + resourceName);
                } else {
                    ((SyncTask) task).addDefaultResource(resource);
                }
            }

            SyncopeRole role;
            for (Long roleId : syncTaskTO.getDefaultRoles()) {
                role = roleDAO.find(roleId);
                if (role == null) {
                    LOG.warn("Ignoring invalid role " + roleId);
                } else {
                    ((SyncTask) task).addDefaultRole(role);
                }
            }

            ((SyncTask) task).setUpdateIdentities(
                    syncTaskTO.isUpdateIdentities());
        }

        return task;
    }

    public TaskExecTO getTaskExecutionTO(final TaskExec execution) {
        TaskExecTO executionTO = new TaskExecTO();
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_TASK_EXECUTION_PROPERTIES);
        executionTO.setTask(execution.getTask().getId());

        return executionTO;
    }

    public TaskTO getTaskTO(final Task task, final TaskUtil taskUtil) {
        TaskTO taskTO = taskUtil.newTaskTO();
        BeanUtils.copyProperties(task, taskTO, IGNORE_TASK_PROPERTIES);

        List<TaskExec> executions = task.getExecs();
        for (TaskExec execution : executions) {
            taskTO.addExecution(getTaskExecutionTO(execution));
        }

        switch (taskUtil) {
            case PROPAGATION:
                ((PropagationTaskTO) taskTO).setResource(
                        ((PropagationTask) task).getResource().getName());
                break;

            case SCHED:
                ((SchedTaskTO) taskTO).setLastExec(new Date());
                ((SchedTaskTO) taskTO).setNextExec(new Date());
                break;

            case SYNC:
                ((SyncTaskTO) taskTO).setLastExec(new Date());
                ((SyncTaskTO) taskTO).setNextExec(new Date());

                ((SyncTaskTO) taskTO).setResource(
                        ((SyncTask) task).getResource().getName());
                for (TargetResource resource :
                        ((SyncTask) task).getDefaultResources()) {

                    ((SyncTaskTO) taskTO).addDefaultResource(resource.getName());
                }
                for (SyncopeRole role :
                        ((SyncTask) task).getDefaultRoles()) {

                    ((SyncTaskTO) taskTO).addDefaultRole(role.getId());
                }
                ((SyncTaskTO) taskTO).setUpdateIdentities(
                        ((SyncTask) task).isUpdateIdentities());
                break;
        }

        return taskTO;
    }
}
