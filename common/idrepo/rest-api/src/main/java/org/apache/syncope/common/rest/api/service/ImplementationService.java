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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for implementations.
 */
@Tag(name = "Implementations")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("implementations")
public interface ImplementationService extends JAXRSService {

    /**
     * Returns a list of all implementations of the given type.
     *
     * @param type implementation type
     * @return list of all implementations.
     */
    @GET
    @Path("{type}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ImplementationTO> list(@NotNull @PathParam("type") String type);

    /**
     * Returns implementation with matching type and key.
     *
     * @param type implementation type
     * @param key key of implementation to be read
     * @return implementation with matching key
     */
    @GET
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ImplementationTO read(@NotNull @PathParam("type") String type, @NotNull @PathParam("key") String key);

    /**
     * Creates a new implementation.
     *
     * @param implementationTO implementation.
     * @return Response object featuring Location header of created implementation
     */
    @Parameter(name = "type", description = "Implementation's type", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @Parameter(name = "key", description = "Implementation's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @POST
    @Path("{type}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull ImplementationTO implementationTO);

    /**
     * Updates an existing implementation.
     *
     * @param implementationTO implementation.
     * @return an empty response if operation was successful
     */
    @Parameter(name = "type", description = "Implementation's type", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @Parameter(name = "key", description = "Implementation's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @PUT
    @Path("{type}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(@NotNull ImplementationTO implementationTO);

    /**
     * Deletes the implementation matching the given key and type.
     *
     * @param type implementation type
     * @param key key for implementation to be deleted
     * @return an empty response if operation was successful
     */
    @DELETE
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("type") String type, @NotNull @PathParam("key") String key);
}
