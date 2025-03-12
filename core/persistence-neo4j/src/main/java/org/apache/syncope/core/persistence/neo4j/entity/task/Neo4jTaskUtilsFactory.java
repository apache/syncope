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

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;

public class Neo4jTaskUtilsFactory implements TaskUtilsFactory {

    protected final Map<TaskType, TaskUtils> instances = new HashMap<>(5);

    @Override
    public TaskUtils getInstance(final TaskType type) {
        TaskUtils instance;
        synchronized (instances) {
            instance = instances.get(type);
            if (instance == null) {
                instance = new Neo4jTaskUtils(type);
                ApplicationContextProvider.getBeanFactory().autowireBean(instance);
                instances.put(type, instance);
            }
        }

        return instance;
    }

    @Override
    public TaskUtils getInstance(final Task<?> task) {
        TaskType type;
        if (task instanceof LiveSyncTask) {
            type = TaskType.LIVE_SYNC;
        } else if (task instanceof PullTask) {
            type = TaskType.PULL;
        } else if (task instanceof PushTask) {
            type = TaskType.PUSH;
        } else if (task instanceof MacroTask) {
            type = TaskType.MACRO;
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
    public TaskUtils getInstance(final Class<? extends TaskTO> taskClass) {
        TaskType type;
        if (taskClass == PropagationTaskTO.class) {
            type = TaskType.PROPAGATION;
        } else if (taskClass == NotificationTaskTO.class) {
            type = TaskType.NOTIFICATION;
        } else if (taskClass == SchedTaskTO.class) {
            type = TaskType.SCHEDULED;
        } else if (taskClass == LiveSyncTaskTO.class) {
            type = TaskType.LIVE_SYNC;
        } else if (taskClass == PullTaskTO.class) {
            type = TaskType.PULL;
        } else if (taskClass == PushTaskTO.class) {
            type = TaskType.PUSH;
        } else if (taskClass == MacroTaskTO.class) {
            type = TaskType.MACRO;
        } else {
            throw new IllegalArgumentException("Invalid TaskTO class: " + taskClass.getName());
        }

        return getInstance(type);
    }

    @Override
    public TaskUtils getInstance(final TaskTO taskTO) {
        return getInstance(taskTO.getClass());
    }
}
