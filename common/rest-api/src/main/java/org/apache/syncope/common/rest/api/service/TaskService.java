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
package org.apache.syncope.common.rest.api.service;

import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.beans.BulkExecDeleteQuery;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;

/**
 * REST operations for tasks.
 */
@Path("tasks")
public interface TaskService extends JAXRSService {

    /**
     * Returns the task matching the given key.
     *
     * @param key key of task to be read
     * @param details whether include executions or not, defaults to true
     * @param <T> type of taskTO
     * @return task with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractTaskTO> T read(
            @NotNull @PathParam("key") Long key,
            @QueryParam(JAXRSService.PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Returns a paged list of existing tasks matching the given query.
     *
     * @param query query conditions
     * @param <T> type of taskTO
     * @return paged list of existing tasks matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractTaskTO> PagedResult<T> list(@BeanParam TaskQuery query);

    /**
     * Creates a new task.
     *
     * @param taskTO task to be created
     * @return Response object featuring Location header of created task
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull SchedTaskTO taskTO);

    /**
     * Updates the task matching the provided key.
     *
     * @param taskTO updated task to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull AbstractTaskTO taskTO);

    /**
     * Deletes the task matching the provided key.
     *
     * @param key key of task to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") Long key);

    /**
     * Returns a paged list of task executions matching the given query.
     *
     * @param query query conditions
     * @return paged list of task executions the given query
     */
    @GET
    @Path("{key}/executions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedResult<ExecTO> listExecutions(@BeanParam TaskExecQuery query);

    /**
     * Returns the list of recently completed task executions, ordered by end date descendent.
     *
     * @param max the maximum number of executions to return
     * @return list of recently completed task executions, ordered by end date descendent
     */
    @GET
    @Path("executions/recent")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ExecTO> listRecentExecutions(@Min(1) @QueryParam(JAXRSService.PARAM_MAX) @DefaultValue("25") int max);

    /**
     * Deletes the task execution matching the provided key.
     *
     * @param executionKey key of task execution to be deleted
     */
    @DELETE
    @Path("executions/{executionKey}")
    void deleteExecution(@NotNull @PathParam("executionKey") Long executionKey);

    /**
     * Deletes the task executions belonging matching the given query.
     *
     * @param query query conditions
     * @return bulk action result
     */
    @DELETE
    @Path("{key}/executions")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult deleteExecutions(@BeanParam BulkExecDeleteQuery query);

    /**
     * Executes the task matching the given query.
     *
     * @param query query conditions
     * @return execution report for the task matching the given query
     */
    @POST
    @Path("{key}/execute")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ExecTO execute(@BeanParam ExecuteQuery query);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of task ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);

    /**
     * List task jobs (running and / or scheduled).
     *
     * @return task jobs (running and / or scheduled)
     */
    @GET
    @Path("jobs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<JobTO> listJobs();

    /**
     * Executes an action on an existing task's job.
     *
     * @param key task key
     * @param action action to execute
     */
    @POST
    @Path("jobs/{key}")
    void actionJob(@NotNull @PathParam("key") Long key, @QueryParam("action") JobAction action);
}
