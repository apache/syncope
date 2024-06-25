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
import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;

/**
 * REST operations for attribute schemas.
 */
@Tag(name = "Schemas")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("schemas")
public interface SchemaService extends JAXRSService {

    /**
     * Returns a list of schemas matching the given query.
     *
     * @param <T> actual SchemaTO
     * @param query query conditions
     * @return list of schemas with matching type, for the given anyTypeClass if provided,
     * with keys containing keyword if provided
     */
    @GET
    @Path("{type}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    <T extends SchemaTO> List<T> search(@BeanParam SchemaQuery query);

    /**
     * Returns schema matching the given type and key.
     *
     * @param <T> actual SchemaTO
     * @param type type for schemas to be read
     * @param key name of schema to be read
     * @return schema matching the given type and name
     */
    @GET
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    <T extends SchemaTO> T read(
            @NotNull @PathParam("type") SchemaType type, @NotNull @PathParam("key") String key);

    /**
     * Creates a new schema.
     *
     * @param type type for schema to be created
     * @param schemaTO schema to be created
     * @return Response object featuring Location header of created schema
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "Schema successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "Key value for the entity created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the entity created") }))
    @POST
    @Path("{type}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull @PathParam("type") SchemaType type, @NotNull SchemaTO schemaTO);

    /**
     * Updates the schema matching the given type and key.
     *
     * @param type type for schemas to be updated
     * @param schemaTO updated schema to be stored
     */
    @Parameter(name = "key", description = "Schema's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{type}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull @PathParam("type") SchemaType type, @NotNull SchemaTO schemaTO);

    /**
     * Deletes the schema matching the given type and key.
     *
     * @param type type for schema to be deleted
     * @param key name of schema to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("type") SchemaType type, @NotNull @PathParam("key") String key);

    @POST
    @Path("PLAIN/{key}/dropdownValues")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Attr getDropdownValues(@NotNull @PathParam("key") String key, @NotNull AnyTO anyTO);
}
