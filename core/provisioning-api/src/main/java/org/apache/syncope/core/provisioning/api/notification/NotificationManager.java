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
package org.apache.syncope.core.provisioning.api.notification;

import java.util.List;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;

/**
 * Create notification tasks that will be executed by NotificationJob.
 *
 * @see org.apache.syncope.core.persistence.api.entity.task.NotificationTask
 */
public interface NotificationManager {

    /**
     * Count the number of task executions of a given task with a given status.
     *
     * @param taskKey task
     * @param status status
     * @return number of task executions
     */
    long countExecutionsWithStatus(String taskKey, String status);

    /**
     * Checks if notifications are available matching the provided conditions.
     *
     * @param domain domain
     * @param type event category type
     * @param category event category
     * @param subcategory event subcategory
     * @param op operation
     * @return created notification tasks
     */
    boolean notificationsAvailable(
            String domain,
            OpEvent.CategoryType type,
            String category,
            String subcategory,
            String op);

    /**
     * Create notification tasks according to the provided event.
     *
     * @param event Spring event raised during Logic processing
     */
    void createTasks(AfterHandlingEvent event);

    /**
     * Create notification tasks for each notification matching provided conditions.
     *
     * @param who user triggering the event
     * @param type event category type
     * @param category event category
     * @param subcategory event subcategory
     * @param op operation
     * @param outcome result value condition.
     * @param before object(s) availabile before the event
     * @param output object(s) produced by the event
     * @param input object(s) provided to the event
     * @return created notification tasks
     */
    @SuppressWarnings("squid:S00107")
    List<NotificationTask> createTasks(
            String who,
            OpEvent.CategoryType type,
            String category,
            String subcategory,
            String op,
            OpEvent.Outcome outcome,
            Object before,
            Object output,
            Object... input);

    long getMaxRetries();

    /**
     * Set execution state of NotificationTask with provided id.
     *
     * @param taskKey task to be updated
     * @param executed execution state
     */
    void setTaskExecuted(String taskKey, boolean executed);

    /**
     * Store execution of a NotificationTask.
     *
     * @param execution task execution.
     * @return merged task execution.
     */
    TaskExec<NotificationTask> storeExec(TaskExec<NotificationTask> execution);
}
