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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;

/**
 * REST operations for groups.
 */
@Tag(name = "Groups")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("groups")
public interface GroupService extends AnyService<GroupTO> {

    @ApiResponses(
            @ApiResponse(responseCode = "200", description =
                    "Group matching the provided key; if value looks like a UUID then it is interpreted as key,"
                    + " otherwise as a name.", headers =
                    @Header(name = HttpHeaders.ETAG, schema =
                            @Schema(type = "string"),
                            description = "Opaque identifier for the latest modification made to the entity returned"
                            + " by this endpoint")))
    @Override
    GroupTO read(String key);

    @Override
    PagedResult<GroupTO> search(AnyQuery anyQuery);

    /**
     * Creates a new group.
     *
     * @param createReq group create request
     * @return Response object featuring Location header of created group as well as the any
     * object itself enriched with propagation status information
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "Group successfully created enriched with propagation status information, as Entity,"
                    + " or empty if 'Prefer: return-no-content' was specified",
                    content =
                    @Content(schema =
                            @Schema(implementation = ProvisioningResult.class)), headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the group created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the group created"),
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied") }))
    @POST
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull GroupCR createReq);

    /**
     * Updates group matching the provided key.
     *
     * @param updateReq modification to be applied to group matching the provided key
     * @return Response object featuring the updated group enriched with propagation status information
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = HttpHeaders.IF_MATCH, in = ParameterIn.HEADER,
            description = "When the provided ETag value does not match the latest modification date of the entity, "
            + "an error is reported and the requested operation is not performed.",
            allowEmptyValue = true, schema =
            @Schema(type = "string"))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @Parameter(name = "key", description = "Group's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Group successfully updated enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")),
        @ApiResponse(responseCode = "412",
                description = "The ETag value provided via the 'If-Match' header does not match the latest modification"
                + " date of the entity") })
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(@NotNull GroupUR updateReq);

    /**
     * This method allows a user to read his own groups.
     *
     * @return own groups
     */
    @GET
    @Path("own")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<GroupTO> own();

    /**
     * (De)provision all members of the given group from / onto all the resources associated to it.
     *
     * @param key group key
     * @param action action type to perform on all group members
     * @return execution report for the task generated on purpose
     */
    @POST
    @Path("{key}/members/{action}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ExecTO provisionMembers(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("action") ProvisionAction action);
}
