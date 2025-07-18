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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Exec;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Sorts the tasks to be executed according to related
 * {@link org.apache.syncope.core.persistence.api.entity.ExternalResource}'s priority, then execute.
 * Tasks related to resources with NULL priority are executed after other tasks, concurrently.
 * Failure during execution of a task related to resource with non-NULL priority are treated as fatal and will interrupt
 * the whole process, resulting in a global failure.
 */
public class PriorityPropagationTaskExecutor extends AbstractPropagationTaskExecutor {

    protected final AsyncTaskExecutor taskExecutor;

    public PriorityPropagationTaskExecutor(
            final ConnectorManager connectorManager,
            final ConnObjectUtils connObjectUtils,
            final TaskDAO taskDAO,
            final ExternalResourceDAO resourceDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final TaskDataBinder taskDataBinder,
            final AnyUtilsFactory anyUtilsFactory,
            final TaskUtilsFactory taskUtilsFactory,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final ApplicationEventPublisher publisher,
            final AsyncTaskExecutor taskExecutor) {

        super(connectorManager,
                connObjectUtils,
                taskDAO,
                resourceDAO,
                plainSchemaDAO,
                notificationManager,
                auditManager,
                taskDataBinder,
                anyUtilsFactory,
                taskUtilsFactory,
                outboundMatcher,
                validator,
                publisher);
        this.taskExecutor = taskExecutor;
    }

    /**
     * Creates new instances of {@link Callable} for usage with{@link java.util.concurrent.CompletionService}.
     *
     * @param taskInfo to be executed
     * @param reporter to report propagation execution status
     * @param executor user that triggered the propagation execution
     * @return new {@link Callable} instance for usage with {@link java.util.concurrent.CompletionService}
     */
    protected Callable<TaskExec<PropagationTask>> newPropagationTaskCallable(
            final PropagationTaskInfo taskInfo, final PropagationReporter reporter, final String executor) {

        String domain = AuthContextUtils.getDomain();
        Set<String> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().
                map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        return () -> AuthContextUtils.callAs(domain, executor, authorities, () -> {
            LOG.debug("Execution started for {}", taskInfo);

            TaskExec<PropagationTask> execution = this.execute(taskInfo, reporter, executor);

            LOG.debug("Execution completed for {} with results {}", taskInfo, execution);

            return execution;
        });
    }

    protected boolean failed(
            final PropagationTaskInfo taskInfpo,
            final TaskExec<PropagationTask> exec,
            final ExecStatus execStatus) {

        return execStatus == ExecStatus.FAILURE;
    }

    @Override
    public PropagationReporter execute(
            final Collection<PropagationTaskInfo> taskInfos,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationReporter reporter = new DefaultPropagationReporter();
        try {
            List<PropagationTaskInfo> prioritizedTasks = taskInfos.stream().
                    filter(task -> task.getResource().getPropagationPriority() != null).
                    sorted(Comparator.comparing(task -> task.getResource().getPropagationPriority())).
                    toList();
            LOG.debug("Propagation tasks sorted by priority, for serial execution: {}", prioritizedTasks);

            List<PropagationTaskInfo> concurrentTasks = taskInfos.stream().
                    filter(task -> !prioritizedTasks.contains(task)).
                    toList();
            LOG.debug("Propagation tasks for concurrent execution: {}", concurrentTasks);

            // first process priority resources sequentially and fail as soon as any propagation failure is reported
            prioritizedTasks.forEach(taskInfo -> {
                TaskExec<PropagationTask> exec = null;
                ExecStatus execStatus;
                String errorMessage = null;
                try {
                    exec = newPropagationTaskCallable(taskInfo, reporter, executor).call();
                    execStatus = ExecStatus.valueOf(exec.getStatus());
                } catch (Exception e) {
                    LOG.error("Unexpected exception", e);
                    execStatus = ExecStatus.FAILURE;
                    errorMessage = e.getMessage();
                }
                if (failed(taskInfo, exec, execStatus)) {
                    throw new PropagationException(
                            taskInfo.getResource().getKey(),
                            Optional.ofNullable(exec).map(Exec::getMessage).orElse(errorMessage));
                }
            });

            // then process non-priority resources concurrently...
            if (!concurrentTasks.isEmpty()) {
                CompletionService<TaskExec<PropagationTask>> completionService =
                        new ExecutorCompletionService<>(taskExecutor);
                List<Future<TaskExec<PropagationTask>>> futures = new ArrayList<>();

                concurrentTasks.forEach(taskInfo -> {
                    try {
                        futures.add(completionService.submit(newPropagationTaskCallable(taskInfo, reporter, executor)));

                        if (nullPriorityAsync) {
                            reporter.onSuccessOrNonPriorityResourceFailures(
                                    taskInfo, ExecStatus.CREATED, null, null, null, null);
                        }
                    } catch (Exception e) {
                        LOG.error("While submitting task for async execution: {}", taskInfo, e);
                        rejected(taskInfo, e.getMessage(), reporter, executor);
                    }
                });

                // ...waiting for all callables to complete, if async processing was not required
                if (!nullPriorityAsync) {
                    futures.forEach(future -> {
                        try {
                            future.get();
                        } catch (Exception e) {
                            LOG.error("Unexpected exception", e);
                        }
                    });
                }
            }
        } catch (PropagationException e) {
            LOG.error("Error propagation priority resource", e);
            reporter.onPriorityResourceFailure(e.getResourceName(), taskInfos);
        }

        return reporter;
    }
}
