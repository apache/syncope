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
package org.apache.syncope.common.services;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.to.JobClassTO;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.SyncActionClassTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.TaskTO;
import org.apache.syncope.common.types.TaskType;

@Path("tasks")
public interface TaskService {

    /**
     * @param taskType
     * @return
     */
    @GET
    @Path("{type}/count")
    int count(@PathParam("type") TaskType taskType);

    /**
     * @param taskTO
     * @return
     */
    @POST
    Response create(TaskTO taskTO);

    /**
     * @param taskId
     */
    @DELETE
    @Path("{taskId}")
    void delete(@PathParam("taskId") Long taskId);

    /**
     * @param executionId
     */
    @DELETE
    @Path("executions/{executionId}")
    void deleteExecution(@PathParam("executionId") Long executionId);

    /**
     * @param taskId
     * @param dryRun
     * @return
     */
    @POST
    @Path("{taskId}/execute")
    TaskExecTO execute(@PathParam("taskId") Long taskId, @QueryParam("dryRun") @DefaultValue("false") boolean dryRun);

    /**
     * @return
     */
    @GET
    @Path("jobClasses")
    Set<JobClassTO> getJobClasses();

    /**
     * @return
     */
    @GET
    @Path("syncActionsClasses")
    Set<SyncActionClassTO> getSyncActionsClasses();

    /**
     * @param taskType
     * @return
     */
    @GET
    @Path("{type}/list")
    // TODO '/list' path will be removed once CXF/JAX-B bug is solved
    List<? extends TaskTO> list(@PathParam("type") TaskType taskType);

    /**
     * @param taskType
     * @param page
     * @param size
     * @return
     */
    @GET
    @Path("{type}")
    List<? extends TaskTO> list(@PathParam("type") TaskType taskType, @QueryParam("page") int page,
            @QueryParam("size") @DefaultValue("25") int size);

    /**
     * @param taskType
     * @return
     */
    @GET
    @Path("{type}/executions")
    List<TaskExecTO> listExecutions(@PathParam("type") TaskType taskType);

    /**
     * @param taskType
     * @param taskId
     * @return
     */
    @GET
    @Path("{type}/{taskId}")
    // TODO TaskType will be removed once CXF migration is done
    <T extends TaskTO> T read(@PathParam("type") TaskType taskType, @PathParam("taskId") Long taskId);

    /**
     * @param executionId
     * @return
     */
    @GET
    @Path("executions/{executionId}")
    TaskExecTO readExecution(@PathParam("executionId") Long executionId);

    /**
     * @param executionId
     * @param status
     * @param message
     * @return
     */
    @POST
    @Path("executions/{executionId}/report")
    TaskExecTO report(@PathParam("executionId") Long executionId, ReportExecTO report);

    /**
     * @param taskId
     * @param taskTO
     */
    @PUT
    @Path("{taskId}")
    void update(@PathParam("taskId") Long taskId, TaskTO taskTO);

}
