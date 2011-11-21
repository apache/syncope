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

import java.util.List;
import javassist.NotFoundException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.syncope.client.to.NotificationTaskTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.NotificationTask;
import org.syncope.core.persistence.beans.PropagationTask;
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
    private static final Logger LOG = LoggerFactory.getLogger(
            TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {
        "executions", "resource", "defaultResources", "defaultRoles",
        "updateIdentities"};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {
        "id", "task"};

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    private void fill(final SyncTask task, final SyncTaskTO taskTO) {
        for (String resourceName : taskTO.getDefaultResources()) {
            ExternalResource resource = resourceDAO.find(resourceName);
            if (resource == null) {
                LOG.warn("Ignoring invalid resource " + resourceName);
            } else {
                task.addDefaultResource(resource);
            }
        }

        for (Long roleId : taskTO.getDefaultRoles()) {
            SyncopeRole role = roleDAO.find(roleId);
            if (role == null) {
                LOG.warn("Ignoring invalid role " + roleId);
            } else {
                task.addDefaultRole(role);
            }
        }

        task.setPerformCreate(taskTO.isPerformCreate());
        task.setPerformUpdate(taskTO.isPerformUpdate());
        task.setPerformDelete(taskTO.isPerformDelete());

        task.setJobActionsClassName(taskTO.getJobActionsClassName());
    }

    public SchedTask createSchedTask(final SchedTaskTO taskTO,
            final TaskUtil taskUtil)
            throws NotFoundException {

        SchedTask task = taskUtil.newTask();
        task.setCronExpression(taskTO.getCronExpression());

        switch (taskUtil) {
            case SCHED:
                task.setJobClassName(taskTO.getJobClassName());
                break;

            case SYNC:
                SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;

                ExternalResource resource = resourceDAO.find(syncTaskTO.
                        getResource());
                if (resource == null) {
                    throw new NotFoundException("Resource "
                            + syncTaskTO.getResource());
                }
                ((SyncTask) task).setResource(resource);

                fill((SyncTask) task, syncTaskTO);
                break;
        }

        return task;
    }

    public void updateSchedTask(final SchedTask task, final SchedTaskTO taskTO,
            final TaskUtil taskUtil) {

        task.setCronExpression(taskTO.getCronExpression());

        if (taskUtil == TaskUtil.SYNC) {
            fill((SyncTask) task, (SyncTaskTO) taskTO);
        }
    }

    public TaskExecTO getTaskExecutionTO(final TaskExec execution) {
        TaskExecTO executionTO = new TaskExecTO();
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_TASK_EXECUTION_PROPERTIES);
        if (execution.getId() != null) {
            executionTO.setId(execution.getId());
        }
        executionTO.setTask(execution.getTask().getId());

        return executionTO;
    }

    private void setExecTime(final SchedTaskTO taskTO) {
        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(
                    JobInstanceLoader.getTriggerName(taskTO.getId()),
                    Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + JobInstanceLoader.
                    getTriggerName(taskTO.getId()), e);
            trigger = null;
        }

        if (trigger != null) {
            taskTO.setLastExec(trigger.getPreviousFireTime());
            taskTO.setNextExec(trigger.getNextFireTime());
        }
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
                setExecTime((SchedTaskTO) taskTO);
                break;

            case SYNC:
                setExecTime((SchedTaskTO) taskTO);

                ((SyncTaskTO) taskTO).setResource(
                        ((SyncTask) task).getResource().getName());
                for (ExternalResource resource :
                        ((SyncTask) task).getDefaultResources()) {

                    ((SyncTaskTO) taskTO).addDefaultResource(
                            resource.getName());
                }
                for (SyncopeRole role :
                        ((SyncTask) task).getDefaultRoles()) {

                    ((SyncTaskTO) taskTO).addDefaultRole(role.getId());
                }
                ((SyncTaskTO) taskTO).setPerformCreate(
                        ((SyncTask) task).isPerformCreate());
                ((SyncTaskTO) taskTO).setPerformUpdate(
                        ((SyncTask) task).isPerformUpdate());
                ((SyncTaskTO) taskTO).setPerformDelete(
                        ((SyncTask) task).isPerformDelete());

                ((SyncTaskTO) taskTO).setJobActionsClassName(
                        ((SyncTask) task).getJobActionsClassName());
                break;

            case NOTIFICATION:
                BeanUtils.copyProperties((NotificationTask) task,
                        (NotificationTaskTO) taskTO, IGNORE_TASK_PROPERTIES);
                break;
        }

        return taskTO;
    }
}
