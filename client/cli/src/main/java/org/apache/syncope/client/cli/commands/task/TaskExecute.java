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

public class TaskExecute extends AbstractTaskCommand {

    private static final String EXECUTE_HELP_MESSAGE = "task --execute {TASK-ID} {DRY-RUN}\n"
            + "          Dry run: true / false";

    private final Input input;

    public TaskExecute(final Input input) {
        this.input = input;
    }

    public void execute() {
        if (input.parameterNumber() == 2) {
            try {
                final Long taskIdToExecute = Long.valueOf(input.firstParameter());
                boolean dryRun = true;
                if ("false".equalsIgnoreCase(input.secondParameter())) {
                    dryRun = false;
                } else if ("true".equalsIgnoreCase(input.secondParameter())) {
                    dryRun = true;
                } else {
                    taskResultManager.notBooleanDeletedError("dry run", input.secondParameter());
                }
                taskResultManager.printTaskExecTO(Arrays.asList(taskService.execute(taskIdToExecute, dryRun)));
            } catch (final WebServiceException | SyncopeClientException ex) {
                if (ex.getMessage().startsWith("NotFound")) {
                    taskResultManager.notFoundError("Task", input.firstParameter());
                } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                    taskResultManager.generic("You cannot delete task " + input.firstParameter());
                } else {
                    taskResultManager.generic(ex.getMessage());
                }
            } catch (final NumberFormatException ex) {
                taskResultManager.notBooleanDeletedError("task", input.firstParameter());
            }
        } else {
            taskResultManager.commandOptionError(EXECUTE_HELP_MESSAGE);
        }
    }

}
