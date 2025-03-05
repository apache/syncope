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
package org.apache.syncope.core.provisioning.api.job;

import org.apache.syncope.common.lib.types.TaskType;

@FunctionalInterface
public interface SchedTaskJobDelegate {

    /**
     * Executes a Job to run the given Task.
     *
     * @param taskType Type of task to run
     * @param taskKey Task key to run
     * @param context execution context, can be used to pass parameters to the job
     * @throws JobExecutionException if anything goes wrong
     */
    void execute(
            TaskType taskType,
            String taskKey,
            JobExecutionContext context) throws JobExecutionException;
}
