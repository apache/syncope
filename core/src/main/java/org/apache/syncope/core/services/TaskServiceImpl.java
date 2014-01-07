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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.wrap.JobClass;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.SchedTaskTO;
import org.apache.syncope.common.wrap.SyncActionClass;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.wrap.PushActionClass;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.rest.controller.TaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends AbstractServiceImpl implements TaskService {

    @Autowired
    private TaskController controller;

    @Override
    public <T extends SchedTaskTO> Response create(final T taskTO) {
        T createdTask;
        if (taskTO instanceof SyncTaskTO || taskTO instanceof SchedTaskTO) {
            createdTask = controller.createSchedTask(taskTO);
        } else {
            throw new BadRequestException();
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdTask.getId())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, createdTask.getId()).
                build();
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
    public List<JobClass> getJobClasses() {
        return CollectionWrapper.wrap(controller.getJobClasses(), JobClass.class);
    }

    @Override
    public List<SyncActionClass> getSyncActionsClasses() {
        return CollectionWrapper.wrap(controller.getSyncActionsClasses(), SyncActionClass.class);
    }

    @Override
    public List<PushActionClass> getPushActionsClasses() {
        return CollectionWrapper.wrap(controller.getPushActionsClasses(), PushActionClass.class);
    }

    @Override
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType taskType) {
        return list(taskType, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType taskType, final String orderBy) {
        return list(taskType, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType taskType, final int page, final int size) {
        return list(taskType, page, size, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType taskType,
            final int page, final int size, final String orderBy) {

        checkPageSize(page, size);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return (PagedResult<T>) buildPagedResult(
                controller.list(taskType, page, size, orderByClauses), page, size, controller.count(taskType));
    }

    @Override
    public <T extends AbstractTaskTO> T read(final Long taskId) {
        return controller.read(taskId);
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
    public void update(final Long taskId, final AbstractTaskTO taskTO) {
        if (taskTO instanceof SyncTaskTO) {
            controller.updateSync((SyncTaskTO) taskTO);
        } else if (taskTO instanceof SchedTaskTO) {
            controller.updateSched((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }
}
