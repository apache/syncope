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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.core.persistence.api.entity.Exec;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskCallable;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
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

    @Resource(name = "propagationTaskExecutorAsyncExecutor")
    protected ThreadPoolTaskExecutor executor;

    /**
     * Creates new instances of {@link PropagationTaskCallable} for usage with
     * {@link java.util.concurrent.CompletionService}.
     *
     * @param taskInfo to be executed
     * @param reporter to report propagation execution status
     * @return new {@link PropagationTaskCallable} instance for usage with
     * {@link java.util.concurrent.CompletionService}
     */
    protected PropagationTaskCallable newPropagationTaskCallable(
            final PropagationTaskInfo taskInfo, final PropagationReporter reporter) {

        PropagationTaskCallable callable = (PropagationTaskCallable) ApplicationContextProvider.getBeanFactory().
                createBean(DefaultPropagationTaskCallable.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        callable.setTaskInfo(taskInfo);
        callable.setReporter(reporter);

        return callable;
    }

    @Override
    protected void doExecute(
            final Collection<PropagationTaskInfo> taskInfos,
            final PropagationReporter reporter,
            final boolean nullPriorityAsync) {

        Map<PropagationTaskInfo, ExternalResource> taskToResource = new HashMap<>(taskInfos.size());
        List<PropagationTaskInfo> prioritizedTasks = new ArrayList<>();

        int[] connRequestTimeout = { 60 };

        taskInfos.forEach(task -> {
            ExternalResource resource = resourceDAO.find(task.getResource());
            taskToResource.put(task, resource);

            if (resource.getPropagationPriority() != null) {
                prioritizedTasks.add(task);

                if (resource.getConnector().getConnRequestTimeout() != null
                        && connRequestTimeout[0] < resource.getConnector().getConnRequestTimeout()) {
                    connRequestTimeout[0] = resource.getConnector().getConnRequestTimeout();
                    LOG.debug("Upgrade request connection timeout to {}", connRequestTimeout);
                }
            }
        });

        Collections.sort(prioritizedTasks, new PriorityComparator(taskToResource));
        LOG.debug("Propagation tasks sorted by priority, for serial execution: {}", prioritizedTasks);

        Collection<PropagationTaskInfo> concurrentTasks = taskInfos.stream().
                filter(task -> !prioritizedTasks.contains(task)).collect(Collectors.toSet());
        LOG.debug("Propagation tasks for concurrent execution: {}", concurrentTasks);

        // first process priority resources sequentially and fail as soon as any propagation failure is reported
        prioritizedTasks.forEach(task -> {
            TaskExec execution = null;
            ExecStatus execStatus;
            try {
                execution = newPropagationTaskCallable(task, reporter).call();
                execStatus = ExecStatus.valueOf(execution.getStatus());
            } catch (Exception e) {
                LOG.error("Unexpected exception", e);
                execStatus = ExecStatus.FAILURE;
            }
            if (execStatus != ExecStatus.SUCCESS) {
                throw new PropagationException(task.getResource(), Optional.ofNullable(execution)
                    .map(Exec::getMessage).orElse(null));
            }
        });

        // then process non-priority resources concurrently...
        CompletionService<TaskExec> completionService = new ExecutorCompletionService<>(executor);
        Map<PropagationTaskInfo, Future<TaskExec>> nullPriority = new HashMap<>(concurrentTasks.size());
        concurrentTasks.forEach(taskInfo -> {
            try {
                nullPriority.put(
                        taskInfo,
                        completionService.submit(newPropagationTaskCallable(taskInfo, reporter)));
            } catch (Exception e) {
                LOG.error("Unexpected exception", e);
            }
        });
        // ...waiting for all callables to complete, if async processing was not required
        if (!nullPriority.isEmpty()) {
            if (nullPriorityAsync) {
                nullPriority.forEach((task, exec) -> reporter.onSuccessOrNonPriorityResourceFailures(task, ExecStatus.CREATED, null, null, null));
            } else {
                final Set<Future<TaskExec>> nullPriorityFutures = new HashSet<>(nullPriority.values());
                try {
                    executor.submit(() -> {
                        while (!nullPriorityFutures.isEmpty()) {
                            try {
                                nullPriorityFutures.remove(completionService.take());
                            } catch (Exception e) {
                                LOG.error("Unexpected exception", e);
                            }
                        }
                    }).get(connRequestTimeout[0], TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("Unexpected exception", e);
                } finally {
                    nullPriorityFutures.forEach(future -> future.cancel(true));
                    nullPriorityFutures.clear();
                    nullPriority.clear();
                }
            }
        }
    }

    /**
     * Compare propagation tasks according to related ExternalResource's priority.
     */
    protected static class PriorityComparator implements Comparator<PropagationTaskInfo>, Serializable {

        private static final long serialVersionUID = -1969355670784448878L;

        private final Map<PropagationTaskInfo, ExternalResource> taskToResource;

        public PriorityComparator(final Map<PropagationTaskInfo, ExternalResource> taskToResource) {
            this.taskToResource = taskToResource;
        }

        @Override
        public int compare(final PropagationTaskInfo task1, final PropagationTaskInfo task2) {
            int prop1 = taskToResource.get(task1).getPropagationPriority();
            int prop2 = taskToResource.get(task2).getPropagationPriority();

            return prop1 > prop2
                    ? 1
                    : prop1 == prop2
                            ? 0
                            : -1;
        }
    }
}
