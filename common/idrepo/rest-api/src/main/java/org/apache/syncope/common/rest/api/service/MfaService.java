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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for MFA management.
 */
@Tag(name = "MFA")
@Path("mfa")
public interface MfaService extends JAXRSService {

    /**
     * Generate MFA information for the given user.
     *
     * @param username username
     * @return MFA information for the given user.
     */
    @POST
    @Path("{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    Mfa generate(@NotNull @PathParam("username") String username);

    /**
     * Store the provided MFA information for the calling user.
     *
     * @param mfa MFA information
     */
    @Operation(security =
            @SecurityRequirement(name = "BasicAuthentication"))
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    void enroll(Mfa mfa);

    /**
     * Dismiss MFA information for the calling user.
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "MFA dismissed"))
    @DELETE
    void dismiss();

    /**
     * Dismiss MFA information for the given user.
     *
     * @param username username
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "MFA dismissed"))
    @DELETE
    @Path("{username}")
    void dismiss(@NotNull @PathParam("username") String username);

    /**
     * Checks if MFA information was enrolled for the given user.
     *
     * @param username username
     * @return Response object featuring the boolean result in the 'X-Syncope-Verfied' header
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful", headers =
                    @Header(name = RESTHeaders.VERIFIED, schema =
                            @Schema(allowableValues = { "true", "false" }, type = "boolean"))))
    @HEAD
    @Path("{username}")
    Response enrolled(@NotNull @PathParam("username") String username);

    /**
     * Check the provided OTP against the provided MFA secret.
     *
     * @param mfaCheck MFA secret and OTP
     * @return Response object featuring the boolean result in the 'X-Syncope-Verfied' header
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful", headers =
                    @Header(name = RESTHeaders.VERIFIED, schema =
                            @Schema(allowableValues = { "true", "false" }, type = "boolean"))))
    @POST
    @Path("check")
    @Consumes({ MediaType.APPLICATION_JSON })
    Response check(MfaCheck mfaCheck);
}
