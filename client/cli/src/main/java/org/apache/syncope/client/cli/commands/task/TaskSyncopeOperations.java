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
package org.apache.syncope.client.cli.commands.task;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;

public class TaskSyncopeOperations {

    private final TaskService taskService = SyncopeServices.get(TaskService.class);

    public List<JobTO> listJobs() {
        return taskService.listJobs();
    }

    public <T extends AbstractTaskTO> T read(final String taskKey) {
        return taskService.read(taskKey, true);
    }

    public void delete(final String taskKey) {
        taskService.delete(taskKey);
    }

    public List<AbstractTaskTO> list(final String type) {
        return taskService.list(new TaskQuery.Builder(TaskType.valueOf(type)).build()).getResult();
    }

    public List<AbstractTaskTO> listPropagationTask() {
        return taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).build()).getResult();
    }

    public void deleteExecution(final String executionKey) {
        taskService.deleteExecution(executionKey);
    }

    public ExecTO execute(final String executionKey, final boolean dryRun) {
        return taskService.execute(
                new ExecuteQuery.Builder().key(executionKey).dryRun(dryRun).build());
    }
}
