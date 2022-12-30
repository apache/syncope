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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;

/**
 * REST operations for reconciliation.
 */
@Tag(name = "Reconciliation")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("reconciliation")
public interface ReconciliationService extends JAXRSService {

    /**
     * Gets compared status between attributes in Syncope and on the given External Resource.
     *
     * @param query query conditions
     * @return reconciliation status
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ReconStatus status(@BeanParam ReconQuery query);

    /**
     * Pushes the matching user, group, any object or linked account in Syncope onto the External Resource.
     *
     * @param query query conditions
     * @param pushTask push specification
     * @return push report
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("push")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ProvisioningReport> push(@BeanParam ReconQuery query, @NotNull PushTaskTO pushTask);

    /**
     * Pulls the matching user, group, any object or linked account from the External Resource into Syncope.
     *
     * @param query query conditions
     * @param pullTask pull specification
     * @return pull report
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("pull")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ProvisioningReport> pull(@BeanParam ReconQuery query, @NotNull PullTaskTO pullTask);

    /**
     * Export a list of any objects matching the given query as CSV according to the provided specification.
     *
     * @param anyQuery query conditions
     * @param spec CSV push specification
     * @return CSV content matching the provided specification
     */
    @GET
    @Path("csv/push")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ RESTHeaders.TEXT_CSV })
    Response push(@BeanParam AnyQuery anyQuery, @BeanParam CSVPushSpec spec);

    /**
     * Pulls the CSV input into Syncope according to the provided specification.
     *
     * @param spec CSV pull specification
     * @param csv CSV input
     * @return pull report
     */
    @POST
    @Path("csv/pull")
    @Consumes({ RESTHeaders.TEXT_CSV })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ProvisioningReport> pull(@BeanParam CSVPullSpec spec, InputStream csv);
}
