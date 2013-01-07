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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Path("tasks")
public interface TaskService {

    @GET
    @Path("{kind}/count")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/count")
    int count(@PathParam("kind") final String kind);

    //    @RequestMapping(method = RequestMethod.POST, value = "/create/sync")
    //    TaskTO createSyncTask(  final SyncTaskTO taskTO);
    //
    //    @RequestMapping(method = RequestMethod.POST, value = "/create/sched")
    //    TaskTO createSchedTask( final SchedTaskTO taskTO);

    @POST
    <T extends TaskTO> T create(T taskTO);

    @DELETE
    @Path("{taskId}")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{taskId}")
    <T extends TaskTO> T delete(@PathParam("taskId") final Long taskId, Class<T> type);

    @DELETE
    @Path("executions/{executionId}")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/delete/{executionId}")
    TaskExecTO deleteExecution(@PathParam("executionId") final Long executionId);

    @POST
    @Path("{taskId}/execute")
    @RequestMapping(method = RequestMethod.POST, value = "/execute/{taskId}")
    TaskExecTO execute(@PathParam("taskId") final Long taskId,
            @QueryParam("dryRun") @DefaultValue("false") final boolean dryRun);

    @GET
    @Path("jobClasses")
    @RequestMapping(method = RequestMethod.GET, value = "/jobClasses")
    Set<String> getJobClasses();

    @GET
    @Path("syncActionsClasses")
    @RequestMapping(method = RequestMethod.GET, value = "/syncActionsClasses")
    Set<String> getSyncActionsClasses();

    @GET
    @Path("{kind}")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    <T extends TaskTO> List<T> list(@PathParam("kind") final String kind, Class<T[]> type);

    @GET
    @Path("{kind}")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list/{page}/{size}")
    <T extends TaskTO> List<T> list(@PathParam("kind") final String kind, @QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size, Class<T[]> type);

    @GET
    @Path("{kind}/executions")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/execution/list")
    List<TaskExecTO> listExecutions(@PathParam("kind") final String kind);

    @GET
    @Path("{taskId}")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{taskId}")
    <T extends TaskTO> T read(@PathParam("taskId") final Long taskId, Class<T> type);

    @GET
    @Path("executions/{executionId}")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/read/{executionId}")
    TaskExecTO readExecution(@PathParam("executionId") final Long executionId);

    @POST
    @Path("executions/{executionId}/report")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/report/{executionId}")
    TaskExecTO report(@PathParam("executionId") final Long executionId,
            @HeaderParam("Execution-Status") final PropagationTaskExecStatus status, final String message);

    //    @RequestMapping(method = RequestMethod.POST, value = "/update/sync")
    //    TaskTO updateSync(final SyncTaskTO taskTO);
    //
    //    @RequestMapping(method = RequestMethod.POST, value = "/update/sched")
    //    TaskTO updateSched(final SchedTaskTO taskTO);

    @PUT
    @Path("{taskId}")
    <T extends TaskTO> T update(@PathParam("taskId") final Long taskId, T taskTO);
}