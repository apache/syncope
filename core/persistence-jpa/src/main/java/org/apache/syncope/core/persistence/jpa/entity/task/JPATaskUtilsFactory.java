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

import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;

@Component
public class JPATaskUtilsFactory implements TaskUtilsFactory {

    @Override
    public TaskUtils getInstance(final TaskType type) {
        return new JPATaskUtils(type);
    }

    @Override
    public TaskUtils getInstance(final Task task) {
        TaskType type;
        if (task instanceof PullTask) {
            type = TaskType.PULL;
        } else if (task instanceof PushTask) {
            type = TaskType.PUSH;
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

    @Override
    public TaskUtils getInstance(final Class<? extends AbstractTaskTO> taskClass) {
        TaskType type;
        if (taskClass == PropagationTaskTO.class) {
            type = TaskType.PROPAGATION;
        } else if (taskClass == NotificationTaskTO.class) {
            type = TaskType.NOTIFICATION;
        } else if (taskClass == SchedTaskTO.class) {
            type = TaskType.SCHEDULED;
        } else if (taskClass == PullTaskTO.class) {
            type = TaskType.PULL;
        } else if (taskClass == PushTaskTO.class) {
            type = TaskType.PUSH;
        } else {
            throw new IllegalArgumentException("Invalid TaskTO class: " + taskClass.getName());
        }

        return getInstance(type);
    }

    @Override
    public TaskUtils getInstance(final AbstractTaskTO taskTO) {
        return getInstance(taskTO.getClass());
    }

}
