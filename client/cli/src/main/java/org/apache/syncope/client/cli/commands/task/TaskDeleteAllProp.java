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

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDeleteAllProp extends AbstractTaskCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TaskDeleteAllProp.class);

    private static final String DELETE_HELP_MESSAGE = "task --delete-all-prop";

    private final Input input;

    public TaskDeleteAllProp(final Input input) {
        this.input = input;
    }

    public void delete() {

        if (input.parameterNumber() == 0) {
            for (final AbstractTaskTO taskTO : taskSyncopeOperations.listPropagationTask()) {
                final String taskId = String.valueOf(taskTO.getKey());
                try {
                    taskSyncopeOperations.delete(taskId);
                    taskResultManager.deletedMessage("Task", taskId);
                } catch (final WebServiceException | SyncopeClientException ex) {
                    LOG.error("Error deleting task", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        taskResultManager.notFoundError("Task", taskId);
                    } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                        taskResultManager.genericError("You cannot delete task " + taskId);
                    } else {
                        taskResultManager.genericError(ex.getMessage());
                    }
                } catch (final NumberFormatException ex) {
                    LOG.error("Error deleting task", ex);
                    taskResultManager.notBooleanDeletedError("task", taskId);
                }
            }
        } else {
            taskResultManager.commandOptionError(DELETE_HELP_MESSAGE);
        }
    }
}
