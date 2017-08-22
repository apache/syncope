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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDetails extends AbstractTaskCommand {

    private static final Logger LOG = LoggerFactory.getLogger(TaskDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "task --details";

    private final Input input;

    public TaskDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedHashMap<>();
                final List<AbstractTaskTO> notificationTaskTOs =
                        taskSyncopeOperations.list(TaskType.NOTIFICATION.name());
                final List<AbstractTaskTO> propagationTaskTOs = taskSyncopeOperations.list(TaskType.PROPAGATION.name());
                final List<AbstractTaskTO> pushTaskTOs = taskSyncopeOperations.list(TaskType.PUSH.name());
                final List<AbstractTaskTO> scheduledTaskTOs = taskSyncopeOperations.list(TaskType.SCHEDULED.name());
                final List<AbstractTaskTO> pullTaskTOs = taskSyncopeOperations.list(TaskType.PULL.name());
                final List<JobTO> jobTOs = taskSyncopeOperations.listJobs();
                final int notificationTaskSize = notificationTaskTOs.size();
                final int propagationTaskSize = propagationTaskTOs.size();
                final int pushTaskSize = pushTaskTOs.size();
                final int scheduledTaskSize = scheduledTaskTOs.size();
                int scheduledNotExecuted = 0;
                final int pullTaskSize = pullTaskTOs.size();
                final int jobsSize = jobTOs.size();

                int notificationNotExecuted = 0;
                for (final AbstractTaskTO notificationTaskTO : notificationTaskTOs) {
                    if (!((NotificationTaskTO) notificationTaskTO).isExecuted()) {
                        notificationNotExecuted++;
                    }
                }

                int propagationNotExecuted = 0;
                for (final AbstractTaskTO propagationTaskTO : propagationTaskTOs) {
                    if (((PropagationTaskTO) propagationTaskTO).getExecutions() == null
                            || ((PropagationTaskTO) propagationTaskTO).getExecutions().isEmpty()) {
                        propagationNotExecuted++;
                    }
                }

                int pushNotExecuted = 0;
                for (final AbstractTaskTO pushTaskTO : pushTaskTOs) {
                    if (((PushTaskTO) pushTaskTO).getExecutions() == null
                            || ((PushTaskTO) pushTaskTO).getExecutions().isEmpty()) {
                        pushNotExecuted++;
                    }
                }

                for (final AbstractTaskTO scheduledTaskTO : scheduledTaskTOs) {
                    if (((SchedTaskTO) scheduledTaskTO).getExecutions() == null
                            || ((SchedTaskTO) scheduledTaskTO).getExecutions().isEmpty()) {
                        scheduledNotExecuted++;
                    }
                }

                int pullNotExecuted = 0;
                int pullFull = 0;
                for (final AbstractTaskTO pullTaskTO : pullTaskTOs) {
                    if (((PullTaskTO) pullTaskTO).getExecutions() == null
                            || ((PullTaskTO) pullTaskTO).getExecutions().isEmpty()) {
                        pullNotExecuted++;
                    }
                    if (((PullTaskTO) pullTaskTO).getPullMode() == PullMode.FULL_RECONCILIATION) {
                        pullFull++;
                    }
                }

                details.put("total number", String.valueOf(notificationTaskSize
                        + propagationTaskSize
                        + pushTaskSize
                        + scheduledTaskSize
                        + pullTaskSize));
                details.put("notification tasks", String.valueOf(notificationTaskSize));
                details.put("notification tasks not executed", String.valueOf(notificationNotExecuted));
                details.put("propagation tasks", String.valueOf(propagationTaskSize));
                details.put("propagation tasks not executed", String.valueOf(propagationNotExecuted));
                details.put("push tasks", String.valueOf(pushTaskSize));
                details.put("push tasks not executed", String.valueOf(pushNotExecuted));
                details.put("scheduled tasks", String.valueOf(scheduledTaskSize));
                details.put("scheduled tasks not executed", String.valueOf(scheduledNotExecuted));
                details.put("pull tasks", String.valueOf(pullTaskSize));
                details.put("pull tasks not executed", String.valueOf(pullNotExecuted));
                details.put("pull tasks with full reconciliation", String.valueOf(pullFull));
                details.put("jobs", String.valueOf(jobsSize));
                taskResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about task", ex);
                taskResultManager.genericError(ex.getMessage());
            } catch (final IllegalArgumentException ex) {
                LOG.error("Error reading details about task", ex);
                taskResultManager.typeNotValidError(
                        "task", input.firstParameter(), CommandUtils.fromEnumToArray(TaskType.class));
            }
        } else {
            taskResultManager.commandOptionError(DETAILS_HELP_MESSAGE);
        }
    }
}
