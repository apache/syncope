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
package org.apache.syncope.core.util;

import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;

@SuppressWarnings("unchecked")
public final class TaskUtil {

    private final TaskType type;

    public static TaskUtil getInstance(final TaskType type) {
        return new TaskUtil(type);
    }

    public static TaskUtil getInstance(final Task task) {
        TaskType type;
        if (task instanceof SyncTask) {
            type = TaskType.SYNCHRONIZATION;
        } else if (task instanceof SchedTask) {
            type = TaskType.SCHEDULED;
        } else if (task instanceof PropagationTask) {
            type = TaskType.PROPAGATION;
        } else if (task instanceof NotificationTask) {
            type = TaskType.NOTIFICATION;
        } else {
            throw new IllegalArgumentException("Invalid task: " + task);
        }

        return getInstance(type);
    }

    public static TaskUtil getInstance(Class<? extends AbstractTaskTO> taskClass) {
        TaskType type;
        if (taskClass == PropagationTaskTO.class) {
            type = TaskType.PROPAGATION;
        } else if (taskClass == NotificationTaskTO.class) {
            type = TaskType.NOTIFICATION;
        } else if (taskClass == SchedTaskTO.class) {
            type = TaskType.SCHEDULED;
        } else if (taskClass == SyncTaskTO.class) {
            type = TaskType.SYNCHRONIZATION;
        } else {
            throw new IllegalArgumentException("Invalid TaskTO class: " + taskClass.getName());
        }

        return getInstance(type);
    }

    public static TaskUtil getInstance(final AbstractTaskTO taskTO) {
        return getInstance(taskTO.getClass());
    }

    private TaskUtil(final TaskType type) {
        this.type = type;
    }

    public TaskType getType() {
        return type;
    }

    public <T extends Task> Class<T> taskClass() {
        Class<T> result = null;

        switch (type) {
            case PROPAGATION:
                result = (Class<T>) PropagationTask.class;
                break;

            case SCHEDULED:
                result = (Class<T>) SchedTask.class;
                break;

            case SYNCHRONIZATION:
                result = (Class<T>) SyncTask.class;
                break;

            case NOTIFICATION:
                result = (Class<T>) NotificationTask.class;
                break;

            default:
        }

        return result;
    }

    public <T extends Task> T newTask() {
        T result = null;

        switch (type) {
            case PROPAGATION:
                result = (T) new PropagationTask();
                break;

            case SCHEDULED:
                result = (T) new SchedTask();
                break;

            case SYNCHRONIZATION:
                result = (T) new SyncTask();
                break;

            case NOTIFICATION:
                result = (T) new NotificationTask();
                break;

            default:
        }

        return result;
    }

    public <T extends AbstractTaskTO> Class<T> taskTOClass() {
        Class<T> result = null;

        switch (type) {
            case PROPAGATION:
                result = (Class<T>) PropagationTaskTO.class;
                break;

            case SCHEDULED:
                result = (Class<T>) SchedTaskTO.class;
                break;

            case SYNCHRONIZATION:
                result = (Class<T>) SyncTaskTO.class;
                break;

            case NOTIFICATION:
                result = (Class<T>) NotificationTaskTO.class;
                break;

            default:
        }

        return result;
    }

    public <T extends AbstractTaskTO> T newTaskTO() {
        T result = null;

        switch (type) {
            case PROPAGATION:
                result = (T) new PropagationTaskTO();
                break;

            case SCHEDULED:
                result = (T) new SchedTaskTO();
                break;

            case SYNCHRONIZATION:
                result = (T) new SyncTaskTO();
                break;

            case NOTIFICATION:
                result = (T) new NotificationTaskTO();
                break;

            default:
        }

        return result;
    }
}
