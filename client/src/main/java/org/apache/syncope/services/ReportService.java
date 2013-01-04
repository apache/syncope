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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.syncope.client.to.ReportExecTO;
import org.apache.syncope.client.to.ReportTO;
import org.apache.syncope.types.ReportExecExportFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Path("reports")
public interface ReportService {

    @POST
    ReportTO create(final ReportTO reportTO);

    @PUT
    @Path("{reportId}")
    ReportTO update(@PathParam("reportId") final Long reportId, final ReportTO reportTO);

    @GET
    @Path("count")
    int count();

    @GET
    List<ReportTO> list();

    @GET
    List<ReportTO> list(@QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size);

    @GET
    @Path("executions")
    List<ReportExecTO> listExecutions();

    @GET
    @Path("reportletConfClasses")
    List<String> getReportletConfClasses();

    @GET
    @Path("{reportId}")
    ReportTO read(@PathParam("reportId") final Long reportId);

    @GET
    @Path("executions/{executionId}")
    ReportExecTO readExecution(@PathParam("executionId") final Long executionId);

    @GET
    @Path("executions/{executionId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    void exportExecutionResult(@PathParam("executionId") final Long executionId,
            @QueryParam("format") final ReportExecExportFormat fmt);

    @POST
    @Path("{reportId}/execute")
    ReportExecTO execute(@PathParam("reportId") final Long reportId);

    @DELETE
    @Path("{reportId}")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{reportId}")
    ReportTO delete(@PathParam("reportId") final Long reportId);

    @DELETE
    @Path("executions/{executionId}")
    @RequestMapping(method = RequestMethod.GET, value = "/execution/delete/{executionId}")
    ReportExecTO deleteExecution(@PathParam("executionId") final Long executionId);

}