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
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.PagedConnObjectResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;

/**
 * REST operations for external resources.
 */
@Tag(name = "Resources")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("resources")
public interface ResourceService extends JAXRSService {

    /**
     * Returns the calculated connObjectKey value for the given type and key.
     *
     * @param key name of resource to read connector object from
     * @param anyTypeKey any object type
     * @param anyKey user, group or any object key
     * @return connObjectKey value for the external resource, for the given type and key
     */
    @ApiResponses({
        @ApiResponse(responseCode = "201",
                description = "connObjectKey value for the external resource, for the given type and key", headers = {
                    @Header(name = RESTHeaders.CONNOBJECT_KEY, schema =
                            @Schema(type = "string"),
                            description = "connObjectKey value for the external resource, for the given type and key")
                }),
        @ApiResponse(responseCode = "404",
                description = "user, group or any object not found, or connObjectKey cannot be calculated") })
    @OPTIONS
    @Path("{key}/{anyTypeKey}/{anyKey}")
    Response getConnObjectKeyValue(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey,
            @NotNull @PathParam("anyKey") String anyKey);

    /**
     * Returns connector object from the external resource, for the given type and key.
     *
     * @param key name of resource to read connector object from
     * @param anyTypeKey any object type
     * @param value if value looks like a UUID then it is interpreted as user, group or any object key, otherwise
     * as key value on the resource
     * @return connector object from the external resource, for the given type and key
     */
    @GET
    @Path("{key}/{anyTypeKey}/{value}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ConnObject readConnObject(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey,
            @NotNull @PathParam("value") String value);

    /**
     * Returns a paged list of connector objects from external resource, for the given type, matching
     * page/size conditions.
     *
     * @param key name of resource to read connector object from
     * @param anyTypeKey any object type
     * @param connObjectTOQuery query conditions
     * @return connector objects from the external resource, for the given type
     */
    @GET
    @Path("{key}/{anyTypeKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedConnObjectResult searchConnObjects(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey,
            @BeanParam ConnObjectTOQuery connObjectTOQuery);

    /**
     * Returns the resource with matching name.
     *
     * @param key Name of resource to be read
     * @return resource with matching name
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ResourceTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of all resources.
     *
     * @return list of all resources
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ResourceTO> list();

    /**
     * Creates a new resource.
     *
     * @param resourceTO Resource to be created
     * @return Response object featuring Location header of created resource
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "Resource successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "Key value for the entity created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull ResourceTO resourceTO);

    /**
     * Updates the resource matching the given name.
     *
     * @param resourceTO resource to be stored
     */
    @Parameter(name = "key", description = "Resource's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull ResourceTO resourceTO);

    /**
     * Queries the connector underlying the given resource for the latest sync token value associated to the given any
     * type and stores the value internally, for later usage.
     *
     * @param key resource
     * @param anyTypeKey any type
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("{key}/{anyTypeKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void setLatestSyncToken(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey);

    /**
     * Removes the sync token value associated to the given any type from the given resource.
     *
     * @param key resource
     * @param anyTypeKey any type
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}/{anyTypeKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void removeSyncToken(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey);

    /**
     * Deletes the resource matching the given name.
     *
     * @param key name of resource to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Checks whether the connection to resource could be established.
     *
     * @param resourceTO resource to be checked
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("check")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void check(@NotNull ResourceTO resourceTO);
}
