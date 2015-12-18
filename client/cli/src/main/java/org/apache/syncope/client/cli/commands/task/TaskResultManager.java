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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.TaskType;

public class TaskResultManager extends CommonsResultManager {

    public void printTasks(final LinkedList<AbstractTaskTO> taskTOs) {
        System.out.println("");
        for (final AbstractTaskTO taskTO : taskTOs) {
            if (taskTO instanceof NotificationTaskTO) {
                printNotificationTask((NotificationTaskTO) taskTO);
            } else if (taskTO instanceof PropagationTaskTO) {
                printPropagationTask((PropagationTaskTO) taskTO);
            } else if (taskTO instanceof PushTaskTO) {
                printPushTask((PushTaskTO) taskTO);
            } else if (taskTO instanceof SchedTaskTO) {
                printScheduledTask((SchedTaskTO) taskTO);
            } else if (taskTO instanceof SyncTaskTO) {
                printSyncTask((SyncTaskTO) taskTO);
            }
        }
    }

    public void printTasksType(final String taskTypeString, final LinkedList<AbstractTaskTO> taskTOs) {
        System.out.println("");
        switch (TaskType.valueOf(taskTypeString)) {
            case NOTIFICATION:
                for (final AbstractTaskTO taskTO : taskTOs) {
                    printNotificationTask(((NotificationTaskTO) taskTO));
                }
                break;
            case PROPAGATION:
                for (final AbstractTaskTO taskTO : taskTOs) {
                    printPropagationTask((PropagationTaskTO) taskTO);
                }
                break;
            case PUSH:
                for (final AbstractTaskTO taskTO : taskTOs) {
                    printPushTask((PushTaskTO) taskTO);
                }
                break;
            case SCHEDULED:
                for (final AbstractTaskTO taskTO : taskTOs) {
                    printScheduledTask((SchedTaskTO) taskTO);
                }
                break;
            case SYNCHRONIZATION:
                for (final AbstractTaskTO taskTO : taskTOs) {
                    printSyncTask((SyncTaskTO) taskTO);
                }
                break;
            default:
                break;
        }
    }

    private void printNotificationTask(final NotificationTaskTO notificationTaskTO) {
        System.out.println(" - Notification task key: " + notificationTaskTO.getKey());
        System.out.println("     executed: " + notificationTaskTO.isExecuted());
        System.out.println("     sender: " + notificationTaskTO.getSender());
        System.out.println("     subjetc: " + notificationTaskTO.getSubject());
        System.out.println("     text body: " + notificationTaskTO.getTextBody());
        System.out.println("     html body: " + notificationTaskTO.getHtmlBody());
        System.out.println("     latest execution status: "
                + notificationTaskTO.getLatestExecStatus());
        System.out.println("     start date: " + notificationTaskTO.getStart());
        System.out.println("     end date: " + notificationTaskTO.getEnd());
        System.out.println("     recipients: " + notificationTaskTO.getRecipients());
        System.out.println("     trace level: " + notificationTaskTO.getTraceLevel());
        printTaskExecTO(notificationTaskTO.getExecutions());
        System.out.println("");
    }

    private void printPropagationTask(final PropagationTaskTO propagationTaskTO) {
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
        System.out.println("     attributes: " + propagationTaskTO.getAttributes());
        System.out.println("     start date: " + propagationTaskTO.getStart());
        System.out.println("     end date: " + propagationTaskTO.getEnd());
        System.out.println("     operation: " + propagationTaskTO.getOperation());
        printTaskExecTO(propagationTaskTO.getExecutions());
        System.out.println("");
    }

    private void printPushTask(final PushTaskTO pushTaskTO) {
        System.out.println(" - Push task key: " + pushTaskTO.getKey());
        System.out.println("     name: " + pushTaskTO.getName());
        System.out.println("     resource: " + pushTaskTO.getResource());
        System.out.println("     cron expression: " + pushTaskTO.getCronExpression());
        System.out.println("     description: " + pushTaskTO.getDescription());
        System.out.println("     is perform create: " + pushTaskTO.isPerformCreate());
        System.out.println("     is perform delete: " + pushTaskTO.isPerformDelete());
        System.out.println("     is perform update: " + pushTaskTO.isPerformUpdate());
        System.out.println("     is sync status: " + pushTaskTO.isSyncStatus());
        System.out.println("     start date: " + pushTaskTO.getStart());
        System.out.println("     end date: " + pushTaskTO.getEnd());
        System.out.println("     last execution: " + pushTaskTO.getLastExec());
        System.out.println("     next execution: " + pushTaskTO.getNextExec());
        System.out.println("     latest execution status: "
                + pushTaskTO.getLatestExecStatus());
        System.out.println("     filters: " + pushTaskTO.getFilters());
        System.out.println("     delegate class: " + pushTaskTO.getJobDelegateClassName());
        System.out.println("     action class: " + pushTaskTO.getActionsClassNames());
        System.out.println("     matching rule: " + pushTaskTO.getMatchingRule());
        System.out.println("     not matching rule: " + pushTaskTO.getUnmatchingRule());
        printTaskExecTO(pushTaskTO.getExecutions());
        System.out.println("");
    }

