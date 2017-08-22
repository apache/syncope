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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskCallable;
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
     * @param task to be executed
     * @param reporter to report propagation execution status
     * @return new {@link PropagationTaskCallable} instance for usage with
     * {@link java.util.concurrent.CompletionService}
     */
    protected PropagationTaskCallable newPropagationTaskCallable(
            final PropagationTask task, final PropagationReporter reporter) {

        PropagationTaskCallable callable = (PropagationTaskCallable) ApplicationContextProvider.getBeanFactory().
                createBean(PropagationTaskCallableImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
        callable.setExecutor(this);
        callable.setTask(task);
        callable.setReporter(reporter);

        return callable;
    }

    @Override
    protected void doExecute(
            final Collection<PropagationTask> tasks,
            final PropagationReporter reporter,
            final boolean nullPriorityAsync) {

        List<PropagationTask> prioritizedTasks = tasks.stream().
                filter(task -> task.getResource().getPropagationPriority() != null).collect(Collectors.toList());
        Collections.sort(prioritizedTasks, new PriorityComparator());
        LOG.debug("Propagation tasks sorted by priority, for serial execution: {}", prioritizedTasks);

        Collection<PropagationTask> concurrentTasks = tasks.stream().
                filter(task -> !prioritizedTasks.contains(task)).collect(Collectors.toSet());
        LOG.debug("Propagation tasks for concurrent execution: {}", concurrentTasks);

        // first process priority resources sequentially and fail as soon as any propagation failure is reported
        prioritizedTasks.forEach(task -> {
            TaskExec execution = null;
            PropagationTaskExecStatus execStatus;
            try {
                execution = newPropagationTaskCallable(task, reporter).call();
                execStatus = PropagationTaskExecStatus.valueOf(execution.getStatus());
            } catch (Exception e) {
                LOG.error("Unexpected exception", e);
                execStatus = PropagationTaskExecStatus.FAILURE;
            }
            if (execStatus != PropagationTaskExecStatus.SUCCESS) {
                throw new PropagationException(
                        task.getResource().getKey(), execution == null ? null : execution.getMessage());
            }
        });

        // then process non-priority resources concurrently...
        final CompletionService<TaskExec> completionService = new ExecutorCompletionService<>(executor);
        Map<PropagationTask, Future<TaskExec>> nullPriority = new HashMap<>(concurrentTasks.size());
        concurrentTasks.forEach(task -> {
            try {
                nullPriority.put(
                        task,
                        completionService.submit(newPropagationTaskCallable(task, reporter)));
            } catch (Exception e) {
                LOG.error("Unexpected exception", e);
            }
        });
        // ...waiting for all callables to complete, if async processing was not required
        if (!nullPriority.isEmpty()) {
            if (nullPriorityAsync) {
                nullPriority.entrySet().forEach(entry -> {
                    reporter.onSuccessOrNonPriorityResourceFailures(
                            entry.getKey(), PropagationTaskExecStatus.CREATED, null, null, null);
                });
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
                    }).get(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("Unexpected exception", e);
                } finally {
                    nullPriorityFutures.forEach(future -> {
                        future.cancel(true);
                    });
                    nullPriorityFutures.clear();
                    nullPriority.clear();
                }
            }
        }
    }

    /**
     * Compare propagation tasks according to related ExternalResource's priority.
     */
    protected static class PriorityComparator implements Comparator<PropagationTask>, Serializable {

        private static final long serialVersionUID = -1969355670784448878L;

        @Override
        public int compare(final PropagationTask task1, final PropagationTask task2) {
            int prop1 = task1.getResource().getPropagationPriority();
            int prop2 = task2.getResource().getPropagationPriority();

            return prop1 > prop2
                    ? 1
                    : prop1 == prop2
                            ? 0
                            : -1;
        }
    }

}
