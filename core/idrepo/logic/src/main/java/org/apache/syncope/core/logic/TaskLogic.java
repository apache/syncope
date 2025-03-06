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
package org.apache.syncope.core.logic;

import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.job.MacroJobDelegate;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationReporter;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class TaskLogic extends AbstractExecutableLogic<TaskTO> {

    protected final TaskDAO taskDAO;

    protected final TaskExecDAO taskExecDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final NotificationDAO notificationDAO;

    protected final TaskDataBinder binder;

    protected final PropagationTaskExecutor taskExecutor;

    protected final NotificationJobDelegate notificationJobDelegate;

    protected final TaskUtilsFactory taskUtilsFactory;

    public TaskLogic(
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final TaskDAO taskDAO,
            final TaskExecDAO taskExecDAO,
            final ExternalResourceDAO resourceDAO,
            final NotificationDAO notificationDAO,
            final TaskDataBinder binder,
            final PropagationTaskExecutor taskExecutor,
            final NotificationJobDelegate notificationJobDelegate,
            final TaskUtilsFactory taskUtilsFactory) {

        super(jobManager, scheduler, jobStatusDAO);

        this.taskDAO = taskDAO;
        this.taskExecDAO = taskExecDAO;
        this.resourceDAO = resourceDAO;
        this.notificationDAO = notificationDAO;
        this.binder = binder;
        this.taskExecutor = taskExecutor;
        this.notificationJobDelegate = notificationJobDelegate;
        this.taskUtilsFactory = taskUtilsFactory;
    }

    protected void securityChecks(final String entitlement, final String realm) {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().getOrDefault(entitlement, Set.of());
        if (authRealms.isEmpty() || authRealms.stream().noneMatch(realm::startsWith)) {
            throw new DelegatedAdministrationException(realm, MacroTask.class.getSimpleName(), null);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_CREATE + "')")
    public <T extends SchedTaskTO> T createSchedTask(final TaskType type, final T taskTO) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(taskTO);
        if (taskUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + taskUtils.getType());
            throw sce;
        }

        if (taskUtils.getType() == TaskType.MACRO) {
            securityChecks(IdRepoEntitlement.TASK_CREATE, ((MacroTaskTO) taskTO).getRealm());
        }

        SchedTask task = binder.createSchedTask(taskTO, taskUtils);
        task = taskDAO.save(task);

        try {
            jobManager.register(
                    task,
                    task.getStartAt(),
                    AuthContextUtils.getUsername(),
                    false,
                    Map.of());
        } catch (Exception e) {
            LOG.error("While registering job for task {}", task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils, false);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_UPDATE + "')")
    public <T extends SchedTaskTO> T updateSchedTask(final TaskType type, final SchedTaskTO taskTO) {
        SchedTask task = taskDAO.<SchedTask>findById(type, taskTO.getKey()).
                orElseThrow(() -> new NotFoundException("Task " + taskTO.getKey()));

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);
        if (taskUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + taskUtils.getType());
            throw sce;
        }

        if (taskUtils.getType() == TaskType.MACRO) {
            securityChecks(IdRepoEntitlement.TASK_UPDATE, ((MacroTask) task).getRealm().getFullPath());
            securityChecks(IdRepoEntitlement.TASK_UPDATE, ((MacroTaskTO) taskTO).getRealm());
        }

        binder.updateSchedTask(task, taskTO, taskUtils);
        task = taskDAO.save(task);
        try {
            jobManager.register(
                    task,
                    task.getStartAt(),
                    AuthContextUtils.getUsername(),
                    false,
                    Map.of());
        } catch (Exception e) {
            LOG.error("While registering job for task {}", task.getKey(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return binder.getTaskTO(task, taskUtils, false);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_LIST + "')")
    @Transactional(readOnly = true)
    public <T extends TaskTO> Page<T> search(
            final TaskType type,
            final String resource,
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final Pageable pageable,
            final boolean details) {

        try {
            if (type == null) {
                throw new IllegalArgumentException("type is required");
            }

            ExternalResource resourceObj = resource == null
                    ? null
                    : resourceDAO.findById(resource).
                            orElseThrow(() -> new IllegalArgumentException("Missing ExternalResource: " + resource));

            Notification notificationObj = notification == null
                    ? null
                    : notificationDAO.findById(notification).
                            orElseThrow(() -> new IllegalArgumentException("Missing Notification: " + notification));

            long count = taskDAO.count(
                    type,
                    resourceObj,
                    notificationObj,
                    anyTypeKind,
                    entityKey);

            List<T> result = taskDAO.findAll(
                    type,
                    resourceObj,
                    notificationObj,
                    anyTypeKind,
                    entityKey,
                    pageable).stream().
                    <T>map(task -> binder.getTaskTO(task, taskUtilsFactory.getInstance(type), details)).
                    toList();

            return new SyncopePage<>(result, pageable, count);
        } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_READ + "')")
    @Transactional(readOnly = true)
    public <T extends TaskTO> T read(final TaskType type, final String key, final boolean details) {
        Task<?> task = taskDAO.findById(type, key).
                orElseThrow(() -> new NotFoundException("Task " + key));

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);
        if (type != null && taskUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + taskUtils.getType());
            throw sce;
        }

        if (taskUtils.getType() == TaskType.MACRO) {
            securityChecks(IdRepoEntitlement.TASK_READ, ((MacroTask) task).getRealm().getFullPath());
        }

        return binder.getTaskTO(task, taskUtilsFactory.getInstance(task), details);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_READ + "')")
    @Transactional(readOnly = true)
    public SyncopeForm getMacroTaskForm(final String key, final Locale locale) {
        MacroTask task = taskDAO.findById(TaskType.MACRO, key).
                filter(MacroTask.class::isInstance).map(MacroTask.class::cast).
                orElseThrow(() -> new NotFoundException("MacroTask " + key));

        securityChecks(IdRepoEntitlement.TASK_READ, task.getRealm().getFullPath());

        return binder.getMacroTaskForm(task, locale);
    }

    protected ExecTO doExecute(
            final Task<?> task,
            final OffsetDateTime startAt,
            final boolean dryRun,
            final Map<String, Object> additionalDataMap) {

        if (startAt != null && startAt.isBefore(OffsetDateTime.now())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add("Cannot schedule in the past");
            throw sce;
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);
        String executor = AuthContextUtils.getUsername();

        ExecTO result = null;
        switch (taskUtils.getType()) {
            case PROPAGATION:
                PropagationTask propagationTask = (PropagationTask) task;
                PropagationTaskInfo taskInfo = new PropagationTaskInfo(
                        propagationTask.getResource(),
                        propagationTask.getOperation(),
                        new ObjectClass(propagationTask.getObjectClassName()),
                        propagationTask.getAnyTypeKind(),
                        propagationTask.getAnyType(),
                        propagationTask.getEntityKey(),
                        propagationTask.getConnObjectKey(),
                        propagationTask.getPropagationData());
                taskInfo.setKey(propagationTask.getKey());
                taskInfo.setOldConnObjectKey(propagationTask.getOldConnObjectKey());

                TaskExec<PropagationTask> propExec = taskExecutor.execute(
                        taskInfo, new DefaultPropagationReporter(), executor);
                result = binder.getExecTO(propExec);
                break;

            case NOTIFICATION:
                TaskExec<NotificationTask> notExec = notificationJobDelegate.executeSingle(
                        (NotificationTask) task, executor);
                result = binder.getExecTO(notExec);
                break;

            case SCHEDULED:
            case LIVE_SYNC:
            case PULL:
            case PUSH:
            case MACRO:
                if (!((SchedTask) task).isActive()) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add("Task " + task.getKey() + " is not active");
                    throw sce;
                }

                if (taskUtils.getType() == TaskType.MACRO) {
                    securityChecks(IdRepoEntitlement.TASK_EXECUTE, ((MacroTask) task).getRealm().getFullPath());
                }

                try {
                    jobManager.register(
                            (SchedTask) task,
                            Optional.ofNullable(startAt).orElseGet(OffsetDateTime::now),
                            executor,
                            dryRun,
                            additionalDataMap);
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
                    sce.getElements().add(e.getMessage());
                    throw sce;
                }

                result = new ExecTO();
                result.setJobType(JobType.TASK);
                result.setRefKey(task.getKey());
                result.setRefDesc(binder.buildRefDesc(task));
                result.setStart(OffsetDateTime.now());
                result.setExecutor(executor);
                result.setStatus(JobStatusDAO.JOB_FIRED_STATUS);
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Override
    public ExecTO execute(final ExecSpecs specs) {
        Task<?> task = taskDAO.findById(specs.getKey()).
                orElseThrow(() -> new NotFoundException("Task " + specs.getKey()));

        return doExecute(
                task,
                specs.getStartAt(),
                specs.getDryRun(),
                Map.of());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    public ExecTO execute(final ExecSpecs specs, final SyncopeForm macroTaskForm) {
        MacroTask task = taskDAO.findById(specs.getKey()).
                filter(MacroTask.class::isInstance).map(MacroTask.class::cast).
                orElseThrow(() -> new NotFoundException("MacroTask " + specs.getKey()));

        return doExecute(
                task,
                specs.getStartAt(),
                specs.getDryRun(),
                Map.of(MacroJobDelegate.MACRO_TASK_FORM_JOBDETAIL_KEY, macroTaskForm));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_DELETE + "')")
    public <T extends TaskTO> T delete(final TaskType type, final String key) {
        Task<?> task = taskDAO.findById(type, key).
                orElseThrow(() -> new NotFoundException("Task " + key));

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);
        if (type != null && taskUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + taskUtils.getType());
            throw sce;
        }

        if (taskUtils.getType() == TaskType.MACRO) {
            securityChecks(IdRepoEntitlement.TASK_DELETE, ((MacroTask) task).getRealm().getFullPath());
        }

        T taskToDelete = binder.getTaskTO(task, taskUtils, true);

        if (TaskType.SCHEDULED == taskUtils.getType()
                || TaskType.LIVE_SYNC == taskUtils.getType()
                || TaskType.PULL == taskUtils.getType()
                || TaskType.PUSH == taskUtils.getType()
                || TaskType.MACRO == taskUtils.getType()) {

            jobManager.unregister(task);
        }

        taskDAO.delete(task);
        return taskToDelete;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_READ + "')")
    @Override
    public Page<ExecTO> listExecutions(
            final String key,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        Task<?> task = taskDAO.findById(key).orElseThrow(() -> new NotFoundException("Task " + key));

        if (task instanceof MacroTask macroTask) {
            securityChecks(IdRepoEntitlement.TASK_READ, macroTask.getRealm().getFullPath());
        }

        long count = taskExecDAO.count(task, before, after);

        List<ExecTO> result = taskExecDAO.findAll(task, before, after, pageable).stream().
                map(binder::getExecTO).toList();

        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_LIST + "')")
    @Override
    public List<ExecTO> listRecentExecutions(final int max) {
        return taskExecDAO.findRecent(max).stream().
                map(exec -> {
                    try {
                        if (exec.getTask() instanceof MacroTask macroTask) {
                            securityChecks(IdRepoEntitlement.TASK_DELETE, macroTask.getRealm().getFullPath());
                        }

                        return binder.getExecTO(exec);
                    } catch (DelegatedAdministrationException e) {
                        LOG.error("Skip executions for command task", e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_DELETE + "')")
    @Override
    public ExecTO deleteExecution(final String execKey) {
        TaskExec<?> exec = taskExecDAO.findById(execKey).
                orElseThrow(() -> new NotFoundException("Task execution " + execKey));

        if (exec.getTask() instanceof MacroTask macroTask) {
            securityChecks(IdRepoEntitlement.TASK_DELETE, macroTask.getRealm().getFullPath());
        }

        ExecTO executionToDelete = binder.getExecTO(exec);
        taskExecDAO.delete(exec);
        return executionToDelete;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_DELETE + "')")
    @Override
    public List<BatchResponseItem> deleteExecutions(
            final String key,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        Task<?> task = taskDAO.findById(key).orElseThrow(() -> new NotFoundException("Task " + key));

        List<BatchResponseItem> batchResponseItems = new ArrayList<>();

        taskExecDAO.findAll(task, before, after, Pageable.unpaged()).forEach(exec -> {
            BatchResponseItem item = new BatchResponseItem();
            item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(exec.getKey()));
            batchResponseItems.add(item);

            try {
                if (exec.getTask() instanceof MacroTask macroTask) {
                    securityChecks(IdRepoEntitlement.TASK_DELETE, macroTask.getRealm().getFullPath());
                }

                taskExecDAO.delete(exec);
                item.setStatus(Response.Status.OK.getStatusCode());
            } catch (Exception e) {
                LOG.error("Error deleting execution {} of task {}", exec.getKey(), key, e);
                item.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                item.setContent(ExceptionUtils2.getFullStackTrace(e));
            }
        });

        return batchResponseItems;
    }

    @Override
    protected Triple<JobType, String, String> getReference(final String jobName) {
        return JobNamer.getTaskKeyFromJobName(jobName).
                flatMap(taskDAO::findById).filter(SchedTask.class::isInstance).
                map(t -> Triple.of(JobType.TASK, t.getKey(), binder.buildRefDesc(t))).
                orElse(null);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_LIST + "')")
    @Override
    public List<JobTO> listJobs() {
        return super.doListJobs(true);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_READ + "')")
    @Override
    public JobTO getJob(final String key) {
        Task<?> task = taskDAO.findById(key).orElseThrow(() -> new NotFoundException("Task " + key));

        if (task instanceof MacroTask macroTask) {
            securityChecks(IdRepoEntitlement.TASK_READ, macroTask.getRealm().getFullPath());
        }

        return getJobTO(JobNamer.getJobName(task), false).
                orElseThrow(() -> new NotFoundException("Job for task " + key));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Override
    public void actionJob(final String key, final JobAction action) {
        Task<?> task = taskDAO.findById(key).orElseThrow(() -> new NotFoundException("Task " + key));

        if (task instanceof MacroTask macroTask) {
            securityChecks(IdRepoEntitlement.TASK_EXECUTE, macroTask.getRealm().getFullPath());
        }

        doActionJob(JobNamer.getJobName(task), action);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_DELETE + "')")
    public List<PropagationTaskTO> purgePropagations(
            final OffsetDateTime since,
            final List<ExecStatus> statuses,
            final List<String> resources) {

        return taskDAO.purgePropagations(since, statuses, resources);
    }

    @Override
    protected TaskTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)
                && !"deleteExecution".equals(method.getName()) && !"readExecution".equals(method.getName())) {

            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof TaskTO taskTO) {
                    key = taskTO.getKey();
                }
            }
        }

        if (key != null) {
            String taskKey = key;
            try {
                Task<?> task = taskDAO.findById(taskKey).orElseThrow(() -> new NotFoundException("Task " + taskKey));
                return binder.getTaskTO(task, taskUtilsFactory.getInstance(task), false);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