    private void printScheduledTask(final SchedTaskTO schedTaskTO) {
        System.out.println(" - Scheduled task key: " + schedTaskTO.getKey());
        System.out.println("     name: " + schedTaskTO.getName());
        System.out.println("     cron expression: " + schedTaskTO.getCronExpression());
        System.out.println("     description: " + schedTaskTO.getDescription());
        System.out.println("     start date: " + schedTaskTO.getStart());
        System.out.println("     end date: " + schedTaskTO.getEnd());
        System.out.println("     last execution: " + schedTaskTO.getLastExec());
        System.out.println("     next execution: " + schedTaskTO.getNextExec());
        System.out.println("     latest execution status: "
                + schedTaskTO.getLatestExecStatus());
        System.out.println("     job delegate class: "
                + schedTaskTO.getJobDelegateClassName());
        printTaskExecTO(schedTaskTO.getExecutions());
        System.out.println("");
    }

    private void printSyncTask(final SyncTaskTO syncTaskTO) {
        System.out.println(" - Sync task key: " + syncTaskTO.getKey());
        System.out.println("     name: " + syncTaskTO.getName());
        System.out.println("     resource: " + syncTaskTO.getResource());
        System.out.println("     realm destination: " + syncTaskTO.getDestinationRealm());
        System.out.println("     cron expression: " + syncTaskTO.getCronExpression());
        System.out.println("     description: " + syncTaskTO.getDescription());
        System.out.println("     sync mode: " + syncTaskTO.getSyncMode());
        System.out.println("     perform create: " + syncTaskTO.isPerformCreate());
        System.out.println("     perform delete: " + syncTaskTO.isPerformDelete());
        System.out.println("     perform update: " + syncTaskTO.isPerformUpdate());
        System.out.println("     sync status: " + syncTaskTO.isSyncStatus());
        System.out.println("     TEMPLATES:");
        printTemplates(syncTaskTO.getTemplates());
        System.out.println("     start date: " + syncTaskTO.getStart());
        System.out.println("     end date: " + syncTaskTO.getEnd());
        System.out.println("     next execution: " + syncTaskTO.getNextExec());
        System.out.println("     last execution: " + syncTaskTO.getLastExec());
        System.out.println("     latest execution status: "
                + syncTaskTO.getLatestExecStatus());
        System.out.println("     job delegate class: "
                + syncTaskTO.getJobDelegateClassName());
        System.out.println("     action class name: " + syncTaskTO.getActionsClassNames());
        System.out.println("     matching rule: " + syncTaskTO.getMatchingRule());
        System.out.println("     unmatching rule: " + syncTaskTO.getUnmatchingRule());
        printTaskExecTO(syncTaskTO.getExecutions());
        System.out.println("");
    }

    private void printTemplates(final Map<String, AnyTO> templates) {
        for (final Map.Entry<String, AnyTO> entrySet : templates.entrySet()) {
            final String key = entrySet.getKey();
            final AnyTO value = entrySet.getValue();
            System.out.println("        " + key + " key: " + value.getKey()
                    + " of realm" + value.getRealm()
                    + " on resource " + value.getResources());

        }
    }

    public void printTaskExecTO(final List<TaskExecTO> taskExecTOs) {
        for (final TaskExecTO taskExecTO : taskExecTOs) {
            System.out.println("     EXECUTIONS: ");
            System.out.println("     - task execution key: " + taskExecTO.getKey());
            System.out.println("       task: " + taskExecTO.getTask());
            System.out.println("       message: ");
            System.out.println("       ###############     <BEGIN MESSAGE>     ###############");
            System.out.println("       message: " + taskExecTO.getMessage());
            System.out.println("       ###############     <END   MESSAGE>     ###############");
            System.out.println("       status: " + taskExecTO.getStatus());
            System.out.println("       start date: " + taskExecTO.getStart());
            System.out.println("       end date: " + taskExecTO.getEnd());
            System.out.println("");
        }
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("tasks details", details);
    }
}
