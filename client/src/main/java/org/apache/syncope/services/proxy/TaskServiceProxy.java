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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.client.to.NotificationTaskTO;
import org.apache.syncope.client.to.PropagationTaskTO;
import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.services.TaskService;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.types.TaskType;

@SuppressWarnings("unchecked")
public class TaskServiceProxy extends SpringServiceProxy implements TaskService {

    public TaskServiceProxy(String baseUrl, SpringRestTemplate callback) {
        super(baseUrl, callback);
    }

    @Override
    public int count(TaskType type) {
        return getRestTemplate().getForObject(baseUrl + "task/{type}/count.json", Integer.class, type);
    }

    @Override
    public <T extends TaskTO> T create(T taskTO) {
        String subTypeString = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : "";

        return (T) getRestTemplate().postForObject(baseUrl + "task/create/{type}", taskTO, taskTO.getClass(), subTypeString);
    }

    @Override
    public <T extends TaskTO> T delete(TaskType type, Long taskId) {
        return (T) getRestTemplate().getForObject(baseUrl + "task/delete/{taskId}", getTOClass(type), taskId);
    }

    @Override
    public TaskExecTO deleteExecution(Long executionId) {
        return getRestTemplate()
                .getForObject(baseUrl + "task/execution/delete/{executionId}", TaskExecTO.class, executionId);
    }

    @Override
    public TaskExecTO execute(Long taskId, boolean dryRun) {
        String param = (dryRun)
                ? "?dryRun=true"
                : "";
        return getRestTemplate().postForObject(baseUrl + "task/execute/{taskId}" + param, null, TaskExecTO.class, taskId);
    }

    @Override
    public Set<String> getJobClasses() {
        return new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/jobClasses.json",
                String[].class)));
    }

    @Override
    public Set<String> getSyncActionsClasses() {
        return new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/syncActionsClasses.json",
                String[].class)));
    }

    @Override
    public <T extends TaskTO> List<T> list(TaskType type) {
        switch (type) {
        case PROPAGATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list",
                    PropagationTaskTO[].class, type));
        case NOTIFICATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list",
                    NotificationTaskTO[].class, type));
        case SCHEDULED:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list", SchedTaskTO[].class,
                    type));
        case SYNCHRONIZATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list", SyncTaskTO[].class,
                    type));
        default:
            throw new IllegalArgumentException("TaskType is not supported.");
        }
    }

    private Class<? extends TaskTO> getTOClass(TaskType type) {
        switch (type) {
        case PROPAGATION:
            return PropagationTaskTO.class;
        case NOTIFICATION:
            return NotificationTaskTO.class;
        case SCHEDULED:
            return SchedTaskTO.class;
        case SYNCHRONIZATION:
            return SyncTaskTO.class;
        default:
            throw new IllegalArgumentException("SchemaType is not supported.");
        }
    }

    @Override
    public <T extends TaskTO> List<T> list(TaskType type, int page, int size) {
        switch (type) {
        case PROPAGATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                    PropagationTaskTO[].class, type, page, size));
        case NOTIFICATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                    NotificationTaskTO[].class, type, page, size));
        case SCHEDULED:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                    SchedTaskTO[].class, type, page, size));
        case SYNCHRONIZATION:
            return (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                    SyncTaskTO[].class, type, page, size));
        default:
            throw new IllegalArgumentException("TaskType is not supported.");
        }
    }

    @Override
    public List<TaskExecTO> listExecutions(TaskType type) {
        return Arrays.asList(getRestTemplate()
                .getForObject(baseUrl + "task/{type}/execution/list", TaskExecTO[].class, type));
    }

    @Override
    public <T extends TaskTO> T read(TaskType type, Long taskId) {
        return (T) getRestTemplate().getForObject(baseUrl + "task/read/{taskId}", getTOClass(type), taskId);
    }

    @Override
    public TaskExecTO readExecution(Long executionId) {
        return getRestTemplate().getForObject(baseUrl + "task/execution/read/{taskId}", TaskExecTO.class, executionId);
    }

    @Override
    public TaskExecTO report(Long executionId, PropagationTaskExecStatus status, String message) {
        return getRestTemplate().getForObject(baseUrl + "task/execution/report/{executionId}"
                + "?executionStatus={status}&message={message}", TaskExecTO.class, executionId, status, message);
    }

    @Override
    public <T extends TaskTO> T update(Long taskId, T taskTO) {
        String path = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : null;
        if (path == null)
            throw new IllegalArgumentException("Task can only be instance of SchedTaskTO or SyncTaskTO");

        return (T) getRestTemplate().postForObject(baseUrl + "task/update/" + path, taskTO, taskTO.getClass());
    }

}
