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
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.RealmQuery;

/**
 * REST operations for realms.
 */
@Tag(name = "Realms")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("realms")
public interface RealmService extends JAXRSService {

    /**
     * Returns a paged list of existing realms matching the given query.
     *
     * @param query query conditions
     * @return paged list of existing realms matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<RealmTO> search(@BeanParam RealmQuery query);

    /**
     * Creates a new realm under the given path.
     *
     * @param parentPath full path of the parent realm
     * @param realmTO new realm
     * @return Response object featuring Location header of created realm as well as the realm itself
     * enriched with propagation status information
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
                    description = "Realm successfully created enriched with propagation status information, as Entity,"
                    + " or empty if 'Prefer: return-no-content' was specified",
                    content =
                    @Content(schema =
                            @Schema(implementation = ProvisioningResult.class)), headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the realm created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the realm created"),
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied") }))
    @POST
    @Path("{parentPath:.*}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull @PathParam("parentPath") String parentPath, @NotNull RealmTO realmTO);

    /**
     * Updates the realm under the given path.
     *
     * @param realmTO realm to be stored
     * @return Response object featuring the updated realm enriched with propagation status information
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
    @Parameter(name = "fullPath", description = "Realm's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Realm successfully updated enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")) })
    @PUT
    @Path("{fullPath:.*}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(@NotNull RealmTO realmTO);

    /**
     * Deletes the realm under the given path.
     *
     * @param fullPath realm path
     * @return Response object featuring the deleted realm enriched with propagation status information
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
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Realm successfully deleted enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")) })
    @DELETE
    @Path("{fullPath:.*}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("fullPath") String fullPath);
}
