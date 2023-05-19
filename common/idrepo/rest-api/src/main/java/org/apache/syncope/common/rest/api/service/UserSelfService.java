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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ComplianceQuery;

/**
 * REST operations for user self-management.
 */
@Tag(name = "UserSelf")
@Path("users/self")
public interface UserSelfService extends JAXRSService {

    /**
     * Returns the user making the service call.
     *
     * @return calling user data, including own UUID, entitlements and delegations
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "200",
                    description = "Calling user data, including own UUID, entitlements and delegations", content =
                    @Content(schema =
                            @Schema(implementation = UserTO.class)), headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID of the calling user"),
                @Header(name = RESTHeaders.OWNED_ENTITLEMENTS, schema =
                        @Schema(type = "string"),
                        description = "List of entitlements owned by the calling user")
            }))
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response read();

    /**
     * Self-registration for new user.
     *
     * @param createReq user to be created
     * @return Response object featuring Location header of self-registered user as well as the user itself
     * enriched with propagation status information
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "User successfully created enriched with propagation status information, as Entity,"
                    + " or empty if 'Prefer: return-no-content' was specified",
                    content =
                    @Content(schema =
                            @Schema(implementation = ProvisioningResult.class)), headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the user created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the user created"),
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied") }))
    @POST
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull UserCR createReq);

    /**
     * Self-updates user.
     *
     * @param updateReq modification to be applied to self
     * @return Response object featuring the updated user
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = "key", description = "User's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "User successfully updated enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")) })
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(@NotNull UserUR updateReq);

    /**
     * Self-updates user.
     *
     * @param user complete update
     * @return Response object featuring the updated user
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = "key", description = "User's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "User successfully updated enriched with propagation status information, as Entity",
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
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(@NotNull UserTO user);

    /**
     * Self-perform a status update.
     *
     * @param statusR status update details
     * @return Response object featuring the updated user enriched with propagation status information
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = "key", description = "User's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "User successfully updated enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")) })
    @POST
    @Path("{key}/status")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response status(@NotNull StatusR statusR);

    /**
     * Self-deletes user.
     *
     * @return Response object featuring the deleted user
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response delete();

    /**
     * Changes own password when change was forced by an administrator.
     *
     * @param password the password value to update
     *
     * @return Response object featuring the updated user
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @POST
    @Path("mustChangePassword")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response mustChangePassword(@NotNull PasswordPatch password);

    /**
     * Checks compliance of the given username and / or password with applicable policies.
     *
     * @param query compliance query
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @POST
    @Path("compliance")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void compliance(@NotNull ComplianceQuery query);

    /**
     * Provides answer for the security question configured for user matching the given username, if any.
     * If provided answer matches the one stored for that user, a password reset token is internally generated,
     * otherwise an error is returned.
     *
     * @param username username for which the security answer is provided
     * @param securityAnswer actual answer text
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("requestPasswordReset")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void requestPasswordReset(@NotNull @QueryParam("username") String username, String securityAnswer);

    /**
     * Reset the password value for the user matching the provided token, if available and still valid.
     * If the token actually matches one of users, and if it is still valid at the time of submission, the matching
     * user's password value is set as provided. The new password value will need anyway to comply with all relevant
     * password policies.
     *
     * @param token password reset token
     * @param password new password to be set
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("confirmPasswordReset")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void confirmPasswordReset(@NotNull @QueryParam("token") String token, String password);
}
