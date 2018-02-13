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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRead extends AbstractTaskCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRead.class);

    private static final String READ_HELP_MESSAGE = "task --read {TASK-TYPE} {TASK-KEY}\n"
            + "   Task type:  PROPAGATION / NOTIFICATION / SCHEDULED / PULL / PUSH";

    private final Input input;

    public TaskRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 2) {
            final String[] parameters = Arrays.copyOfRange(input.getParameters(), 1, input.parameterNumber());
            try {
                final List<TaskTO> taskTOs = new ArrayList<>();
                for (final String parameter : parameters) {
                    taskTOs.add(taskSyncopeOperations.read(input.firstParameter(), parameter));
                }
                taskResultManager.printTasks(taskTOs);
            } catch (final SyncopeClientException | WebServiceException ex) {
                LOG.error("Error reading task", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    taskResultManager.notFoundError("Task", parameters[0]);
                } else {
                    taskResultManager.genericError(ex.getMessage());
                }
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error reading task", ex);
                taskResultManager.typeNotValidError(
                        "task", input.firstParameter(), CommandUtils.fromEnumToArray(TaskType.class));
            }
        } else {
            taskResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
