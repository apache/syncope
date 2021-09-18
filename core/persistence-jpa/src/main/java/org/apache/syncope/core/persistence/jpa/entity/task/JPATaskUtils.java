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

import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;

@SuppressWarnings("unchecked")
public final class JPATaskUtils implements TaskUtils {

    protected final EntityFactory entityFactory;

    protected final TaskType type;

    protected JPATaskUtils(final EntityFactory entityFactory, final TaskType type) {
        this.entityFactory = entityFactory;
        this.type = type;
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public <T extends Task> Class<T> taskClass() {
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

            case NOTIFICATION:
                result = (Class<T>) NotificationTask.class;
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends Task> T newTask() {
        T result = null;

        switch (type) {
            case PROPAGATION:
                result = (T) entityFactory.newEntity(PropagationTask.class);
                break;

            case SCHEDULED:
                result = (T) entityFactory.newEntity(SchedTask.class);
                break;

            case PULL:
                result = (T) entityFactory.newEntity(PullTask.class);
                break;

            case PUSH:
                result = (T) entityFactory.newEntity(PushTask.class);
                break;

            case NOTIFICATION:
                result = (T) entityFactory.newEntity(NotificationTask.class);
                break;

            default:
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

            case NOTIFICATION:
                result = (Class<T>) NotificationTaskTO.class;
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends TaskTO> T newTaskTO() {
        final Class<T> taskClass = taskTOClass();
        try {
            return taskClass == null ? null : taskClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
