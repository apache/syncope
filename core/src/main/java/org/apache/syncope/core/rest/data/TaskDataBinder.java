/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.rest.data;

import org.apache.commons.lang.StringUtils;
import org.apache.syncope.client.to.AbstractAttributableTO;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.PropagationTaskTO;
import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.core.init.JobInstanceLoader;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.util.TaskUtil;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class TaskDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {"latestExecStatus", "executions", "resource", "user"};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {"id", "task"};

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private JexlUtil jexlUtil;

    private void checkJexl(final AbstractAttributableTO attributableTO, final SyncopeClientException sce) {

        for (AttributeTO attrTO : attributableTO.getAttributes()) {
            if (!attrTO.getValues().isEmpty() && !jexlUtil.isExpressionValid(attrTO.getValues().get(0))) {

                sce.addElement("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
        for (AttributeTO attrTO : attributableTO.getVirtualAttributes()) {
            if (!attrTO.getValues().isEmpty() && !jexlUtil.isExpressionValid(attrTO.getValues().get(0))) {

                sce.addElement("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
    }

    private void fill(final SyncTask task, final SyncTaskTO taskTO) {
        if (taskTO.getUserTemplate() != null) {
            UserTO template = taskTO.getUserTemplate();

            // 1. validate JEXL expressions in user template
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.InvalidSyncTask);

            if (StringUtils.isNotBlank(template.getUsername()) && !jexlUtil.isExpressionValid(template.getUsername())) {

                sce.addElement("Invalid JEXL: " + template.getUsername());
            }
            if (StringUtils.isNotBlank(template.getPassword()) && !jexlUtil.isExpressionValid(template.getPassword())) {

                sce.addElement("Invalid JEXL: " + template.getPassword());
            }

            checkJexl(template, sce);

            for (MembershipTO memb : template.getMemberships()) {
                checkJexl(memb, sce);
            }

            if (!sce.isEmpty()) {
                SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(
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

    public SchedTask createSchedTask(final SchedTaskTO taskTO, final TaskUtil taskUtil)
            throws NotFoundException {

        SchedTask task = taskUtil.newTask();
        task.setCronExpression(taskTO.getCronExpression());
        task.setName(taskTO.getName());
        task.setDescription(taskTO.getDescription());

        switch (taskUtil) {
            case SCHED:
                task.setJobClassName(taskTO.getJobClassName());
                break;

            case SYNC:
                if (!(taskTO instanceof SyncTaskTO)) {
                    throw new ClassCastException("taskUtil is type SyncTask but taskTO is not SyncTaskTO: " + taskTO.
                            getClass().getName());
                }
                SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;

                ExternalResource resource = resourceDAO.find(syncTaskTO.getResource());
                if (resource == null) {
                    throw new NotFoundException("Resource " + syncTaskTO.getResource());
                }
                ((SyncTask) task).setResource(resource);

                fill((SyncTask) task, syncTaskTO);
                break;
        }

        return task;
    }

    public void updateSchedTask(final SchedTask task, final SchedTaskTO taskTO, final TaskUtil taskUtil) {
        task.setCronExpression(taskTO.getCronExpression());
        task.setName(taskTO.getName());
        task.setDescription(taskTO.getDescription());

        if (taskUtil == TaskUtil.SYNC) {
            if (!(task instanceof SyncTask)) {
                throw new ClassCastException("taskUtil is type SyncTask but task is not SyncTask: " + task.getClass().
                        getName());
            }
            if (!(taskTO instanceof SyncTaskTO)) {
                throw new ClassCastException("taskUtil is type SyncTask but taskTO is not SyncTaskTO: " + taskTO.
                        getClass().getName());
            }

            fill((SyncTask) task, (SyncTaskTO) taskTO);
        }
    }

    public TaskExecTO getTaskExecTO(final TaskExec execution) {
        TaskExecTO executionTO = new TaskExecTO();
        BeanUtils.copyProperties(execution, executionTO, IGNORE_TASK_EXECUTION_PROPERTIES);

        if (execution.getId() != null) {
            executionTO.setId(execution.getId());
        }

        if (execution.getTask() != null && execution.getTask().getId() != null) {
            executionTO.setTask(execution.getTask().getId());
        }

        return executionTO;
    }

    private void setExecTime(final SchedTaskTO taskTO, final Task task) {
        String triggerName = JobInstanceLoader.getTriggerName(JobInstanceLoader.getJobName(task));

        Trigger trigger = null;
        try {
            trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
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
                ? ""
                : latestExec.getStatus());

        taskTO.setStartDate(latestExec == null
                ? null
                : latestExec.getStartDate());

        taskTO.setEndDate(latestExec == null
                ? null
                : latestExec.getEndDate());

        for (TaskExec execution : task.getExecs()) {
            taskTO.addExecution(getTaskExecTO(execution));
        }

        switch (taskUtil) {
            case PROPAGATION:
                if (!(task instanceof PropagationTask)) {
                    throw new ClassCastException("taskUtil is type Propagation but task is not PropagationTask: "
                            + task.getClass().getName());
                }

                ((PropagationTaskTO) taskTO).setResource(((PropagationTask) task).getResource().getName());
                if (((PropagationTask) task).getSyncopeUser() != null) {
                    ((PropagationTaskTO) taskTO).setUser(((PropagationTask) task).getSyncopeUser().getId());
                }
                break;

            case SCHED:
                setExecTime((SchedTaskTO) taskTO, task);
                ((SchedTaskTO) taskTO).setName(((SchedTask) task).getName());
                ((SchedTaskTO) taskTO).setDescription(((SchedTask) task).getDescription());
                break;

            case SYNC:
                setExecTime((SchedTaskTO) taskTO, task);
                ((SyncTaskTO) taskTO).setName(((SyncTask) task).getName());
                ((SyncTaskTO) taskTO).setDescription(((SyncTask) task).getDescription());
                if (!(task instanceof SyncTask)) {
                    throw new ClassCastException("taskUtil is type Sync but task is not SyncTask: " + task.getClass().
                            getName());
                }

                ((SyncTaskTO) taskTO).setResource(((SyncTask) task).getResource().getName());
                break;

            case NOTIFICATION:
                if (((NotificationTask) task).isExecuted() && StringUtils.isBlank(taskTO.getLatestExecStatus())) {
                    taskTO.setLatestExecStatus("[EXECUTED]");
                }
                break;

            default:
        }

        return taskTO;
    }
}
