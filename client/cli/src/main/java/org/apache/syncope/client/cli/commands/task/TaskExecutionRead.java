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
package org.apache.syncope.client.cli.commands.task;

import java.util.Arrays;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;

public class TaskExecutionRead extends AbstractTaskCommand {

    private static final String EXECUTION_READ_HELP_MESSAGE = "task --read-execution {TASK-ID} {TASK-ID} [...]";

    private final Input input;

    public TaskExecutionRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    taskResultManager.printTaskExecTO(
                            Arrays.asList(taskSyncopeOperations.readExecution(parameter)));
                } catch (final NumberFormatException ex) {
                    taskResultManager.notBooleanDeletedError("task execution", parameter);
                } catch (final SyncopeClientException | WebServiceException ex) {
                    if (ex.getMessage().startsWith("NotFound")) {
                        taskResultManager.notFoundError("Task execution", parameter);
                    } else {
                        taskResultManager.genericError(ex.getMessage());
                    }
                    break;
                }
            }
        } else {
            taskResultManager.commandOptionError(EXECUTION_READ_HELP_MESSAGE);
        }
    }
}
