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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Exec;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskCallable;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Sorts the tasks to be executed according to related
 * {@link org.apache.syncope.core.persistence.api.entity.resource.ExternalResource}'s priority, then execute.
 * Tasks related to resources with NULL priority are executed after other tasks, concurrently.
 * Failure during execution of a task related to resource with non-NULL priority are treated as fatal and will interrupt
 * the whole process, resulting in a global failure.
 */
public class PriorityPropagationTaskExecutor extends AbstractPropagationTaskExecutor {

    /**
     * Creates new instances of {@link PropagationTaskCallable} for usage with
     * {@link java.util.concurrent.CompletionService}.
     *
     * @param taskInfo to be executed
     * @param reporter to report propagation execution status
     * @param executor user that triggered the propagation execution
     * @return new {@link PropagationTaskCallable} instance for usage with
     * {@link java.util.concurrent.CompletionService}
     */
    protected static PropagationTaskCallable newPropagationTaskCallable(
            final PropagationTaskInfo taskInfo, final PropagationReporter reporter, final String executor) {

        PropagationTaskCallable callable = (PropagationTaskCallable) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultPropagationTaskCallable.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        callable.setTaskInfo(taskInfo);
        callable.setReporter(reporter);
        callable.setExecutor(executor);

        return callable;
    }

    protected final ThreadPoolTaskExecutor taskExecutor;

    public PriorityPropagationTaskExecutor(
            final ConnectorManager connectorManager,
            final ConnObjectUtils connObjectUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final TaskDAO taskDAO,
            final ExternalResourceDAO resourceDAO,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final TaskDataBinder taskDataBinder,
            final AnyUtilsFactory anyUtilsFactory,
            final TaskUtilsFactory taskUtilsFactory,
            final EntityFactory entityFactory,
            final OutboundMatcher outboundMatcher,
            final ThreadPoolTaskExecutor taskExecutor) {

        super(connectorManager,
                connObjectUtils,
                userDAO,
                groupDAO,
                anyObjectDAO,
                taskDAO,
                resourceDAO,
                notificationManager,
                auditManager,
                taskDataBinder,
                anyUtilsFactory,
                taskUtilsFactory,
                entityFactory,
                outboundMatcher);
        this.taskExecutor = taskExecutor;
    }

    @Override
    public PropagationReporter execute(
            final Collection<PropagationTaskInfo> taskInfos,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationReporter reporter = new DefaultPropagationReporter();
        try {
            List<PropagationTaskInfo> prioritizedTasks = taskInfos.stream().
                    filter(task -> task.getExternalResource().getPropagationPriority() != null).
                    sorted(Comparator.comparing(task -> task.getExternalResource().getPropagationPriority())).
                    collect(Collectors.toList());
            LOG.debug("Propagation tasks sorted by priority, for serial execution: {}", prioritizedTasks);

            List<PropagationTaskInfo> concurrentTasks = taskInfos.stream().
                    filter(task -> !prioritizedTasks.contains(task)).
                    collect(Collectors.toList());
            LOG.debug("Propagation tasks for concurrent execution: {}", concurrentTasks);

            // first process priority resources sequentially and fail as soon as any propagation failure is reported
            prioritizedTasks.forEach(task -> {
                TaskExec exec = null;
                ExecStatus execStatus;
                String errorMessage = null;
                try {
                    exec = newPropagationTaskCallable(task, reporter, executor).call();
                    execStatus = ExecStatus.valueOf(exec.getStatus());
                } catch (Exception e) {
                    LOG.error("Unexpected exception", e);
                    execStatus = ExecStatus.FAILURE;
                    errorMessage = e.getMessage();
                }
                if (execStatus != ExecStatus.SUCCESS) {
                    throw new PropagationException(
                            task.getResource(),
                            Optional.ofNullable(exec).map(Exec::getMessage).orElse(errorMessage));
                }
            });

            // then process non-priority resources concurrently...
            if (!concurrentTasks.isEmpty()) {
                CompletionService<TaskExec> completionService = new ExecutorCompletionService<>(taskExecutor);
                List<Future<TaskExec>> futures = new ArrayList<>();

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
