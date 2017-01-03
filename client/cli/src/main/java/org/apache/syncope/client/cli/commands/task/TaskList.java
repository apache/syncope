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

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskList extends AbstractTaskCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TaskList.class);

    private static final String LIST_HELP_MESSAGE = "task --list {TASK-TYPE}\n"
            + "   Task type: NOTIFICATION / PROPAGATION / PUSH / SCHEDULED / PULL";

    private final Input input;

    public TaskList(final Input input) {
        this.input = input;
    }

    public void list() {
        if (input.parameterNumber() == 1) {
            try {
                taskResultManager.printTasksType(
                        input.firstParameter(), taskSyncopeOperations.list(input.firstParameter()));
            } catch (final SyncopeClientException ex) {
                LOG.error("Error listing task", ex);
                taskResultManager.genericError(ex.getMessage());
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error listing task", ex);
                taskResultManager.typeNotValidError(
                        "task", input.firstParameter(), CommandUtils.fromEnumToArray(TaskType.class));
            }
        } else {
            taskResultManager.commandOptionError(LIST_HELP_MESSAGE);
        }
    }
}
