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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;

/**
 * REST operations for reports.
 */
@Path("reports")
public interface ReportService extends ExecutableService {

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
}
