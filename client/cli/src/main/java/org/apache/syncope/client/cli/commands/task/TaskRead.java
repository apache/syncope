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
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRead extends AbstractTaskCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRead.class);

    private static final String READ_HELP_MESSAGE = "task --read {TASK-KEY} {TASK-KEY} [...]";

    private final Input input;

    public TaskRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            final List<AbstractTaskTO> taskTOs = new ArrayList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    taskTOs.add(taskSyncopeOperations.read(parameter));
                } catch (final NumberFormatException ex) {
                    LOG.error("Error reading task", ex);
                    taskResultManager.notBooleanDeletedError("task", parameter);
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error reading task", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        taskResultManager.notFoundError("Task", parameter);
                    } else {
                        taskResultManager.genericError(ex.getMessage());
                    }
                    break;
                }
            }
            taskResultManager.printTasks(taskTOs);
        } else {
            taskResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
