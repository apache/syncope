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
package org.apache.syncope.services;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.types.PropagationTaskExecStatus;

@Path("tasks")
public interface TaskService {

    @GET
    @Path("{kind}/count")
    int count(@PathParam("kind") final String kind);

    @POST
    <T extends TaskTO> T create(T taskTO);

    @DELETE
    @Path("{taskId}")
    <T extends TaskTO> T delete(@PathParam("taskId") final Long taskId, Class<T> type);

    @DELETE
    @Path("executions/{executionId}")
    TaskExecTO deleteExecution(@PathParam("executionId") final Long executionId);

    @POST
    @Path("{taskId}/execute")
    TaskExecTO execute(@PathParam("taskId") final Long taskId,
            @QueryParam("dryRun") @DefaultValue("false") final boolean dryRun);

    @GET
    @Path("jobClasses")
    Set<String> getJobClasses();

    @GET
    @Path("syncActionsClasses")
    Set<String> getSyncActionsClasses();

    @GET
    @Path("{kind}")
    <T extends TaskTO> List<T> list(@PathParam("kind") final String kind, Class<T[]> type);

    @GET
    @Path("{kind}")
    <T extends TaskTO> List<T> list(@PathParam("kind") final String kind, @QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size, Class<T[]> type);

    @GET
    @Path("{kind}/executions")
    List<TaskExecTO> listExecutions(@PathParam("kind") final String kind);

    @GET
    @Path("{taskId}")
    <T extends TaskTO> T read(@PathParam("taskId") final Long taskId, Class<T> type);

    @GET
    @Path("executions/{executionId}")
    TaskExecTO readExecution(@PathParam("executionId") final Long executionId);

    @POST
    @Path("executions/{executionId}/report")
    //TODO create new TaskExecutionReportTO object which contains status and message
    TaskExecTO report(@PathParam("executionId") final Long executionId,
            @HeaderParam("Execution-Status") final PropagationTaskExecStatus status, final String message);

    @PUT
    @Path("{taskId}")
    <T extends TaskTO> T update(@PathParam("taskId") final Long taskId, T taskTO);
}