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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * General info about this Apache Syncope deployment.
 */
@Tag(name = "Syncope")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("")
public interface SyncopeService extends JAXRSService {

    /**
     * Requests for batch execution.
     *
     * @param input batch request
     * @return batch results returned as Response entity, in case no 'Prefer: respond-async' was specified
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference to process the batch request asynchronously",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "", allowableValues = { "respond-async" }))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Batch request processed, results returned as Response entity, "
                + "in case no 'Prefer: respond-async' was specified"),
        @ApiResponse(responseCode = "202",
                description = "Batch accepted for asynchronous processing, "
                + "in case 'Prefer: respond-async' was specified", headers = {
                    @Header(name = HttpHeaders.LOCATION, schema =
                            @Schema(type = "string"),
                            description = "URL to poll in order to get the results of the requested batch processing"),
                    @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                            @Schema(type = "string"),
                            description = "Allows the server to inform the "
                            + "client about the fact that a specified preference was applied") }) })
    @POST
    @Path("batch")
    @Consumes(RESTHeaders.MULTIPART_MIXED)
    @Produces(RESTHeaders.MULTIPART_MIXED)
    Response batch(InputStream input);

    /**
     * Gets batch results, in case asynchronous was requested.
     *
     * @return batch results as Response entity
     */
    @GET
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Batch results available, returned as Response entity"),
        @ApiResponse(responseCode = "202",
                description = "Batch results not yet available, retry later", headers = {
                    @Header(name = HttpHeaders.LOCATION, schema =
                            @Schema(type = "string"),
                            description = "URL to poll in order to get the results of the requested batch processing"),
                    @Header(name = HttpHeaders.RETRY_AFTER, schema =
                            @Schema(type = "integer"),
                            description = "seconds after which attempt again to get batch results") }),
        @ApiResponse(responseCode = "404", description = "No batch process was found for the provided boundary") })
    @Path("batch")
    @Produces(RESTHeaders.MULTIPART_MIXED)
    Response batch();

    /**
     * Returns the list of Groups, according to provided paging instructions, assignable to Users and Any Objects of
     * the provided Realm.
     *
     * @param term groups search term
     * @param realm of the User and Any Objects assignable to the returned Groups
     * @param page search page
     * @param size search page size
     * @return list of Groups, according to provided paging instructions, assignable to Users and Any Objects of
     * the provided Realm
     */
    @POST
    @Path("assignableGroups/{realm:.*}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<GroupTO> searchAssignableGroups(
            @NotNull @PathParam("realm") String realm,
            @QueryParam("term") String term,
            @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue("1") int page,
            @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue("25") int size);

    /**
     * Extracts User type extension information, for the provided group.
     *
     * @param groupName group name
     * @return User type extension information, for the provided group
     */
    @GET
    @Path("userTypeExtension/{groupName}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    TypeExtensionTO readUserTypeExtension(@NotNull @PathParam("groupName") String groupName);

    /**
     * Exports the internal storage content as downloadable XML file.
     *
     * @param threshold the maximum number of rows to take for each element of internal storage
     * @param elements if provided, the list of elements to export; otherwise all elements will be included
     * @return internal storage content as downloadable XML file
     */
    @GET
    @Path("internalStorage/stream")
    Response exportInternalStorageContent(
            @QueryParam("threshold") @DefaultValue("100") int threshold,
            @QueryParam("elements") List<String> elements);
}
