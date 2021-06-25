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

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public interface SchedTaskJobDelegate extends JobDelegate {

    /**
     * Executes a Quartz Job to run the given Task.
     *
     * @param taskKey Task key to run
     * @param dryRun indicates if execution shall be simulated with no actual changes
     * @param context Quartz' execution context, can be used to pass parameters to the job
     * @throws JobExecutionException if anything goes wrong
     */
    void execute(String taskKey, boolean dryRun, JobExecutionContext context) throws JobExecutionException;
}
