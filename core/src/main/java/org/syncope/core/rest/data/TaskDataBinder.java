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
import org.apache.commons.lang.StringUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.util.JexlUtil;
import org.syncope.core.util.TaskUtil;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class TaskDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {
        "latestExecStatus", "executions", "resource", "user"};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {
        "id", "task"};

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private JexlUtil jexlUtil;

    private void checkJexl(final AbstractAttributableTO attributableTO,
            final SyncopeClientException sce) {

        for (AttributeTO attrTO : attributableTO.getAttributes()) {
            if (!attrTO.getValues().isEmpty()
                    && !jexlUtil.isExpressionValid(attrTO.getValues().get(0))) {

                sce.addElement("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
        for (AttributeTO attrTO : attributableTO.getVirtualAttributes()) {
            if (!attrTO.getValues().isEmpty()
                    && !jexlUtil.isExpressionValid(attrTO.getValues().get(0))) {

                sce.addElement("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
    }

    private void fill(final SyncTask task, final SyncTaskTO taskTO) {
        if (taskTO.getUserTemplate() != null) {
            UserTO template = taskTO.getUserTemplate();

            // 1. validate JEXL expressions in user template
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidSyncTask);

            if (StringUtils.isNotBlank(template.getUsername())
                    && !jexlUtil.isExpressionValid(template.getUsername())) {

                sce.addElement("Invalid JEXL: " + template.getUsername());
            }
            if (StringUtils.isNotBlank(template.getPassword())
                    && !jexlUtil.isExpressionValid(template.getPassword())) {

                sce.addElement("Invalid JEXL: " + template.getPassword());
            }

            checkJexl(template, sce);

            for (MembershipTO memb : template.getMemberships()) {
                checkJexl(memb, sce);
            }

            if (!sce.isEmpty()) {
                SyncopeClientCompositeErrorException scce =
                        new SyncopeClientCompositeErrorException(
                        HttpStatus.BAD_REQUEST);
                scce.addException(sce);
                throw scce;
            }
        }

        // 2. all JEXL expressions are valid: accept user template
        task.setUserTemplate(taskTO.getUserTemplate());

        task.setPerformCreate(taskTO.isPerformCreate());
        task.setPerformUpdate(taskTO.isPerformUpdate());
        task.setPerformDelete(taskTO.isPerformDelete());
        task.setSyncStatus(taskTO.isSyncStatus());
        task.setFullReconciliation(taskTO.isFullReconciliation());

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

    public TaskExecTO getTaskExecTO(final TaskExec execution) {
        TaskExecTO executionTO = new TaskExecTO();
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_TASK_EXECUTION_PROPERTIES);
        if (execution.getId() != null) {
            executionTO.setId(execution.getId());
        }
        executionTO.setTask(execution.getTask().getId());

        return executionTO;
    }

    private void setExecTime(final SchedTaskTO taskTO, final Task task) {
        String triggerName = JobInstanceLoader.getTriggerName(
                JobInstanceLoader.getJobName(task));

        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(triggerName,
                    Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
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

        TaskExec latestExec = taskExecDAO.findLatestStarted(task);
        taskTO.setLatestExecStatus(latestExec == null
                ? "" : latestExec.getStatus());

        List<TaskExec> executions = task.getExecs();
        for (TaskExec execution : executions) {
            taskTO.addExecution(getTaskExecTO(execution));
        }

        switch (taskUtil) {
            case PROPAGATION:
                ((PropagationTaskTO) taskTO).setResource(
                        ((PropagationTask) task).getResource().getName());
                if (((PropagationTask) task).getSyncopeUser() != null) {
                    ((PropagationTaskTO) taskTO).setUser(
                            ((PropagationTask) task).getSyncopeUser().getId());
                }
                break;

            case SCHED:
                setExecTime((SchedTaskTO) taskTO, task);
                break;

            case SYNC:
                setExecTime((SchedTaskTO) taskTO, task);

                ((SyncTaskTO) taskTO).setResource(
                        ((SyncTask) task).getResource().getName());
                break;

            case NOTIFICATION:
                break;

            default:
        }

        return taskTO;
    }
}
