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
package org.apache.syncope.core.persistence.jpa.entity.task;

import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

@SuppressWarnings("unchecked")
public final class JPATaskUtils implements TaskUtils {

    protected final TaskType type;

    protected JPATaskUtils(final TaskType type) {
        this.type = type;
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public <T extends Task<T>> Class<T> taskClass() {
        Class<T> result = null;

        switch (type) {
            case PROPAGATION:
                result = (Class<T>) PropagationTask.class;
                break;

            case SCHEDULED:
                result = (Class<T>) SchedTask.class;
                break;

            case PULL:
                result = (Class<T>) PullTask.class;
                break;

            case PUSH:
                result = (Class<T>) PushTask.class;
                break;

            case MACRO:
                result = (Class<T>) MacroTask.class;
                break;

            case NOTIFICATION:
                result = (Class<T>) NotificationTask.class;
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends Task<T>> T newTask() {
        T result = null;

        switch (type) {
            case PROPAGATION:
                result = (T) new JPAPropagationTask();
                break;

            case SCHEDULED:
                result = (T) new JPASchedTask();
                break;

            case PULL:
                result = (T) new JPAPullTask();
                break;

            case PUSH:
                result = (T) new JPAPushTask();
                break;

            case MACRO:
                result = (T) new JPAMacroTask();
                break;

            case NOTIFICATION:
                result = (T) new JPANotificationTask();
                break;

            default:
        }

        if (result != null) {
            ((AbstractTask<?>) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        return result;
    }

    @Override
    public <E extends TaskExec<?>> E newTaskExec() {
        E result;

        switch (type) {
            case NOTIFICATION:
                result = (E) new JPANotificationTaskExec();
                break;

            case PROPAGATION:
                result = (E) new JPAPropagationTaskExec();
                break;

            case PULL:
                result = (E) new JPAPullTaskExec();
                break;

            case PUSH:
                result = (E) new JPAPushTaskExec();
                break;

            case MACRO:
                result = (E) new JPAMacroTaskExec();
                break;

            case SCHEDULED:
                result = (E) new JPASchedTaskExec();
                break;

            default:
                result = null;
        }

        if (result != null) {
            ((AbstractTaskExec<?>) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        return result;
    }

    @Override
    public <T extends TaskTO> Class<T> taskTOClass() {
        Class<T> result = null;

        switch (type) {
            case PROPAGATION:
                result = (Class<T>) PropagationTaskTO.class;
                break;

            case SCHEDULED:
                result = (Class<T>) SchedTaskTO.class;
                break;

            case PULL:
                result = (Class<T>) PullTaskTO.class;
                break;

            case PUSH:
                result = (Class<T>) PushTaskTO.class;
                break;

            case MACRO:
                result = (Class<T>) MacroTaskTO.class;
                break;

            case NOTIFICATION:
                result = (Class<T>) NotificationTaskTO.class;
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends TaskTO> T newTaskTO() {
        Class<T> taskClass = taskTOClass();
        try {
            return taskClass == null ? null : taskClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getTaskTable() {
        String result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.TABLE;
                break;

            case PROPAGATION:
                result = JPAPropagationTask.TABLE;
                break;

            case PUSH:
                result = JPAPushTask.TABLE;
                break;

            case PULL:
                result = JPAPullTask.TABLE;
                break;

            case MACRO:
                result = JPAMacroTask.TABLE;
                break;

            case SCHEDULED:
                result = JPASchedTask.TABLE;
                break;

            default:
        }

        return result;
    }

    @Override
    public Class<? extends Task<?>> getTaskEntity() {
        Class<? extends Task<?>> result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTask.class;
                break;

            case PROPAGATION:
                result = JPAPropagationTask.class;
                break;

            case PUSH:
                result = JPAPushTask.class;
                break;

            case PULL:
                result = JPAPullTask.class;
                break;

            case MACRO:
                result = JPAMacroTask.class;
                break;

            case SCHEDULED:
                result = JPASchedTask.class;
                break;

            default:
        }

        return result;
    }

    @Override
    public String getTaskExecTable() {
        String result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTaskExec.TABLE;
                break;

            case PROPAGATION:
                result = JPAPropagationTaskExec.TABLE;
                break;

            case SCHEDULED:
                result = JPASchedTaskExec.TABLE;
                break;

            case PUSH:
                result = JPAPushTaskExec.TABLE;
                break;

            case PULL:
                result = JPAPullTaskExec.TABLE;
                break;

            case MACRO:
                result = JPAMacroTaskExec.TABLE;
                break;

            default:
        }

        return result;
    }

    @Override
    public Class<? extends TaskExec<?>> getTaskExecEntity() {
        Class<? extends TaskExec<?>> result = null;

        switch (type) {
            case NOTIFICATION:
                result = JPANotificationTaskExec.class;
                break;

            case PROPAGATION:
                result = JPAPropagationTaskExec.class;
                break;

            case SCHEDULED:
                result = JPASchedTaskExec.class;
                break;

            case PUSH:
                result = JPAPushTaskExec.class;
                break;

            case PULL:
                result = JPAPullTaskExec.class;
                break;

            case MACRO:
                result = JPAMacroTaskExec.class;
                break;

            default:
        }

        return result;
    }
}
