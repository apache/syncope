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
package org.apache.syncope.common.lib.types;

import java.io.Serializable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ThreadPoolSettings implements Serializable {

    private static final long serialVersionUID = -3860071577309258396L;

    public enum RejectionPolicy {
        ABORT(new ThreadPoolExecutor.AbortPolicy()),
        CALLER_RUNS(new ThreadPoolExecutor.CallerRunsPolicy()),
        DISCARD(new ThreadPoolExecutor.DiscardPolicy()),
        DISCARD_OLDEST(new ThreadPoolExecutor.DiscardOldestPolicy());

        private final RejectedExecutionHandler handler;

        RejectionPolicy(final RejectedExecutionHandler handler) {
            this.handler = handler;
        }

        public RejectedExecutionHandler getHandler() {
            return handler;
        }
    }

    private int corePoolSize = 1;

    private int maxPoolSize = Integer.MAX_VALUE;

    private int keepAliveSeconds = 60;

    private int queueCapacity = Integer.MAX_VALUE;

    private boolean allowCoreThreadTimeOut = false;

    private boolean prestartAllCoreThreads = false;

    private boolean waitForTasksToCompleteOnShutdown = false;

    private int awaitTerminationSeconds = 0;

    private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(final int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(final int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(final int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(final int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(final boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public boolean isPrestartAllCoreThreads() {
        return prestartAllCoreThreads;
    }

    public void setPrestartAllCoreThreads(final boolean prestartAllCoreThreads) {
        this.prestartAllCoreThreads = prestartAllCoreThreads;
    }

    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    public void setWaitForTasksToCompleteOnShutdown(final boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    public void setAwaitTerminationSeconds(final int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    public RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    public void setRejectionPolicy(final RejectionPolicy rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(corePoolSize).
                append(maxPoolSize).
                append(keepAliveSeconds).
                append(queueCapacity).
                append(allowCoreThreadTimeOut).
                append(prestartAllCoreThreads).
                append(waitForTasksToCompleteOnShutdown).
                append(awaitTerminationSeconds).
                append(rejectionPolicy).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ThreadPoolSettings other = (ThreadPoolSettings) obj;
        return new EqualsBuilder().
                append(corePoolSize, other.corePoolSize).
                append(maxPoolSize, other.maxPoolSize).
                append(keepAliveSeconds, other.keepAliveSeconds).
                append(queueCapacity, other.queueCapacity).
                append(allowCoreThreadTimeOut, other.allowCoreThreadTimeOut).
                append(prestartAllCoreThreads, other.prestartAllCoreThreads).
                append(waitForTasksToCompleteOnShutdown, other.waitForTasksToCompleteOnShutdown).
                append(awaitTerminationSeconds, other.awaitTerminationSeconds).
                append(rejectionPolicy, other.rejectionPolicy).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                append(corePoolSize).
                append(maxPoolSize).
                append(keepAliveSeconds).
                append(queueCapacity).
                append(allowCoreThreadTimeOut).
                append(prestartAllCoreThreads).
                append(waitForTasksToCompleteOnShutdown).
                append(awaitTerminationSeconds).
                append(rejectionPolicy).
                build();
    }
}
