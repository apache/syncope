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
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.JobClassTO;
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
import org.apache.syncope.core.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends AbstractServiceImpl implements TaskService, ContextAware {

    @Autowired
    private TaskController controller;

    @Override
    public int count(final TaskType taskType) {
        return controller.count(taskType);
    }

    @Override
    public Response create(final TaskTO taskTO) {
        TaskTO createdTask;
        if (taskTO instanceof SyncTaskTO || taskTO instanceof SchedTaskTO) {
            createdTask = controller.createSchedTask((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }

        TaskType taskType = TaskUtil.getInstance(taskTO.getClass()).getType();
        URI location = uriInfo.getAbsolutePathBuilder().path(taskType.toString() + "/" + createdTask.getId()).build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, createdTask.getId()).build();
    }

    @Override
    public void delete(final Long taskId) {
        controller.delete(taskId);
    }

    @Override
    public void deleteExecution(final Long executionId) {
        controller.deleteExecution(executionId);
    }

    @Override
    public TaskExecTO execute(final Long taskId, final boolean dryRun) {
        return controller.execute(taskId, dryRun);
    }

    @Override
    public Set<JobClassTO> getJobClasses() {
        return CollectionWrapper.wrapJobClasses(controller.getJobClasses());
    }

    @Override
    public Set<SyncActionClassTO> getSyncActionsClasses() {
        return CollectionWrapper.wrapSyncActionClasses(controller.getSyncActionsClasses());
    }

    @Override
    public <T extends TaskTO> List<T> list(final TaskType taskType) {
        return controller.list(taskType);
    }

    @Override
    public <T extends TaskTO> List<T> list(final TaskType taskType, final int page, final int size) {
        return controller.list(taskType, page, size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TaskTO> T read(final TaskType taskType, final Long taskId) {
        return (T) controller.read(taskId);
    }

    @Override
    public TaskExecTO readExecution(final Long executionId) {
        return controller.readExecution(executionId);
    }

    @Override
    public void report(final Long executionId, final ReportExecTO report) {
        controller.report(executionId, PropagationTaskExecStatus.fromString(report.getStatus()),
                report.getMessage());
    }

    @Override
    public void update(final Long taskId, final TaskTO taskTO) {
        if (taskTO instanceof SyncTaskTO) {
            controller.updateSync((SyncTaskTO) taskTO);
        } else if (taskTO instanceof SchedTaskTO) {
            controller.updateSched((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
    }

    @Override
    public BulkActionRes bulkAction(final BulkAction bulkAction) {
        return controller.bulkAction(bulkAction);
    }
}
