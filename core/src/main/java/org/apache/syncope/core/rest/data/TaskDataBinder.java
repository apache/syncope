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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.init.JobInstanceLoader;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskExecDAO;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.TaskUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class TaskDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TaskDataBinder.class);

    private static final String[] IGNORE_TASK_PROPERTIES = {"executions", "resource",};

    private static final String[] IGNORE_TASK_EXECUTION_PROPERTIES = {"id", "task"};

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    private void checkJexl(final AbstractAttributableTO attributableTO, final SyncopeClientException sce) {
        for (AttributeTO attrTO : attributableTO.getAttrs()) {
            if (!attrTO.getValues().isEmpty() && !JexlUtil.isExpressionValid(attrTO.getValues().get(0))) {
                sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }

        for (AttributeTO attrTO : attributableTO.getVirAttrs()) {
            if (!attrTO.getValues().isEmpty() && !JexlUtil.isExpressionValid(attrTO.getValues().get(0))) {
                sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
            }
        }
    }

    private void fill(final SyncTask task, final SyncTaskTO taskTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSyncTask);

        // 1. validate JEXL expressions in user and role templates
        if (taskTO.getUserTemplate() != null) {
            UserTO template = taskTO.getUserTemplate();

            if (StringUtils.isNotBlank(template.getUsername()) && !JexlUtil.isExpressionValid(template.getUsername())) {
                sce.getElements().add("Invalid JEXL: " + template.getUsername());
            }
            if (StringUtils.isNotBlank(template.getPassword()) && !JexlUtil.isExpressionValid(template.getPassword())) {
                sce.getElements().add("Invalid JEXL: " + template.getPassword());
            }

            checkJexl(template, sce);

            for (MembershipTO memb : template.getMemberships()) {
                checkJexl(memb, sce);
            }
        }
        if (taskTO.getRoleTemplate() != null) {
            RoleTO template = taskTO.getRoleTemplate();

            if (StringUtils.isNotBlank(template.getName()) && !JexlUtil.isExpressionValid(template.getName())) {
                sce.getElements().add("Invalid JEXL: " + template.getName());
            }

            checkJexl(template, sce);
        }
        if (!sce.isEmpty()) {
            throw sce;
        }

        // 2. all JEXL expressions are valid: accept user and role templates
        task.setUserTemplate(taskTO.getUserTemplate());
        task.setRoleTemplate(taskTO.getRoleTemplate());

        // 3. fill the remaining fields
        task.setPerformCreate(taskTO.isPerformCreate());
        task.setPerformUpdate(taskTO.isPerformUpdate());
        task.setPerformDelete(taskTO.isPerformDelete());
        task.setSyncStatus(taskTO.isSyncStatus());
        task.setFullReconciliation(taskTO.isFullReconciliation());

        task.setActionsClassName(taskTO.getActionsClassName());
    }

    public SchedTask createSchedTask(final SchedTaskTO taskTO, final TaskUtil taskUtil) {
        SchedTask task = taskUtil.newTask();
        task.setCronExpression(taskTO.getCronExpression());
        task.setName(taskTO.getName());
        task.setDescription(taskTO.getDescription());

        if (taskUtil.getType() == TaskType.SCHEDULED) {
            task.setJobClassName(taskTO.getJobClassName());
        }
        if (taskUtil.getType() == TaskType.SYNCHRONIZATION) {
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
        }

        return task;
    }

    public void updateSchedTask(final SchedTask task, final SchedTaskTO taskTO, final TaskUtil taskUtil) {
        task.setCronExpression(taskTO.getCronExpression());
        if (StringUtils.isNotBlank(taskTO.getName())) {
            task.setName(taskTO.getName());
        }
        if (StringUtils.isNotBlank(taskTO.getDescription())) {
            task.setDescription(taskTO.getDescription());
        }

        if (taskUtil.getType() == TaskType.SYNCHRONIZATION) {
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

    public <T extends AbstractTaskTO> T getTaskTO(final Task task, final TaskUtil taskUtil) {
        T taskTO = taskUtil.newTaskTO();
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
            taskTO.getExecutions().add(getTaskExecTO(execution));
        }

        switch (taskUtil.getType()) {
            case PROPAGATION:
                if (!(task instanceof PropagationTask)) {
                    throw new ClassCastException("taskUtil is type Propagation but task is not PropagationTask: "
                            + task.getClass().getName());
                }
                ((PropagationTaskTO) taskTO).setResource(((PropagationTask) task).getResource().getName());
                break;

            case SCHEDULED:
                if (!(task instanceof SchedTask)) {
                    throw new ClassCastException("taskUtil is type Sched but task is not SchedTask: "
                            + task.getClass().getName());
                }
                setExecTime((SchedTaskTO) taskTO, task);
                ((SchedTaskTO) taskTO).setName(((SchedTask) task).getName());
                ((SchedTaskTO) taskTO).setDescription(((SchedTask) task).getDescription());
                break;

            case SYNCHRONIZATION:
                if (!(task instanceof SyncTask)) {
                    throw new ClassCastException("taskUtil is type Sync but task is not SyncTask: "
                            + task.getClass().getName());
                }
                setExecTime((SchedTaskTO) taskTO, task);
                ((SyncTaskTO) taskTO).setName(((SyncTask) task).getName());
                ((SyncTaskTO) taskTO).setDescription(((SyncTask) task).getDescription());
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
