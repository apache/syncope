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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Collection;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;

/**
 * Execute propagation tasks.
 *
 * @see PropagationTaskTO
 */
@SuppressWarnings("squid:S1214")
public interface PropagationTaskExecutor {

    /**
     * Remove any RetryTemplate defined for the given External Resource from local cache.
     *
     * @param resource External Resource name
     */
    void expireRetryTemplate(String resource);

    /**
     * Execute the given task and returns the generated {@link TaskExec}.
     *
     * @param taskInfo to be executed
     * @param reporter to report propagation execution status
     * @param executor the executor of this task
     * @return the generated TaskExec
     */
    TaskExec<PropagationTask> execute(PropagationTaskInfo taskInfo, PropagationReporter reporter, String executor);

    /**
     * Execute the given collection of tasks.
     * The process is interrupted as soon as the result of the communication with a resource with non-null priority is
     * in error.
     *
     * @param taskInfos to be execute, in given order
     * @param nullPriorityAsync asynchronously executes tasks related to resources with no priority
     * @param executor the executor of this task
     * @return reporter to report propagation execution status
     */
    PropagationReporter execute(Collection<PropagationTaskInfo> taskInfos, boolean nullPriorityAsync, String executor);
}
