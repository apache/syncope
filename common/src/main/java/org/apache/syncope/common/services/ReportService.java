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

import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;

@Path("reports")
public interface ReportService {

    /**
     * @return Returns number of existing reports
     */
    @GET
    @Path("count")
    int count();

    /**
     * @param reportTO Report to be created
     * @return Response containing URI location for created resource
     */
    @POST
    Response create(ReportTO reportTO);

    /**
     * @param reportId Deletes report with matching id
     */
    @DELETE
    @Path("{reportId}")
    void delete(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID of execution report to be deleted
     */
    @DELETE
    @Path("executions/{executionId}")
    void deleteExecution(@PathParam("executionId") Long executionId);

    /**
     * @param reportId ID of report to be executed.
     * @return Execution result
     */
    @POST
    @Path("{reportId}/execute")
    ReportExecTO execute(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID of execution report to be selected
     * @param fmt file-format selection
     * @return Returns a stream for content download
     */
    @GET
    @Path("executions/{executionId}/dbDump")
    Response exportExecutionResult(@PathParam("executionId") Long executionId,
            @QueryParam("format") ReportExecExportFormat fmt);

    /**
     * @return Returns a list of all reportletConfClasses
     */
    @GET
    @Path("reportletConfClasses")
    Set<String> getReportletConfClasses();

    /**
     * @return Returns a list of all reports
     */
    @GET
    List<ReportTO> list();

    /**
     * @param page selected page in relation to size
     * @param size Number of items per page
     * @return Returns a list of reports according to pagination
     */
    @GET
    List<ReportTO> list(@QueryParam("page") int page,
            @QueryParam("size") @DefaultValue("25") int size);

    /**
     * @return Returns list of all previous executions
     */
    @GET
    @Path("executions")
    List<ReportExecTO> listExecutions();

    /**
     * @param reportId ID of report to be read
     * @return Report with matching ID
     */
    @GET
    @Path("{reportId}")
    ReportTO read(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID ExecutionReport to be selected
     * @return Returns ExecutionReport with matching id
     */
    @GET
    @Path("executions/{executionId}")
    ReportExecTO readExecution(@PathParam("executionId") Long executionId);

    /**
     * @param reportId ID for report to be updated
     * @param reportTO Updates report with matching reportId
     */
    @PUT
    @Path("{reportId}")
    void update(@PathParam("reportId") Long reportId, ReportTO reportTO);

}
