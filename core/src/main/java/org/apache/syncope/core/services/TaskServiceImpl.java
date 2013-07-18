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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.NotificationTaskTO;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.TaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService, ContextAware {

    @Autowired
    private TaskController taskController;

    private UriInfo uriInfo;

    @Override
    public int count(final TaskType taskType) {
        return taskController.countInternal(taskType.toString());
    }

    @Override
    public Response create(final TaskTO taskTO) {
        TaskTO createdTask;
        if (taskTO instanceof SyncTaskTO || taskTO instanceof SchedTaskTO) {
            createdTask = taskController.createSchedTaskInternal((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
        TaskType taskType = getTaskType(taskTO.getClass());
        URI location = uriInfo.getAbsolutePathBuilder().path(taskType.toString() + "/" + createdTask.getId()).build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, createdTask.getId()).build();
    }

    private TaskType getTaskType(Class<? extends TaskTO> taskClass) {
        if (taskClass == PropagationTaskTO.class) {
            return TaskType.PROPAGATION;
        } else if (taskClass == NotificationTaskTO.class) {
            return TaskType.NOTIFICATION;
        } else if (taskClass == SchedTaskTO.class) {
            return TaskType.SCHEDULED;
        } else if (taskClass == SyncTaskTO.class) {
            return TaskType.SYNCHRONIZATION;
        } else {
            throw new IllegalArgumentException("Invalid task class: " + taskClass.getName());
        }
    }

    @Override
    public void delete(final Long taskId) {
        taskController.delete(taskId);
    }

    @Override
    public void deleteExecution(final Long executionId) {
        taskController.deleteExecution(executionId);
    }

    @Override
    public TaskExecTO execute(final Long taskId, final boolean dryRun) {
        return taskController.execute(taskId, dryRun);
    }

    @Override
    public Set<JobClassTO> getJobClasses() {
        @SuppressWarnings("unchecked")
        Set<String> jobClasses = (Set<String>) taskController.getJobClasses().getModel().values().iterator().next();
        return CollectionWrapper.wrapJobClasses(jobClasses);
    }

    @Override
    public Set<SyncActionClassTO> getSyncActionsClasses() {
        @SuppressWarnings("unchecked")
        Set<String> actionClasses = (Set<String>) taskController.getSyncActionsClasses().getModel().values().iterator()
                .next();
        return CollectionWrapper.wrapSyncActionClasses(actionClasses);
    }

    @Override
    public <T extends TaskTO> List<T> list(final TaskType taskType) {
        return (List<T>) taskController.list(taskType.toString());
    }

    @Override
    public <T extends TaskTO> List<T> list(final TaskType taskType, final int page, final int size) {
        return (List<T>) taskController.list(taskType.toString(), page, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TaskTO> T read(final TaskType taskType, final Long taskId) {
        return (T) taskController.read(taskId);
    }

    @Override
    public TaskExecTO readExecution(final Long executionId) {
        return taskController.readExecution(executionId);
    }

    @Override
    public void report(final Long executionId, final ReportExecTO report) {
        taskController.report(executionId, PropagationTaskExecStatus.fromString(report.getStatus()),
                report.getMessage());
    }

    @Override
    public void update(final Long taskId, final TaskTO taskTO) {
        if (taskTO instanceof SyncTaskTO) {
            taskController.updateSync((SyncTaskTO) taskTO);
        } else if (taskTO instanceof SchedTaskTO) {
            taskController.updateSched((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }

    @Override
    public BulkActionRes bulkAction(BulkAction bulkAction) {
        return taskController.bulkAction(bulkAction);
    }
}
