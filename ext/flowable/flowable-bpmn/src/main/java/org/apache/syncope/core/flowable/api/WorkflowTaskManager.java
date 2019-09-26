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
package org.apache.syncope.core.flowable.api;

import java.util.List;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;

public interface WorkflowTaskManager {

    /**
     * Gets variable value.
     * Returns null when no variable value is found with the given name or when the value is set to null.
     *
     * @param <T> variable type
     * @param executionId id of execution, cannot be null.
     * @param variableName name of variable, cannot be null.
     * @param variableClass class of variable, cannot be null.
     * @return the variable value or null if the variable is undefined or the value of the variable is null.
     * @throws org.flowable.common.engine.api.FlowableObjectNotFoundException
     * when no execution is found for the given executionId.
     * @throws ClassCastException
     * when cannot cast variable to given class
     */
    <T> T getVariable(String executionId, String variableName, Class<T> variableClass);

    /**
     * Updates or create sa variable for an execution.
     *
     * @param executionId id of execution to set variable in, cannot be null.
     * @param variableName name of variable to set, cannot be null.
     * @param value value to set; when null is passed, the variable is not removed, only it's value will be set to null
     * @throws org.flowable.common.engine.api.FlowableObjectNotFoundException
     * when no execution is found for the given executionId.
     */
    void setVariable(String executionId, String variableName, Object value);

    /**
     * Get tasks available for execution, for given user.
     *
     * @param userKey user key
     * @return available tasks
     */
    List<WorkflowTask> getAvailableTasks(String userKey);

    /**
     * Execute a task on an user.
     *
     * @param workflowTaskExecInput input for task execution
     * @return user after task execution
     */
    UserWorkflowResult<String> executeNextTask(WorkflowTaskExecInput workflowTaskExecInput);
}
