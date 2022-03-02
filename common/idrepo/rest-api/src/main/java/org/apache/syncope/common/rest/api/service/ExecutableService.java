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

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecDeleteQuery;
import org.apache.syncope.common.rest.api.beans.ExecListQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;

public interface ExecutableService extends JAXRSService {

    /**
     * Returns a paged list of executions matching the given query.
     *
     * @param query query conditions
     * @return paged list of executions the given query
     */
    @GET
    @Path("{key}/executions")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<ExecTO> listExecutions(@BeanParam ExecListQuery query);

    /**
     * Returns the list of recently completed executions, ordered by end date descendent.
     *
     * @param max the maximum number of executions to return
     * @return list of recently completed executions, ordered by end date descendent
     */
    @GET
    @Path("executions/recent")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ExecTO> listRecentExecutions(@Min(1) @QueryParam(JAXRSService.PARAM_MAX) @DefaultValue("25") int max);

    /**
     * Deletes the executable execution matching the provided key.
     *
     * @param executionKey key of executable execution to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("executions/{executionKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void deleteExecution(@NotNull @PathParam("executionKey") String executionKey);

    /**
     * Deletes the executions matching the given query.
     *
     * @param query query conditions
     * @return batch results as Response entity
     */
    @DELETE
    @ApiResponses(
            @ApiResponse(responseCode = "200",
                    description = "Batch results available, returned as Response entity"))
    @Path("{key}/executions")
    @Produces(RESTHeaders.MULTIPART_MIXED)
    Response deleteExecutions(@BeanParam ExecDeleteQuery query);

    /**
     * Executes the executable matching the given specs.
     *
     * @param specs conditions to exec
     * @return execution report for the executable matching the given specs
     */
    @POST
    @Path("{key}/execute")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ExecTO execute(@BeanParam ExecSpecs specs);

    /**
     * Returns job (running or scheduled) for the executable matching the given key.
     *
     * @param key executable key
     * @return job (running or scheduled) for the given key
     */
    @GET
    @Path("jobs/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    JobTO getJob(@PathParam("key") String key);

    /**
     * List jobs (running and / or scheduled).
     *
     * @return jobs (running and / or scheduled)
     */
    @GET
    @Path("jobs")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<JobTO> listJobs();

    /**
     * Executes an action on an existing executable's job.
     *
     * @param key executable key
     * @param action action to execute
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("jobs/{key}")
    void actionJob(@NotNull @PathParam("key") String key, @QueryParam("action") JobAction action);
}
