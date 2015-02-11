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
package org.apache.syncope.core.rest.cxf.service;

import java.net.URI;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.logic.TaskLogic;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends AbstractServiceImpl implements TaskService {

    @Autowired
    private TaskLogic logic;

    @Override
    public <T extends SchedTaskTO> Response create(final T taskTO) {
        T createdTask;
        if (taskTO instanceof SyncTaskTO || taskTO instanceof PushTaskTO || taskTO instanceof SchedTaskTO) {
            createdTask = logic.createSchedTask(taskTO);
        } else {
            throw new BadRequestException();
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdTask.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, createdTask.getKey()).
                build();
    }

    @Override
    public void delete(final Long taskKey) {
        logic.delete(taskKey);
    }

    @Override
    public void deleteExecution(final Long executionKey) {
        logic.deleteExecution(executionKey);
    }

    @Override
    public TaskExecTO execute(final Long taskKey, final boolean dryRun) {
        return logic.execute(taskKey, dryRun);
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
    public <T extends AbstractTaskTO> PagedResult<T> list(
            final TaskType taskType, final Integer page, final Integer size) {

        return list(taskType, page, size, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractTaskTO> PagedResult<T> list(final TaskType taskType,
            final Integer page, final Integer size, final String orderBy) {

        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return (PagedResult<T>) buildPagedResult(
                logic.list(taskType, page, size, orderByClauses), page, size, logic.count(taskType));
    }

    @Override
    public <T extends AbstractTaskTO> T read(final Long taskKey) {
        return logic.read(taskKey);
    }

    @Override
    public TaskExecTO readExecution(final Long executionKey) {
        return logic.readExecution(executionKey);
    }

    @Override
    public void report(final Long executionKey, final ReportExecTO reportExec) {
        reportExec.setKey(executionKey);
        logic.report(
                executionKey, PropagationTaskExecStatus.fromString(reportExec.getStatus()), reportExec.getMessage());
    }

    @Override
    public void update(final Long taskKey, final AbstractTaskTO taskTO) {
        taskTO.setKey(taskKey);
        if (taskTO instanceof SyncTaskTO) {
            logic.updateSync((SyncTaskTO) taskTO);
        } else if (taskTO instanceof SchedTaskTO) {
            logic.updateSched((SchedTaskTO) taskTO);
        } else {
            throw new BadRequestException();
        }
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return logic.bulk(bulkAction);
    }
}
