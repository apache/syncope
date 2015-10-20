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
package org.apache.syncope.client.cli.commands;

import org.apache.syncope.client.cli.commands.logger.LoggerCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "task")
public class TaskCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerCommand.class);

    private static final String HELP_MESSAGE = "Usage: task [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list-task \n"
            + "       Syntax: --list-task {TASK-TYPE} \n"
            + "          Task type: NOTIFICATION / PROPAGATION / PUSH / SCHEDULED / SYNCHRONIZATION\n"
            + "    --list-running-jobs \n"
            + "    --list-scheduled-jobs \n"
            + "    --read \n"
            + "       Syntax: --read {TASK-ID} {TASK-ID} [...]\n"
            + "    --read-execution \n"
            + "       Syntax: --read-execution {TASK-EXEC-ID} {TASK-EXEC-ID} [...]\n"
            + "    --delete \n"
            + "       Syntax: --delete {TASK-ID} {TASK-ID} [...]\n"
            + "    --delete-execution \n"
            + "       Syntax: --delete-execution {TASK-EXEC-ID} {TASK-EXEC-ID} [...]\n"
            + "    --execute \n"
            + "       Syntax: --execute {TASK-ID} {DRY-RUN}"
            + "          Dry run: true / false";

    @Override
    public void execute(final Input input) {
        LOG.debug("Option: {}", input.getOption());
        LOG.debug("Parameters:");
        for (final String parameter : input.getParameters()) {
            LOG.debug("   > " + parameter);
        }

        final String[] parameters = input.getParameters();

        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        final TaskService taskService = SyncopeServices.get(TaskService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST_TASK:
                final String listTaskErrorMessage = "task --list-task {TASK-TYPE}\n"
                        + "   Task type: NOTIFICATION / PROPAGATION / PUSH / SCHEDULED / SYNCHRONIZATION";
                if (parameters.length == 1) {
                    try {
                        final TaskType taskType = TaskType.valueOf(parameters[0]);
                        for (final AbstractTaskTO taskTO : taskService.list(taskType, new TaskQuery()).getResult()) {
                            switch (taskType) {
                                case NOTIFICATION:
                                    final NotificationTaskTO notificationTaskTO = (NotificationTaskTO) taskTO;
                                    System.out.println("");
                                    System.out.println(" - Notification task key: " + notificationTaskTO.getKey());
                                    System.out.println("     executed: " + notificationTaskTO.isExecuted());
                                    System.out.println("     sender: " + notificationTaskTO.getSender());
                                    System.out.println("     subjetc: " + notificationTaskTO.getSubject());
                                    System.out.println("     text body: " + notificationTaskTO.getTextBody());
                                    System.out.println("     html body: " + notificationTaskTO.getHtmlBody());
                                    System.out.println("     latest execution status: "
                                            + notificationTaskTO.getLatestExecStatus());
                                    System.out.println("     start date: " + notificationTaskTO.getStartDate());
                                    System.out.println("     end date: " + notificationTaskTO.getEndDate());
                                    System.out.println("     recipients: " + notificationTaskTO.getRecipients());
                                    System.out.println("     trace level: " + notificationTaskTO.getTraceLevel());
                                    for (final TaskExecTO taskExecTO : notificationTaskTO.getExecutions()) {
                                        printTaskExecTO(taskExecTO);
                                    }
                                    System.out.println("");
                                    break;
                                case PROPAGATION:
                                    final PropagationTaskTO propagationTaskTO = (PropagationTaskTO) taskTO;
                                    System.out.println("");
                                    System.out.println(" - Propagation task key: " + propagationTaskTO.getKey());
                                    System.out.println("     resource: " + propagationTaskTO.getResource());
                                    System.out.println("     any key: " + propagationTaskTO.getAnyKey());
                                    System.out.println("     any type kind: " + propagationTaskTO.getAnyTypeKind());
                                    System.out.println("     connector object key: "
                                            + propagationTaskTO.getConnObjectKey());
                                    System.out.println("     old connector object key: "
                                            + propagationTaskTO.getOldConnObjectKey());
                                    System.out.println("     latest execution status: "
                                            + propagationTaskTO.getLatestExecStatus());
                                    System.out.println("     class name: " + propagationTaskTO.getObjectClassName());
                                    System.out.println("     xml attribute: " + propagationTaskTO.getXmlAttributes());
                                    System.out.println("     start date: " + propagationTaskTO.getStartDate());
                                    System.out.println("     end date: " + propagationTaskTO.getEndDate());
                                    System.out.println("     operation: " + propagationTaskTO.getOperation());
                                    for (final TaskExecTO taskExecTO : propagationTaskTO.getExecutions()) {
                                        printTaskExecTO(taskExecTO);
                                    }
                                    System.out.println("");
                                    break;
                                case PUSH:
                                    final PushTaskTO pushTaskTO = (PushTaskTO) taskTO;
                                    System.out.println("");
                                    System.out.println(" - Push task key: " + pushTaskTO.getKey());
                                    System.out.println("     name: " + pushTaskTO.getName());
                                    System.out.println("     resource: " + pushTaskTO.getResource());
                                    System.out.println("     cron expression: " + pushTaskTO.getCronExpression());
                                    System.out.println("     description: " + pushTaskTO.getDescription());
                                    System.out.println("     is perform create: " + pushTaskTO.isPerformCreate());
                                    System.out.println("     is perform delete: " + pushTaskTO.isPerformDelete());
                                    System.out.println("     is perform update: " + pushTaskTO.isPerformUpdate());
                                    System.out.println("     is sync status: " + pushTaskTO.isSyncStatus());
                                    System.out.println("     start date: " + pushTaskTO.getStartDate());
                                    System.out.println("     end date: " + pushTaskTO.getEndDate());
                                    System.out.println("     last execution: " + pushTaskTO.getLastExec());
                                    System.out.println("     next execution: " + pushTaskTO.getNextExec());
                                    System.out.println("     latest execution status: "
                                            + pushTaskTO.getLatestExecStatus());
                                    System.out.println("     filters: " + pushTaskTO.getFilters());
                                    System.out.println("     delegate class: " + pushTaskTO.getJobDelegateClassName());
                                    System.out.println("     action class: " + pushTaskTO.getActionsClassNames());
                                    System.out.println("     matching rule: " + pushTaskTO.getMatchingRule());
                                    System.out.println("     not matching rule: " + pushTaskTO.getUnmatchingRule());
                                    for (final TaskExecTO taskExecTO : pushTaskTO.getExecutions()) {
                                        printTaskExecTO(taskExecTO);
                                    }
                                    System.out.println("");
                                    break;
                                case SCHEDULED:
                                    final SchedTaskTO schedTaskTO = (SchedTaskTO) taskTO;
                                    System.out.println("");
                                    System.out.println(" - Scheduled task key: " + schedTaskTO.getKey());
                                    System.out.println("     name: " + schedTaskTO.getName());
                                    System.out.println("     cron expression: " + schedTaskTO.getCronExpression());
                                    System.out.println("     description: " + schedTaskTO.getDescription());
                                    System.out.println("     start date: " + schedTaskTO.getStartDate());
                                    System.out.println("     end date: " + schedTaskTO.getEndDate());
                                    System.out.println("     last execution: " + schedTaskTO.getLastExec());
                                    System.out.println("     next execution: " + schedTaskTO.getNextExec());
                                    System.out.println("     latest execution status: "
                                            + schedTaskTO.getLatestExecStatus());
                                    System.out.println("     job delegate class: "
                                            + schedTaskTO.getJobDelegateClassName());
                                    for (final TaskExecTO taskExecTO : schedTaskTO.getExecutions()) {
                                        printTaskExecTO(taskExecTO);
                                    }
                                    System.out.println("");
                                    break;
                                case SYNCHRONIZATION:
                                    final SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;
                                    System.out.println("");
                                    System.out.println(" - Sync task key: " + syncTaskTO.getKey());
                                    System.out.println("     name: " + syncTaskTO.getName());
                                    System.out.println("     resource: " + syncTaskTO.getResource());
                                    System.out.println("     realm destination: " + syncTaskTO.getDestinationRealm());
                                    System.out.println("     cron expression: " + syncTaskTO.getCronExpression());
                                    System.out.println("     description: " + syncTaskTO.getDescription());
                                    System.out.println("     is full reconciliation: "
                                            + syncTaskTO.isFullReconciliation());
                                    System.out.println("     is perform create: " + syncTaskTO.isPerformCreate());
                                    System.out.println("     is perform delete: " + syncTaskTO.isPerformDelete());
                                    System.out.println("     is perform update: " + syncTaskTO.isPerformUpdate());
                                    System.out.println("     is sync status: " + syncTaskTO.isSyncStatus());
                                    System.out.println("     templates:");
                                    for (Map.Entry<String, AnyTO> entrySet : syncTaskTO.getTemplates().entrySet()) {
                                        final String key = entrySet.getKey();
                                        final AnyTO value = entrySet.getValue();
                                        System.out.println("        " + key + " key: " + value.getKey()
                                                + " of realm" + value.getRealm()
                                                + " on resource " + value.getResources());

                                    }
                                    System.out.println("     start date: " + syncTaskTO.getStartDate());
                                    System.out.println("     end date: " + syncTaskTO.getEndDate());
                                    System.out.println("     next execution: " + syncTaskTO.getNextExec());
                                    System.out.println("     last execution: " + syncTaskTO.getLastExec());
                                    System.out.println("     latest execution status: "
                                            + syncTaskTO.getLatestExecStatus());
                                    System.out.println("     job delegate class: "
                                            + syncTaskTO.getJobDelegateClassName());
                                    System.out.println("     action class name: " + syncTaskTO.getActionsClassNames());
                                    System.out.println("     matching rule: " + syncTaskTO.getMatchingRule());
                                    System.out.println("     unmatching rule: " + syncTaskTO.getUnmatchingRule());
                                    for (final TaskExecTO taskExecTO : syncTaskTO.getExecutions()) {
                                        printTaskExecTO(taskExecTO);
                                    }
                                    System.out.println("");
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (final SyncopeClientException ex) {
                        Messages.printMessage(ex.getMessage());
                    } catch (final IllegalArgumentException ex) {
                        Messages.printTypeNotValidMessage(
                                "task", parameters[0], CommandUtils.fromEnumToArray(TaskType.class));
                    }
                } else {
                    Messages.printCommandOptionMessage(listTaskErrorMessage);
                }
                break;
            case LIST_RUNNING_JOBS:
                try {
                    for (final TaskExecTO taskExecTO : taskService.listJobs(JobStatusType.RUNNING)) {
                        printTaskExecTO(taskExecTO);
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage(ex.getMessage());
                }
                break;
            case LIST_SCHEDULED_JOBS:
                try {
                    for (final TaskExecTO taskExecTO : taskService.listJobs(JobStatusType.SCHEDULED)) {
                        printTaskExecTO(taskExecTO);
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage(ex.getMessage());
                }
                break;
            case READ:
                final String readErrorMessage = "task --read {TASK-ID} {TASK-ID} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            final AbstractTaskTO taskTO = taskService.read(Long.valueOf(parameter));
                            if (taskTO instanceof NotificationTaskTO) {
                                final NotificationTaskTO notificationTaskTO = (NotificationTaskTO) taskTO;
                                System.out.println("");
                                System.out.println(" - Notification task key: " + notificationTaskTO.getKey());
                                System.out.println("     executed: " + notificationTaskTO.isExecuted());
                                System.out.println("     sender: " + notificationTaskTO.getSender());
                                System.out.println("     subjetc: " + notificationTaskTO.getSubject());
                                System.out.println("     text body: " + notificationTaskTO.getTextBody());
                                System.out.println("     html body: " + notificationTaskTO.getHtmlBody());
                                System.out.println("     latest execution status: "
                                        + notificationTaskTO.getLatestExecStatus());
                                System.out.println("     start date: " + notificationTaskTO.getStartDate());
                                System.out.println("     end date: " + notificationTaskTO.getEndDate());
                                System.out.println("     recipients: " + notificationTaskTO.getRecipients());
                                System.out.println("     trace level: " + notificationTaskTO.getTraceLevel());
                                for (final TaskExecTO taskExecTO : notificationTaskTO.getExecutions()) {
                                    printTaskExecTO(taskExecTO);
                                }
                                System.out.println("");
                            } else if (taskTO instanceof PropagationTaskTO) {
                                final PropagationTaskTO propagationTaskTO = (PropagationTaskTO) taskTO;
                                System.out.println("");
                                System.out.println(" - Propagation task key: " + propagationTaskTO.getKey());
                                System.out.println("     resource: " + propagationTaskTO.getResource());
                                System.out.println("     any key: " + propagationTaskTO.getAnyKey());
                                System.out.println("     any type kind: " + propagationTaskTO.getAnyTypeKind());
                                System.out.println("     connector object key: "
                                        + propagationTaskTO.getConnObjectKey());
                                System.out.println("     old connector object key: "
                                        + propagationTaskTO.getOldConnObjectKey());
                                System.out.println("     latest execution status: "
                                        + propagationTaskTO.getLatestExecStatus());
                                System.out.println("     class name: " + propagationTaskTO.getObjectClassName());
                                System.out.println("     xml attribute: " + propagationTaskTO.getXmlAttributes());
                                System.out.println("     start date: " + propagationTaskTO.getStartDate());
                                System.out.println("     end date: " + propagationTaskTO.getEndDate());
                                System.out.println("     operation: " + propagationTaskTO.getOperation());
                                for (final TaskExecTO taskExecTO : propagationTaskTO.getExecutions()) {
                                    printTaskExecTO(taskExecTO);
                                }
                                System.out.println("");
                            } else if (taskTO instanceof PushTaskTO) {
                                final PushTaskTO pushTaskTO = (PushTaskTO) taskTO;
                                System.out.println("");
                                System.out.println(" - Push task key: " + pushTaskTO.getKey());
                                System.out.println("     name: " + pushTaskTO.getName());
                                System.out.println("     resource: " + pushTaskTO.getResource());
                                System.out.println("     cron expression: " + pushTaskTO.getCronExpression());
                                System.out.println("     description: " + pushTaskTO.getDescription());
                                System.out.println("     is perform create: " + pushTaskTO.isPerformCreate());
                                System.out.println("     is perform delete: " + pushTaskTO.isPerformDelete());
                                System.out.println("     is perform update: " + pushTaskTO.isPerformUpdate());
                                System.out.println("     is sync status: " + pushTaskTO.isSyncStatus());
                                System.out.println("     start date: " + pushTaskTO.getStartDate());
                                System.out.println("     end date: " + pushTaskTO.getEndDate());
                                System.out.println("     last execution: " + pushTaskTO.getLastExec());
                                System.out.println("     next execution: " + pushTaskTO.getNextExec());
                                System.out.println("     latest execution status: "
                                        + pushTaskTO.getLatestExecStatus());
                                System.out.println("     filters: " + pushTaskTO.getFilters());
                                System.out.println("     delegate class: " + pushTaskTO.getJobDelegateClassName());
                                System.out.println("     action class: " + pushTaskTO.getActionsClassNames());
                                System.out.println("     matching rule: " + pushTaskTO.getMatchingRule());
                                System.out.println("     not matching rule: " + pushTaskTO.getUnmatchingRule());
                                for (final TaskExecTO taskExecTO : pushTaskTO.getExecutions()) {
                                    printTaskExecTO(taskExecTO);
                                }
                                System.out.println("");
                            } else if (taskTO instanceof SchedTaskTO) {
                                final SchedTaskTO schedTaskTO = (SchedTaskTO) taskTO;
                                System.out.println("");
                                System.out.println(" - Scheduled task key: " + schedTaskTO.getKey());
                                System.out.println("     name: " + schedTaskTO.getName());
                                System.out.println("     cron expression: " + schedTaskTO.getCronExpression());
                                System.out.println("     description: " + schedTaskTO.getDescription());
                                System.out.println("     start date: " + schedTaskTO.getStartDate());
                                System.out.println("     end date: " + schedTaskTO.getEndDate());
                                System.out.println("     last execution: " + schedTaskTO.getLastExec());
                                System.out.println("     next execution: " + schedTaskTO.getNextExec());
                                System.out.println("     latest execution status: "
                                        + schedTaskTO.getLatestExecStatus());
                                System.out.println("     job delegate class: "
                                        + schedTaskTO.getJobDelegateClassName());
                                for (final TaskExecTO taskExecTO : schedTaskTO.getExecutions()) {
                                    printTaskExecTO(taskExecTO);
                                }
                                System.out.println("");
                            } else if (taskTO instanceof SyncTaskTO) {
                                final SyncTaskTO syncTaskTO = (SyncTaskTO) taskTO;
                                System.out.println("");
                                System.out.println(" - Sync task key: " + syncTaskTO.getKey());
                                System.out.println("     name: " + syncTaskTO.getName());
                                System.out.println("     resource: " + syncTaskTO.getResource());
                                System.out.println("     realm destination: " + syncTaskTO.getDestinationRealm());
                                System.out.println("     cron expression: " + syncTaskTO.getCronExpression());
                                System.out.println("     description: " + syncTaskTO.getDescription());
                                System.out.println("     is full reconciliation: "
                                        + syncTaskTO.isFullReconciliation());
                                System.out.println("     is perform create: " + syncTaskTO.isPerformCreate());
                                System.out.println("     is perform delete: " + syncTaskTO.isPerformDelete());
                                System.out.println("     is perform update: " + syncTaskTO.isPerformUpdate());
                                System.out.println("     is sync status: " + syncTaskTO.isSyncStatus());
                                System.out.println("     templates:");
                                for (Map.Entry<String, AnyTO> entrySet : syncTaskTO.getTemplates().entrySet()) {
                                    final String key = entrySet.getKey();
                                    final AnyTO value = entrySet.getValue();
                                    System.out.println("        " + key + "key : " + value.getKey()
                                            + " of realm" + value.getRealm()
                                            + " on resource " + value.getResources());

                                }
                                System.out.println("     start date: " + syncTaskTO.getStartDate());
                                System.out.println("     end date: " + syncTaskTO.getEndDate());
                                System.out.println("     next execution: " + syncTaskTO.getNextExec());
                                System.out.println("     last execution: " + syncTaskTO.getLastExec());
                                System.out.println("     latest execution status: "
                                        + syncTaskTO.getLatestExecStatus());
                                System.out.println("     job delegate class: "
                                        + syncTaskTO.getJobDelegateClassName());
                                System.out.println("     action class name: " + syncTaskTO.getActionsClassNames());
                                System.out.println("     matching rule: " + syncTaskTO.getMatchingRule());
                                System.out.println("     unmatching rule: " + syncTaskTO.getUnmatchingRule());
                                for (final TaskExecTO taskExecTO : syncTaskTO.getExecutions()) {
                                    printTaskExecTO(taskExecTO);
                                }
                                System.out.println("");
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("task", parameter);
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Task", parameter);
                            } else {
                                Messages.printMessage("Error: " + ex.getMessage());
                            }
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readErrorMessage);
                }
                break;
            case READ_EXECUTION:
                final String readExecutionErrorMessage = "task --read-execution {TASK-ID} {TASK-ID} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            final TaskExecTO taskExecTO = taskService.readExecution(Long.valueOf(parameter));
                            printTaskExecTO(taskExecTO);
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("task execution", parameter);
                        } catch (final SyncopeClientException | WebServiceException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Task execution", parameter);
                            } else {
                                Messages.printMessage("Error: " + ex.getMessage());
                            }
                            break;
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readExecutionErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = "task --delete {TASK-ID} {TASK-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            taskService.delete(Long.valueOf(parameter));
                            Messages.printDeletedMessage("Task", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Task", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                Messages.printMessage("You cannot delete task " + parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("task", parameter);
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteErrorMessage);
                }
                break;
            case DELETE_EXECUTION:
                final String deleteExecutionErrorMessage = "task --delete-execution "
                        + "{TASK-EXEC-ID} {TASK-EXEC-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            taskService.deleteExecution(Long.valueOf(parameter));
                            Messages.printDeletedMessage("Task execution", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Task execution", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                Messages.printMessage("You cannot delete task execution" + parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("task execution", parameter);
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteExecutionErrorMessage);
                }
                break;
            case EXECUTE:
                final String executeErrorMessage = "task --execute {TASK-ID} {DRY-RUN}\n"
                        + "          Dry run: true / false";

                if (parameters.length == 2) {

                    try {
                        final Long taskIdToExecute = Long.valueOf(parameters[0]);
                        boolean dryRun;
                        if ("false".equalsIgnoreCase(parameters[1])) {
                            dryRun = false;
                        } else if ("true".equalsIgnoreCase(parameters[1])) {
                            dryRun = true;
                        } else {
                            Messages.printNotBooleanDeletedMessage("dry run", parameters[1]);
                            break;
                        }
                        final TaskExecTO taskExecTO = taskService.execute(taskIdToExecute, dryRun);
                        printTaskExecTO(taskExecTO);
                    } catch (final WebServiceException | SyncopeClientException ex) {
                        if (ex.getMessage().startsWith("NotFound")) {
                            Messages.printNofFoundMessage("Task", parameters[0]);
                        } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                            Messages.printMessage("You cannot delete task " + parameters[0]);
                        } else {
                            Messages.printMessage(ex.getMessage());
                        }
                    } catch (final NumberFormatException ex) {
                        Messages.printIdNotNumberDeletedMessage("task", parameters[0]);
                    }
                } else {
                    Messages.printCommandOptionMessage(executeErrorMessage);
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                Messages.printDefaultMessage(input.getOption(), HELP_MESSAGE);
        }

    }

    private void printTaskExecTO(final TaskExecTO taskExecTO) {
        System.out.println("     EXECUTIONS: ");
        System.out.println("     - task execution key: " + taskExecTO.getKey());
        System.out.println("       task: " + taskExecTO.getTask());
        System.out.println("       message: ");
        System.out.println("       ###############     <BEGIN MESSAGE>     ###############");
        System.out.println("       message: " + taskExecTO.getMessage());
        System.out.println("       ###############     <END   MESSAGE>     ###############");
        System.out.println("       status: " + taskExecTO.getStatus());
        System.out.println("       start date: " + taskExecTO.getStartDate());
        System.out.println("       end date: " + taskExecTO.getEndDate());
        System.out.println("");
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
        LIST_TASK("--list-task"),
        LIST_RUNNING_JOBS("--list-running-jobs"),
        LIST_SCHEDULED_JOBS("--list-scheduled-jobs"),
        READ("--read"),
        READ_EXECUTION("--read-execution"),
        DELETE("--delete"),
        DELETE_EXECUTION("--delete-execution"),
        EXECUTE("--execute");

        private final String optionName;

        Options(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static Options fromName(final String name) {
            Options optionToReturn = HELP;
            for (final Options option : Options.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}
