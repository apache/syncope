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
package org.apache.syncope.core.persistence.neo4j.entity.task;

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
public final class Neo4jTaskUtils implements TaskUtils {

    protected final TaskType type;

    protected Neo4jTaskUtils(final TaskType type) {
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
                result = (T) new Neo4jPropagationTask();
                break;

            case SCHEDULED:
                result = (T) new Neo4jSchedTask();
                break;

            case PULL:
                result = (T) new Neo4jPullTask();
                break;

            case PUSH:
                result = (T) new Neo4jPushTask();
                break;

            case MACRO:
                result = (T) new Neo4jMacroTask();
                break;

            case NOTIFICATION:
                result = (T) new Neo4jNotificationTask();
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
                result = (E) new Neo4jNotificationTaskExec();
                break;

            case PROPAGATION:
                result = (E) new Neo4jPropagationTaskExec();
                break;

            case PULL:
                result = (E) new Neo4jPullTaskExec();
                break;

            case PUSH:
                result = (E) new Neo4jPushTaskExec();
                break;

            case MACRO:
                result = (E) new Neo4jMacroTaskExec();
                break;

            case SCHEDULED:
                result = (E) new Neo4jSchedTaskExec();
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
                result = Neo4jNotificationTask.NODE;
                break;

            case PROPAGATION:
                result = Neo4jPropagationTask.NODE;
                break;

            case PUSH:
                result = Neo4jPushTask.NODE;
                break;

            case PULL:
                result = Neo4jPullTask.NODE;
                break;

            case MACRO:
                result = Neo4jMacroTask.NODE;
                break;

            case SCHEDULED:
                result = Neo4jSchedTask.NODE;
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
                result = Neo4jNotificationTask.class;
                break;

            case PROPAGATION:
                result = Neo4jPropagationTask.class;
                break;

            case PUSH:
                result = Neo4jPushTask.class;
                break;

            case PULL:
                result = Neo4jPullTask.class;
                break;

            case MACRO:
                result = Neo4jMacroTask.class;
                break;

            case SCHEDULED:
                result = Neo4jSchedTask.class;
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
                result = Neo4jNotificationTaskExec.NODE;
                break;

            case PROPAGATION:
                result = Neo4jPropagationTaskExec.NODE;
                break;

            case SCHEDULED:
                result = Neo4jSchedTaskExec.NODE;
                break;

            case PUSH:
                result = Neo4jPushTaskExec.NODE;
                break;

            case PULL:
                result = Neo4jPullTaskExec.NODE;
                break;

            case MACRO:
                result = Neo4jMacroTaskExec.NODE;
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
                result = Neo4jNotificationTaskExec.class;
                break;

            case PROPAGATION:
                result = Neo4jPropagationTaskExec.class;
                break;

            case SCHEDULED:
                result = Neo4jSchedTaskExec.class;
                break;

            case PUSH:
                result = Neo4jPushTaskExec.class;
                break;

            case PULL:
                result = Neo4jPullTaskExec.class;
                break;

            case MACRO:
                result = Neo4jMacroTaskExec.class;
                break;

            default:
        }

        return result;
    }
}
