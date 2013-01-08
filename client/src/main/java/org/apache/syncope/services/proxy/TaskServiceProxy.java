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

import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.services.TaskService;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.springframework.web.client.RestTemplate;

public class TaskServiceProxy extends SpringServiceProxy implements TaskService {

    public TaskServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public int count(String kind) {
        return restTemplate.getForObject(baseUrl + "task/{kind}/count.json", Integer.class, kind);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TaskTO> T create(T taskTO) {
        String subTypeString = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : "";

        return (T) restTemplate.postForObject(baseUrl + "task/create/{type}", taskTO, taskTO.getClass(),
                subTypeString);
    }

    @Override
    public <T extends TaskTO> T delete(Long taskId, Class<T> type) {
        return restTemplate.getForObject(baseUrl + "task/delete/{taskId}", type, taskId);
    }

    @Override
    public TaskExecTO deleteExecution(Long executionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TaskExecTO execute(Long taskId, boolean dryRun) {
        String param = (dryRun)
                ? "?dryRun=true"
                : "";
        return restTemplate.postForObject(baseUrl + "task/execute/{taskId}" + param, null, TaskExecTO.class,
                taskId);
    }

    @Override
    public Set<String> getJobClasses() {
        return new HashSet<String>(Arrays.asList(restTemplate.getForObject(baseUrl + "task/jobClasses.json",
                String[].class)));
    }

    @Override
    public Set<String> getSyncActionsClasses() {
        return new HashSet<String>(Arrays.asList(restTemplate.getForObject(baseUrl
                + "task/syncActionsClasses.json", String[].class)));
    }

    @Override
    public <T extends TaskTO> List<T> list(String kind, Class<T[]> type) {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "task/{kind}/list", type, kind));
    }

    @Override
    public <T extends TaskTO> List<T> list(String kind, int page, int size, Class<T[]> type) {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "task/{kind}/list/{page}/{size}.json",
                type, kind, page, size));
    }

    @Override
    public List<TaskExecTO> listExecutions(String kind) {
        return Arrays.asList(restTemplate.getForObject(baseUrl + "task/{kind}/execution/list",
                TaskExecTO[].class, kind));
    }

    @Override
    public <T extends TaskTO> T read(Long taskId, Class<T> type) {
        return restTemplate.getForObject(baseUrl + "task/read/{taskId}", type, taskId);
    }

    @Override
    public TaskExecTO readExecution(Long executionId) {
        return restTemplate.getForObject(baseUrl + "task/execution/read/{taskId}", TaskExecTO.class,
                executionId);
    }

    @Override
    public TaskExecTO report(Long executionId, PropagationTaskExecStatus status, String message) {
        return restTemplate.getForObject(baseUrl + "task/execution/report/{executionId}"
                + "?executionStatus={status}&message={message}", TaskExecTO.class, executionId, status,
                message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TaskTO> T update(Long taskId, T taskTO) {
        String path = (taskTO instanceof SyncTaskTO)
                ? "sync"
                : (taskTO instanceof SchedTaskTO)
                        ? "sched"
                        : null;
        if (path == null)
            throw new IllegalArgumentException("Task can only be instance of SchedTaskTO or SyncTaskTO");

        return (T) restTemplate.postForObject(baseUrl + "task/update/" + path, taskTO, taskTO.getClass());
    }

}
