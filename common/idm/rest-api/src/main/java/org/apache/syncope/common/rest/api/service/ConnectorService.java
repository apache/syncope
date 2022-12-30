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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnIdObjectClass;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for connector bundles and instances.
 */
@Tag(name = "Connectors")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("connectors")
public interface ConnectorService extends JAXRSService {

    /**
     * Returns available connector bundles with property keys in selected language.
     *
     * @param lang language to select property keys; default language is English
     * @return available connector bundles with property keys in selected language
     */
    @GET
    @Path("bundles")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ConnIdBundle> getBundles(@QueryParam("lang") String lang);

    /**
     * Builds the list of ConnId object classes information for the connector bundle matching the given connector
     * instance key, with the provided configuration.
     *
     * @param connInstanceTO connector instance object providing configuration properties
     * @param includeSpecial if set to true, special schema names (like '__PASSWORD__') will be included;
     * default is false
     * @return supported object classes info for the connector bundle matching the given connector instance key, with
     * the provided configuration
     */
    @Parameter(name = "key", description = "Connector instance's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @POST
    @Path("{key}/supportedObjectClasses")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ConnIdObjectClass> buildObjectClassInfo(
            @NotNull ConnInstanceTO connInstanceTO,
            @QueryParam("includeSpecial") @DefaultValue("false") boolean includeSpecial);

    /**
     * Returns connector instance with matching key.
     *
     * @param key connector instance key to be read
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return connector instance with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ConnInstanceTO read(@NotNull @PathParam("key") String key, @QueryParam("lang") String lang);

    /**
     * Returns connector instance for matching resource.
     *
     * @param resourceName resource name to be used for connector lookup
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return connector instance for matching resource
     */
    @GET
    @Path("byResource/{resourceName}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ConnInstanceTO readByResource(
            @NotNull @PathParam("resourceName") String resourceName, @QueryParam("lang") String lang);

    /**
     * Returns a list of all connector instances with property keys in the matching language.
     *
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return list of all connector instances with property keys in the matching language
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    /**
     * Creates a new connector instance.
     *
     * @param connInstanceTO connector instance to be created
     * @return Response object featuring Location header of created connector instance
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "ConnInstance successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the entity created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Updates the connector instance matching the provided key.
     *
     * @param connInstanceTO connector instance to be stored
     */
    @Parameter(name = "key", description = "Connector instance's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Deletes the connector instance matching the provided key.
     *
     * @param key connector instance key to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Checks whether the connection to resource could be established.
     *
     * @param connInstanceTO connector instance to be used for connection check
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("check")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void check(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Reload all connector bundles and instances.
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("reload")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void reload();
}
