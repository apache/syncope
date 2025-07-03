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
package org.apache.syncope.core.spring.task;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;

public class VirtualThreadPoolTaskExecutor
        extends ExecutorConfigurationSupport
        implements AsyncTaskExecutor, SchedulingTaskExecutor {

    private static final long serialVersionUID = 4747270938984213408L;

    private int poolSize = -1;

    private long taskTerminationTimeout;

    private TaskDecorator taskDecorator;

    private SimpleAsyncTaskExecutor executor;

    /**
     * Set the the maximum number of managed threads.
     *
     * @param poolSize the value to set (default is {@code Integer.MAX_VALUE})
     */
    public void setPoolSize(final int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Specify a timeout (in milliseconds) for task termination when closing
     * this executor. The default is 0, not waiting for task termination at all.
     * <p>
     * Note that a concrete >0 timeout specified here will lead to the
     * wrapping of every submitted task into a task-tracking runnable which
     * involves considerable overhead in case of a high number of tasks.
     * However, for a modest level of submissions with longer-running
     * tasks, this is feasible in order to arrive at a graceful shutdown.
     * <p>
     * Note that {@code SimpleAsyncTaskExecutor} does not participate in
     * a coordinated lifecycle stop but rather just awaits task termination
     * on {@link #close()}.
     *
     * @param taskTerminationTimeout the timeout in milliseconds
     * @see SimpleAsyncTaskExecutor#close
     */
    public void setTaskTerminationTimeout(final long taskTerminationTimeout) {
        this.taskTerminationTimeout = taskTerminationTimeout;
    }

    /**
     * Specify a custom {@link TaskDecorator} to be applied to any {@link Runnable}
     * about to be executed.
     * <p>
     * Note that such a decorator is not necessarily being applied to the
     * user-supplied {@code Runnable}/{@code Callable} but rather to the actual
     * execution callback (which may be a wrapper around the user-supplied task).
     * </p>
     * <p>
     * The primary use case is to set some execution context around the task's
     * invocation, or to provide some monitoring/statistics for task execution.
     * </p>
     * <p>
     * <b>NOTE:</b> Exception handling in {@code TaskDecorator} implementations
     * is limited to plain {@code Runnable} execution via {@code execute} calls.
     * In case of {@code #submit} calls, the exposed {@code Runnable} will be a
     * {@code FutureTask} which does not propagate any exceptions; you might
     * have to cast it and call {@code Future#get} to evaluate exceptions.
     * See the {@code ThreadPoolExecutor#afterExecute} javadoc for an example
     * of how to access exceptions in such a {@code Future} case.
     * </p>
     *
     * @param taskDecorator value to set
     */
    public void setTaskDecorator(final TaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }

    @Override
    protected ExecutorService initializeExecutor(
            final ThreadFactory threadFactory,
            final RejectedExecutionHandler rejectedExecutionHandler) {

        executor = new SimpleAsyncTaskExecutor(getThreadNamePrefix());
        executor.setVirtualThreads(true);
        executor.setDaemon(true);
        executor.setConcurrencyLimit(poolSize);
        if (taskTerminationTimeout >= 0) {
            executor.setTaskTerminationTimeout(taskTerminationTimeout);
        }
        Optional.ofNullable(taskDecorator).ifPresent(executor::setTaskDecorator);

        return new AbstractExecutorService() {

            @Override
            public void execute(final Runnable task) {
                executor.execute(task);
            }

            @Override
            public void shutdown() {
                executor.close();
            }

            @Override
            public List<Runnable> shutdownNow() {
                executor.close();
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return !executor.isActive();
            }

            @Override
            public boolean isTerminated() {
                return !executor.isActive();
            }

            @Override
            public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
                return !executor.isActive();
            }
        };
    }

    @Override
    public void execute(final Runnable task) {
        executor.execute(task);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public void shutdown() {
        executor.close();
    }
}
