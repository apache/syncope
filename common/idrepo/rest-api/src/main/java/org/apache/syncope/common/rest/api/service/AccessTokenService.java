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
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;

/**
 * REST operations for access tokens.
 */
@Tag(name = "AccessTokens")
@Path("accessTokens")
public interface AccessTokenService extends JAXRSService {

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, in case of successful authentication.
     * The provided value is a signed JSON Web Token.
     *
     * @return empty response bearing the X-Syncope-Token header value, in case of successful authentication
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication") })
    @ApiResponses({
        @ApiResponse(responseCode = "204",
                description = "JWT successfully generated", headers = {
                    @Header(name = RESTHeaders.TOKEN, schema =
                            @Schema(type = "string"), description = "Generated JWT"),
                    @Header(name = RESTHeaders.TOKEN_EXPIRE, schema =
                            @Schema(type = "string"), description = "Expiration of the generated JWT") }),
        @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @POST
    @Path("login")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response login();

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, with extended lifetime.
     * The provided value is a signed JSON Web Token.
     *
     * @return an empty response bearing the X-Syncope-Token header value, with extended lifetime
     */
    @Operation(security = {
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "204",
                    description = "JWT successfully refreshed", headers = {
                @Header(name = RESTHeaders.TOKEN, schema =
                        @Schema(type = "string"),
                        description = "Generated JWT"),
                @Header(name = RESTHeaders.TOKEN_EXPIRE, schema =
                        @Schema(type = "string"),
                        description = "Expiration of the refreshed JWT") }))
    @POST
    @Path("refresh")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response refresh();

    /**
     * Invalidates the access token of the requesting user.
     */
    @Operation(security = {
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("logout")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void logout();

    /**
     * Returns a paged list of existing access tokens matching the given query.
     *
     * @param query query conditions
     * @return paged list of existing access tokens matching the given query
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @GET
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<AccessTokenTO> list(@BeanParam AccessTokenQuery query);

    /**
     * Invalidates the access token matching the provided key.
     *
     * @param key access token key
     */
    @Operation(security = {
        @SecurityRequirement(name = "BasicAuthentication"),
        @SecurityRequirement(name = "Bearer") })
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@PathParam("key") String key);
}
