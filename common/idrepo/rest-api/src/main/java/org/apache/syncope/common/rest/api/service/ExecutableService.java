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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
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
    PagedResult<ExecTO> listExecutions(@BeanParam ExecQuery query);

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
    Response deleteExecutions(@BeanParam ExecQuery query);

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
