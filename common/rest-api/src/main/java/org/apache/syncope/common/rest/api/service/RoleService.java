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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import java.io.InputStream;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for roles.
 */
@Api(tags = "Roles", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })
@Path("roles")
public interface RoleService extends JAXRSService {

    /**
     * Returns a list of all roles.
     *
     * @return list of all roles.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<RoleTO> list();

    /**
     * Returns role with matching key.
     *
     * @param key role key to be read
     * @return role with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    RoleTO read(@NotNull @PathParam("key") String key);

    /**
     * Creates a new role.
     *
     * @param roleTO role to be created
     * @return Response object featuring Location header of created role
     */
    @ApiResponses(
            @ApiResponse(code = 201,
                    message = "Role successfully created", responseHeaders = {
                @ResponseHeader(name = RESTHeaders.RESOURCE_KEY, response = String.class,
                        description = "Key value for the entity created")
                , @ResponseHeader(name = HttpHeaders.LOCATION, response = String.class,
                        description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull RoleTO roleTO);

    /**
     * Updates the role matching the provided key.
     *
     * @param roleTO role to be stored
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull RoleTO roleTO);

    /**
     * Deletes the role matching the provided key.
     *
     * @param key role key to be deleted
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Gets the console layout information as JSON string for the role with the given key, if available.
     *
     * @param key role key
     * @return console layout information as JSON string for the role with the given key, if available
     */
    @GET
    @Path("{key}/consoleLayout")
    @Produces({ MediaType.APPLICATION_JSON })
    Response getConsoleLayoutInfo(@NotNull @PathParam("key") String key);

    /**
     * Sets the console layout information as JSON string for the role with the given key, if available.
     *
     * @param key role key
     * @param consoleLayoutInfoIn console layout information to be set
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @PUT
    @Path("{key}/consoleLayout")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void setConsoleLayoutInfo(@NotNull @PathParam("key") String key, InputStream consoleLayoutInfoIn);

    /**
     * Removes the console layout information for the role with the given key, if available.
     *
     * @param key role key
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @DELETE
    @Path("{key}/consoleLayout")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void removeConsoleLayoutInfo(@NotNull @PathParam("key") String key);
}
