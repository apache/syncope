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
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.lib.wrap.ReportletConfClass;

/**
 * REST operations for reports.
 */
@Path("reports")
public interface ReportService extends JAXRSService {

    /**
     * Returns a list of available classes for reportlet configuration.
     *
     * @return list of available classes for reportlet configuration
     */
    @GET
    @Path("reportletConfClasses")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ReportletConfClass> getReportletConfClasses();

    /**
     * Returns report with matching key.
     *
     * @param reportKey key of report to be read
     * @return report with matching key
     */
    @GET
    @Path("{reportKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ReportTO read(@NotNull @PathParam("reportKey") Long reportKey);

    /**
     * Returns report execution with matching key.
     *
     * @param executionId report execution id to be selected
     * @return report execution with matching key
     */
    @GET
    @Path("executions/{executionId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ReportExecTO readExecution(@NotNull @PathParam("executionId") Long executionId);

    /**
     * Returns a paged list of all existing reports.
     *
     * @return paged list of all existing reports
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<ReportTO> list();

    /**
     * Returns a paged list of all existing reports.
     *
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of all existing reports
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<ReportTO> list(@QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of all existing reports matching page/size conditions.
     *
     * @param page selected page in relation to size
     * @param size number of entries per page
     * @return paged list of existing reports matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<ReportTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size);

    /**
     * Returns a paged list of all existing reports matching page/size conditions.
     *
     * @param page selected page in relation to size
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of existing reports matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<ReportTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size,
            @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Creates a new report.
     *
     * @param reportTO report to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created report
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE, value = "Featuring <tt>Location</tt> header of created report")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull ReportTO reportTO);

    /**
     * Updates report with matching key.
     *
     * @param reportKey id for report to be updated
     * @param reportTO report to be stored
     */
    @PUT
    @Path("{reportKey}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("reportKey") Long reportKey, ReportTO reportTO);

    /**
     * Deletes report with matching key.
     *
     * @param reportKey Deletes report with matching key
     */
    @DELETE
    @Path("{reportKey}")
    void delete(@NotNull @PathParam("reportKey") Long reportKey);

    /**
     * Deletes report execution with matching key.
     *
     * @param executionId key of execution report to be deleted
     */
    @DELETE
    @Path("executions/{executionId}")
    void deleteExecution(@NotNull @PathParam("executionId") Long executionId);

    /**
     * Executes the report with matching key.
     *
     * @param reportKey key of report to be executed
     * @return report execution result
     */
    @POST
    @Path("{reportKey}/execute")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ReportExecTO execute(@NotNull @PathParam("reportKey") Long reportKey);

    /**
     * Exports the report execution with matching key in the requested format.
     *
     * @param executionId key of execution report to be selected
     * @param fmt file-format selection
     * @return a stream for content download
     */
    @GET
    @Path("executions/{executionId}/stream")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response exportExecutionResult(@NotNull @PathParam("executionId") Long executionId,
            @QueryParam("format") ReportExecExportFormat fmt);
}
