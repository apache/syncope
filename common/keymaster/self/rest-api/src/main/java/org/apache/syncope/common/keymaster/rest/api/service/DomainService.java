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
package org.apache.syncope.common.keymaster.rest.api.service;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for Self Keymaster's domains.
 */
@Tag(name = "Domains")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication") })
@Path("domains")
public interface DomainService extends Serializable {

    /**
     * Returns the list of defined domains.
     *
     * @return list of defined domains
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    List<Domain> list();

    /**
     * Returns the domain matching the given key.
     *
     * @param key key of the domain to be read
     * @return domain matching the given key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    Domain read(@NotNull @PathParam("key") String key);

    /**
     * Creates a new domain.
     *
     * @param domain domain to be created
     * @return Response object featuring Location header of created domain
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "Domain successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "Key of the domain created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the domain created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    Response create(Domain domain);

    /**
     * Notify that the given domain is deployed.
     *
     * @param key key of domain to be updated
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("{key}/deployed")
    @Produces({ MediaType.APPLICATION_JSON })
    void deployed(@NotNull @PathParam("key") String key);

    /**
     * Change admin's password for the given domain.
     *
     * @param key key of domain to be updated
     * @param password encoded password value
     * @param cipherAlgorithm password cipher algorithm
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("{key}/changeAdminPassword")
    @Produces({ MediaType.APPLICATION_JSON })
    void changeAdminPassword(
            @NotNull @PathParam("key") String key,
            @QueryParam("password") String password,
            @QueryParam("cipherAlgorithm") CipherAlgorithm cipherAlgorithm);

    /**
     * Adjusts the connection pool to the domain database.
     *
     * @param key key of domain to be updated
     * @param poolMaxActive database pool max size
     * @param poolMinIdle database pool max size
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("{key}/adjustPoolSize")
    @Produces({ MediaType.APPLICATION_JSON })
    void adjustPoolSize(
            @NotNull @PathParam("key") String key,
            @QueryParam("poolMaxActive") int poolMaxActive,
            @QueryParam("poolMinIdle") int poolMinIdle);

    /**
     * Deletes the domain matching the provided key.
     *
     * @param key key of domain to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    void delete(@NotNull @PathParam("key") String key);
}
