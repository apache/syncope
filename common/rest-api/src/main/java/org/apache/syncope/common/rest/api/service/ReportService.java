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
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.beans.BulkExecDeleteQuery;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;

/**
 * REST operations for reports.
 */
@Path("reports")
public interface ReportService extends JAXRSService {

    /**
     * Returns report with matching key.
     *
     * @param key key of report to be read
     * @return report with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ReportTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of all existing reports.
     *
     * @return paged list of existing reports matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ReportTO> list();

    /**
     * Creates a new report.
     *
     * @param reportTO report to be created
     * @return Response object featuring Location header of created report
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull ReportTO reportTO);

    /**
     * Updates report with matching key.
     *
     * @param reportTO report to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull ReportTO reportTO);

    /**
     * Deletes report with matching key.
     *
     * @param key Deletes report with matching key
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Returns the list of recently completed report executions, ordered by end date descendent.
     *
     * @param max the maximum number of executions to return
     * @return list of recently completed report executions, ordered by end date descendent
     */
    @GET
    @Path("executions/recent")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ExecTO> listRecentExecutions(@Min(1) @QueryParam(JAXRSService.PARAM_MAX) @DefaultValue("25") int max);

    /**
     * Deletes report execution with matching key.
     *
     * @param executionKey key of execution report to be deleted
     */
    @DELETE
    @Path("executions/{executionKey}")
    void deleteExecution(@NotNull @PathParam("executionKey") String executionKey);

    /**
     * Deletes the report executions belonging matching the given query.
     *
     * @param query query conditions
     * @return bulk action result
     */
    @DELETE
    @Path("{key}/executions")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult deleteExecutions(@BeanParam BulkExecDeleteQuery query);

    /**
     * Executes the report matching the given query.
     *
     * @param query query conditions
     * @return execution report for the report matching the given query
     */
    @POST
    @Path("{key}/execute")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ExecTO execute(@BeanParam ExecuteQuery query);

    /**
     * Exports the report execution with matching key in the requested format.
     *
     * @param executionKey key of execution report to be selected
     * @param fmt file-format selection
     * @return a stream for content download
     */
    @GET
    @Path("executions/{executionKey}/stream")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response exportExecutionResult(
            @NotNull @PathParam("executionKey") String executionKey,
            @QueryParam("format") ReportExecExportFormat fmt);

    /**
     * List report jobs (running and / or scheduled).
     *
     * @return report jobs (running and / or scheduled)
     */
    @GET
    @Path("jobs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<JobTO> listJobs();

    /**
     * Executes an action on an existing report's job.
     *
     * @param key report key
     * @param action action to execute
     */
    @POST
    @Path("jobs/{key}")
    void actionJob(@NotNull @PathParam("key") String key, @QueryParam("action") JobAction action);
}
