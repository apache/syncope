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

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.syncope.common.rest.api.RESTHeaders;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;

/**
 * REST operations for resgistered client applications.
 */
@Tag(name = "RegisteredClientApps")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("registeredClientApps")
public interface RegisteredClientAppService extends JAXRSService {

    /**
     * Returns a list of all client applications to be registered.
     *
     * @return list of all client applications.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<RegisteredClientAppTO> list();

    /**
     * Returns a client application with matching key.
     *
     * @param clientAppId registered client application ID to be read
     * @return registered client application with matching id
     */
    @GET
    @Path("{clientAppId}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    RegisteredClientAppTO read(@NotNull @PathParam("clientAppId") Long clientAppId);

    @GET
    @Path("{clientAppId}/{type}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    RegisteredClientAppTO read(
            @NotNull @PathParam("clientAppId") Long clientAppId,
            @NotNull @PathParam("type") ClientAppType type);

    /**
     * Returns a client application with matching key.
     *
     * @param name registered client application name to be read
     * @return registered client application with matching name
     */
    @GET
    @Path("/name/{name}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    RegisteredClientAppTO read(@NotNull @PathParam("name") String name);

    @GET
    @Path("/name/{name}/{type}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    RegisteredClientAppTO read(
            @NotNull @PathParam("name") String name,
            @NotNull @PathParam("type") ClientAppType type);

    /**
     * Create a new client app.
     *
     * @param registeredClientAppTO
     * @return Response object featuring Location header of created registered client app
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "ClientApp successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the entity created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull RegisteredClientAppTO registeredClientAppTO);

    /**
     * Delete client app matching the given key.
     *
     * @param name name of registered client application to be deleted
     * @return
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{name}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    boolean delete(@NotNull @PathParam("name") String name);

}
