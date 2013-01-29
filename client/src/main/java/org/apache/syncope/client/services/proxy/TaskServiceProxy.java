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
package org.apache.syncope.client.services.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("unchecked")
public class TaskServiceProxy extends SpringServiceProxy implements TaskService {

    public TaskServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public int count(final TaskType type) {
        return getRestTemplate().getForObject(baseUrl + "task/{type}/count.json", Integer.class, type);
    }

    @Override
    public Response create(final TaskTO taskTO) {
        String subTypeString = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : "";

        TaskTO task = getRestTemplate().postForObject(baseUrl + "task/create/{type}", taskTO, taskTO.getClass(),
                subTypeString);

        return Response.created(URI.create(baseUrl + "task/read/" + task.getId() + ".json")).build();
    }

    @Override
    public void delete(final Long taskId) {
        try {
            getRestTemplate().getRequestFactory()
                    .createRequest(URI.create(baseUrl + "task/delete/" + taskId), HttpMethod.GET).execute();
        } catch (IOException e) {
            // TODO log event
            e.printStackTrace();
        }
    }

    @Override
    public void deleteExecution(final Long executionId) {
        getRestTemplate().getForObject(baseUrl + "task/execution/delete/{executionId}.json", TaskExecTO.class, executionId);
    }

    @Override
    public TaskExecTO execute(final Long taskId, final boolean dryRun) {
        String param = (dryRun)
                ? "?dryRun=true"
                : "";
        return getRestTemplate().postForObject(baseUrl + "task/execute/{taskId}.json" + param, null, TaskExecTO.class,
                taskId);
    }

    @Override
    public Set<JobClassTO> getJobClasses() {
        Set<String> classes = new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "task/jobClasses.json", String[].class)));

        return CollectionWrapper.wrapJobClasses(classes);
    }

    @Override
    public Set<SyncActionClassTO> getSyncActionsClasses() {
        Set<String> classes = new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "task/syncActionsClasses.json", String[].class)));

        return CollectionWrapper.wrapSyncActionClasses(classes);
    }

    @Override
    public List<? extends TaskTO> list(final TaskType type) {
        switch (type) {
            case PROPAGATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list.json",
                        PropagationTaskTO[].class, type));

            case NOTIFICATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list.json",
                        NotificationTaskTO[].class, type));

            case SCHEDULED:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list.json",
                        SchedTaskTO[].class, type));

            case SYNCHRONIZATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list.json",
                        SyncTaskTO[].class, type));

            default:
                throw new IllegalArgumentException("TaskType is not supported.");
        }
    }

    @Override
    public List<? extends TaskTO> list(final TaskType type, final int page, final int size) {
        switch (type) {
            case PROPAGATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                        PropagationTaskTO[].class, type, page, size));

            case NOTIFICATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                        NotificationTaskTO[].class, type, page, size));

            case SCHEDULED:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                        SchedTaskTO[].class, type, page, size));

            case SYNCHRONIZATION:
                return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/list/{page}/{size}.json",
                        SyncTaskTO[].class, type, page, size));

            default:
                throw new IllegalArgumentException("TaskType is not supported :" + type);
        }
    }

    @Override
    public List<TaskExecTO> listExecutions(final TaskType type) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "task/{type}/execution/list.json", TaskExecTO[].class,
                type));
    }

    @Override
    public <T extends TaskTO> T read(final TaskType type, final Long taskId) {
        return (T) getRestTemplate().getForObject(baseUrl + "task/read/{taskId}.json", getTOClass(type), taskId);
    }

    @Override
    public TaskExecTO readExecution(final Long executionId) {
        return getRestTemplate().getForObject(baseUrl + "task/execution/read/{taskId}.json", TaskExecTO.class,
                executionId);
    }

    @Override
    public TaskExecTO report(final Long executionId, final PropagationTaskExecStatus status, final String message) {
        return getRestTemplate().getForObject(
                baseUrl + "task/execution/report/{executionId}.json" + "?executionStatus={status}&message={message}",
                TaskExecTO.class, executionId, status, message);
    }

    @Override
    public void update(final Long taskId, final TaskTO taskTO) {
        String path = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : null;
        if (path == null) {
            throw new IllegalArgumentException("Task can only be instance of SchedTaskTO or SyncTaskTO");
        }

        getRestTemplate().postForObject(baseUrl + "task/update/" + path, taskTO, taskTO.getClass());
    }

    private Class<? extends TaskTO> getTOClass(final TaskType type) {
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
                throw new IllegalArgumentException("SchemaType is not supported: " + type);
        }
    }
}
