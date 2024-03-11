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

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;

public class VirtualThreadPoolTaskExecutor
        extends ExecutorConfigurationSupport
        implements AsyncTaskExecutor, SchedulingTaskExecutor {

    private static final long serialVersionUID = 4747270938984213408L;

    private int poolSize = -1;

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
     * @return the maximum number of managed threads
     */
    public int getPoolSize() {
        return poolSize;
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
        executor.setConcurrencyLimit(poolSize);
        Optional.ofNullable(taskDecorator).ifPresent(executor::setTaskDecorator);

        return new ExecutorServiceAdapter(executor);
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
        // manual shutdown is not supported
    }
}
