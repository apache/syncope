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

import org.apache.syncope.common.lib.types.TaskType;
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
    public <T extends Task<T>> T newTask() {
        T result = null;

        switch (type) {
            case PROPAGATION:
                result = (T) new JPAPropagationTask();
                break;

            case SCHEDULED:
                result = (T) new JPASchedTask();
                break;

            case LIVE_SYNC:
                result = (T) new JPALiveSyncTask();
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

            case LIVE_SYNC:
                result = (E) new JPALiveSyncTaskExec();
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
    public String getTaskStorage() {
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

            case LIVE_SYNC:
                result = JPALiveSyncTask.TABLE;
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

            case LIVE_SYNC:
                result = JPALiveSyncTask.class;
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
    public String getTaskExecStorage() {
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

            case LIVE_SYNC:
                result = JPALiveSyncTaskExec.TABLE;
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

            case LIVE_SYNC:
                result = JPALiveSyncTaskExec.class;
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
